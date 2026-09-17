package com.fadhil.financereceipt.receipt;

import java.util.Calendar;
import java.util.Objects;

/** Data sintetis untuk regresi aturan, bukan pengukuran akurasi OCR pada foto nyata. */
public final class ReceiptParserCases {
    private static int assertions;
    public static void main(String[] args) {
        assertions = 0;
        ReceiptParser parser = new ReceiptParser();
        ReceiptParser.Result r = parser.parse("TOKO MAJU\nJl. Melati 10\n11/09/2026 14:20\n" +
                "Subtotal 25.000\nTOTAL 27.500\nTUNAI 50.000\nKEMBALI 22.500");
        eq("nama toko", "TOKO MAJU", r.storeName);
        eq("total bukan tunai/subtotal", 27500L, r.totalAmount);
        date("tanggal transaksi", r.dateMillis, 2026, 9, 11);
        eq("tanpa peringatan", 0, r.warnings.size());
        eq("rupiah ID", 125000L, parser.parseRupiah("Rp 125.000,00"));
        eq("rupiah gaya EN", 125000L, parser.parseRupiah("125,000.00"));
        eq("ribuan koma", 125000L, parser.parseRupiah("125,000"));
        eq("tanpa pemisah", 500L, parser.parseRupiah("500"));
        eq("rupiah strip", 25000L, parser.parseRupiah("25.000,-"));
        eq("pecahan nonnol tidak dibulatkan", null, parser.parseRupiah("25.000,50"));
        eq("kelompok angka salah", null, parser.parseRupiah("12.34.567"));
        eq("overflow", null, parser.parseRupiah("9999999999999999999999999"));
        eq("negatif", null, parser.parseRupiah("-1000"));
        eq("nol", null, parser.parseRupiah("0"));
        eq("total baris berikut", 35000L, parser.parse("WARUNG ENAK\nTOTAL\nRp 35.000\nTunai 50.000").totalAmount);
        eq("tidak lompat ke tunai", null, parser.parse("WARUNG ENAK\nTOTAL\nTunai 50.000").totalAmount);
        eq("tidak pakai maksimum", null, parser.parse("TOKO MAJU\nHarga 70.000\nTunai 100.000").totalAmount);
        eq("grand total diprioritaskan", 45000L, parser.parse("TOKO MAJU\nTotal 40000\nGrand Total 45000\nBayar 50000").totalAmount);
        eq("jumlah barang dikecualikan", null, parser.parse("Total Item 5\nTotal Barang 5\nSubtotal 25000").totalAmount);
        eq("diskon dikecualikan", null, parser.parse("Total Diskon 5000\nDiskon 10%").totalAmount);
        r = parser.parse("TOKO MAJU\nTOTAL 25000\nTOTAL 30000");
        eq("total terakhir setingkat", 30000L, r.totalAmount);
        eq("peringatan total ganda", true, r.warnings.toString().contains("beberapa kandidat total"));
        eq("format total strip", 25000L, parser.parse("TOTAL Rp25.000,-").totalAmount);
        eq("total tanpa spasi setelah titik dua", 25000L, parser.parse("TOTAL:25000").totalAmount);
        eq("total tanda sama dengan", 25000L, parser.parse("TOTAL=25000").totalAmount);
        eq("waktu bukan nominal", null, parser.parse("TOTAL 14:20").totalAmount);
        eq("jangan ambil bagian tanggal", null, parser.parse("Total 11/09/2026").totalAmount);
        eq("jangan ambil persentase", null, parser.parse("Total 10%").totalAmount);
        eq("jangan ambil angka OCR campuran", null, parser.parse("Total 25.00O").totalAmount);
        eq("tanggal tidak valid", null, parser.parse("31/02/2026").dateMillis);
        eq("bukan tahun kabisat", null, parser.parse("29/02/2025").dateMillis);
        date("tahun kabisat", parser.parse("29/02/2024").dateMillis, 2024, 2, 29);
        date("tanggal ISO", parser.parse("2026-09-11").dateMillis, 2026, 9, 11);
        date("bulan Indonesia", parser.parse("11 September 2026").dateMillis, 2026, 9, 11);
        date("bulan singkat", parser.parse("11-Agt-26").dateMillis, 2026, 8, 11);
        date("bulan Inggris", parser.parse("11 May 2026").dateMillis, 2026, 5, 11);
        date("kedaluwarsa diabaikan", parser.parse("Expired 11/10/2026\n11/09/2026").dateMillis, 2026, 9, 11);
        eq("alamat tidak dianggap toko", "MINIMARKET CERIA", parser.parse("Jl. Mawar 12\nTelp 0812345678\nMINIMARKET CERIA").storeName);
        eq("kosong", null, parser.parse("").totalAmount);
        eq("kosong 3 peringatan", 3, parser.parse("").warnings.size());
        eq("normalisasi spasi", "TOKO MAJU\nTOTAL 25000", parser.parse("  TOKO\t MAJU\r\n\n TOTAL  25000 ").normalizedText);
        System.out.println("PASS: " + assertions + " assertions on synthetic receipt text.");
    }

    private static void eq(String label, Object expected, Object actual) {
        assertions++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(label + ": expected=" + expected + ", actual=" + actual);
    }
    private static void date(String label, Long millis, int y, int m, int d) {
        if (millis == null) throw new AssertionError(label + ": tanggal tidak ditemukan");
        Calendar calendar = Calendar.getInstance(); calendar.setTimeInMillis(millis);
        eq(label + " year", y, calendar.get(Calendar.YEAR));
        eq(label + " month", m, calendar.get(Calendar.MONTH) + 1);
        eq(label + " day", d, calendar.get(Calendar.DAY_OF_MONTH));
    }
}
