package com.fadhil.financereceipt.receipt;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rule-Based NER sesuai proposal. Murni JVM, tanpa model terlatih atau jaringan. */
public final class ReceiptParser {
    public static final String VERSION = "rules-1.0";
    private static final Pattern NUMERIC_DATE = Pattern.compile(
            "(?<!\\d)(\\d{1,4})([/.-])(\\d{1,2})\\2(\\d{2,4})(?!\\d)");
    private static final Pattern WORD_DATE = Pattern.compile(
            "(?i)(?<!\\d)(\\d{1,2})[\\s-]+([a-z]+)[\\s,-]+(\\d{4}|\\d{2})(?!\\d)");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?<![\\p{L}\\d/:.,-])(?:Rp\\.?\\s*)?(\\d+(?:[.,]\\d+)*)(?![\\p{L}\\d/:.,%])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLUDED_TOTAL = Pattern.compile(
            "(?i)\\b(sub\\s*total|kembali(?:an)?|change|tunai|cash|debit|kredit|credit|" +
            "diskon|discount|ppn|pajak|tax|hemat|saving|total\\s+(?:item|qty|barang|diskon))\\b");
    private static final Pattern TOTAL = Pattern.compile(
            "(?i)\\b(grand\\s*total|total\\s*(?:bayar|belanja|pembayaran)|" +
            "jumlah\\s*(?:bayar|pembayaran)|total|jumlah|bayar|amount\\s*due)\\b");

    public static final class Result {
        public final String storeName;
        public final Long dateMillis;
        public final Long totalAmount;
        public final String normalizedText;
        public final List<String> warnings;
        public Result(String store, Long date, Long total, String normalized, List<String> warnings) {
            this.storeName = store;
            this.dateMillis = date;
            this.totalAmount = total;
            this.normalizedText = normalized;
            this.warnings = Collections.unmodifiableList(warnings);
        }
    }

    public Result parse(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .replace('\u00a0', ' ').split("\\r?\\n")) {
            String cleaned = line.replaceAll("[\\p{Cntrl}&&[^\\t]]", "")
                    .replaceAll("[\\t ]+", " ").trim();
            if (!cleaned.isEmpty()) lines.add(cleaned);
        }
        StringBuilder joined = new StringBuilder();
        for (String line : lines) { if (joined.length() > 0) joined.append('\n'); joined.append(line); }
        String normalized = joined.toString();
        List<String> warnings = new ArrayList<>();
        Long date = null;
        for (String line : lines) {
            // Hindari tanggal kedaluwarsa/promosi yang bukan tanggal transaksi.
            if (line.toLowerCase(Locale.ROOT).matches(".*\\b(exp|expired|berlaku|kadaluarsa|kedaluwarsa)\\b.*")) continue;
            Long candidate = parseDate(line);
            if (candidate != null) {
                if (date == null) date = candidate;
                else if (!date.equals(candidate) && !warnings.contains("Terdapat beberapa tanggal. Periksa tanggal transaksi."))
                    warnings.add("Terdapat beberapa tanggal. Periksa tanggal transaksi.");
            }
        }

        String store = null;
        for (int i = 0; i < Math.min(lines.size(), 8); i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.length() < 3 || line.length() > 120 ||
                    !Pattern.compile("[\\p{L}]{3}").matcher(line).find()) continue;
            if (NUMERIC_DATE.matcher(line).find() || WORD_DATE.matcher(line).find()) continue;
            if (lower.matches(".*\\b(jl|jln|jalan|telp|telepon|phone|npwp|kasir|cashier|" +
                    "tanggal|waktu|invoice|receipt|struk|nota|no|nomor|www|http|" +
                    "pelanggan|member|selamat|terima|subtotal|total|tunai|kembali)\\b.*")) continue;
            int digits = 0;
            for (int c = 0; c < line.length(); c++) if (Character.isDigit(line.charAt(c))) digits++;
            if (digits > line.length() / 3) continue;
            store = line;
            break;
        }

        Long total = null;
        int bestPriority = -1;
        List<Long> competingTotals = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (EXCLUDED_TOTAL.matcher(line).find()) continue;
            Matcher keyword = TOTAL.matcher(line);
            if (!keyword.find()) continue;
            String key = keyword.group().toLowerCase(Locale.ROOT);
            int priority = key.contains("grand") || key.contains("due") ||
                    key.contains("bayar") && !key.equals("bayar") ? 3 : key.equals("bayar") ? 1 : 2;
            String tail = line.substring(keyword.end()).trim();
            Long amount = lastAmount(tail);
            // Nilai pada baris berikutnya hanya diterima bila baris itu nominal saja.
            if (amount == null && tail.matches("[:=Rp.\\s]*") && i + 1 < lines.size()) {
                String next = lines.get(i + 1).trim();
                if (next.matches("(?i)(?:Rp\\.?\\s*)?\\d[\\d.,]*[\\s,-]*")) amount = lastAmount(next);
            }
            if (amount == null || amount <= 0) continue;
            if (priority > bestPriority) {
                total = amount;
                bestPriority = priority;
                competingTotals.clear();
                competingTotals.add(amount);
            } else if (priority == bestPriority) {
                total = amount; // Posisi paling akhir pada tingkat prioritas yang sama.
                competingTotals.add(amount);
            }
        }
        if (new java.util.HashSet<>(competingTotals).size() > 1)
            warnings.add("Terdapat beberapa kandidat total. Periksa nominal pembayaran.");
        if (bestPriority == 1) warnings.add("Nominal diambil dari label Bayar. Pastikan bukan uang yang diserahkan.");
        if (store == null) warnings.add("Nama toko belum ditemukan. Isi secara manual.");
        if (date == null) warnings.add("Tanggal belum ditemukan atau tidak valid. Pilih tanggal transaksi.");
        if (total == null) warnings.add("Total belum ditemukan atau formatnya meragukan. Isi nominal secara manual.");
        return new Result(store, date, total, normalized, warnings);
    }

    private Long lastAmount(String text) {
        text = text.replaceFirst("^\\s*[:=]\\s*", "");
        text = text.replaceAll("(?<=\\d)[.,]-(?=\\s|$)", "");
        Matcher m = AMOUNT.matcher(text);
        Long result = null;
        while (m.find()) {
            // Persen, tanggal, dan waktu tidak boleh menjadi nominal.
            if (m.end() < text.length() && text.substring(m.end()).matches("^\\s*[%:/].*")) continue;
            Long parsed = parseRupiah(m.group(1));
            if (parsed != null) result = parsed;
        }
        return result;
    }

    /** Rupiah utuh; pecahan nonnol ditolak, tidak dibulatkan diam-diam. */
    public Long parseRupiah(String input) {
        if (input == null) return null;
        String value = input.trim().replaceFirst("(?i)^Rp\\.?\\s*", "").trim()
                .replaceFirst("[.,]-$", "");
        String decimal;
        if (value.matches("\\d+")) decimal = value;
        else if (value.matches("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?"))
            decimal = value.replace(".", "").replace(',', '.');
        else if (value.matches("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?"))
            decimal = value.replace(",", "");
        else if (value.matches("\\d+[.,]\\d{1,2}")) decimal = value.replace(',', '.');
        else return null;
        try {
            long amount = new BigDecimal(decimal).longValueExact();
            return amount > 0 ? amount : null;
        } catch (ArithmeticException | NumberFormatException ignored) { return null; }
    }

    private Long parseDate(String text) {
        Matcher numeric = NUMERIC_DATE.matcher(text);
        while (numeric.find()) {
            boolean iso = numeric.group(1).length() == 4;
            int year = Integer.parseInt(numeric.group(iso ? 1 : 4));
            if (year < 100) year += 2000;
            Long date = validDate(year, Integer.parseInt(numeric.group(3)),
                    Integer.parseInt(numeric.group(iso ? 4 : 1)));
            if (date != null) return date;
        }
        Matcher word = WORD_DATE.matcher(text);
        while (word.find()) {
            String name = word.group(2).toLowerCase(Locale.ROOT);
            String[] months = {"jan|januari|january", "feb|februari|february", "mar|maret|march",
                    "apr|april", "mei|may", "jun|juni|june", "jul|juli|july", "agu|agt|agustus|aug|august",
                    "sep|sept|september", "okt|oktober|oct|october", "nov|november", "des|desember|dec|december"};
            for (int i = 0; i < months.length; i++) if (name.matches(months[i])) {
                int year = Integer.parseInt(word.group(3));
                if (year < 100) year += 2000;
                Long date = validDate(year, i + 1, Integer.parseInt(word.group(1)));
                if (date != null) return date;
            }
        }
        return null;
    }

    private Long validDate(int year, int month, int day) {
        if (year < 2000 || year > 2099) return null;
        Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setLenient(false);
        calendar.set(year, month - 1, day, 12, 0, 0);
        try { return calendar.getTimeInMillis(); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
