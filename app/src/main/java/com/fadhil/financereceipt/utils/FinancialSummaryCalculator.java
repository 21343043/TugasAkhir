package com.fadhil.financereceipt.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/** Logika murni JVM. Tidak mengakses Android, database, OCR, atau NER. */
public final class FinancialSummaryCalculator {
    private static final BigInteger HUNDRED = BigInteger.valueOf(100);
    private FinancialSummaryCalculator() { }

    public enum Group {
        NEEDS("Needs", 50), WANTS("Wants", 30), SAVINGS("Savings", 20),
        UNGROUPED("Belum dikelompokkan", 0);
        public final String label;
        public final int targetPercentage;
        Group(String label, int targetPercentage) {
            this.label = label;
            this.targetPercentage = targetPercentage;
        }
        public static Group fromValue(String value) {
            if ("NEEDS".equals(value)) return NEEDS;
            if ("WANTS".equals(value)) return WANTS;
            if ("SAVINGS".equals(value)) return SAVINGS;
            return UNGROUPED;
        }
    }

    public enum Status {
        NO_INCOME, WITHIN_LIMIT, AT_LIMIT, OVER_LIMIT,
        BELOW_TARGET, TARGET_REACHED, UNCLASSIFIED
    }

    public static final class Entry {
        public final long transactionDate;
        public final String transactionType;
        public final long amount;
        public final String financialGroup;
        public Entry(long transactionDate, String transactionType, long amount, String financialGroup) {
            this.transactionDate = transactionDate;
            this.transactionType = transactionType;
            this.amount = amount;
            this.financialGroup = financialGroup;
        }
    }

    public static final class GroupSummary {
        public final Group group;
        public final BigInteger amount;
        /** null jika tidak ada pengeluaran; bukan komposisi 0% yang terukur. */
        public final BigDecimal expensePercentage;
        /** null jika tidak ada pemasukan; jangan membagi dengan nol. */
        public final BigDecimal incomePercentage;
        public final Status status;
        /** Fraksi tak dibulatkan ke persen tampilan, untuk sudut pie chart. */
        public final float expenseFraction;
        private GroupSummary(Group group, BigInteger amount, BigInteger expense, BigInteger income) {
            this.group = group;
            this.amount = amount;
            this.expensePercentage = percentage(amount, expense);
            this.incomePercentage = percentage(amount, income);
            this.expenseFraction = expense.signum() == 0 ? 0f : new BigDecimal(amount)
                    .divide(new BigDecimal(expense), 12, RoundingMode.HALF_UP).floatValue();
            this.status = status(group, amount, income);
        }
    }

    public static final class Summary {
        public final BigInteger totalIncome;
        public final BigInteger totalExpense;
        public final BigInteger balance;
        public final List<GroupSummary> groups;
        public final int transactionCount;
        public final boolean hasUngroupedExpenses;
        private Summary(BigInteger income, BigInteger expense, EnumMap<Group, BigInteger> amounts,
                        int count) {
            totalIncome = income;
            totalExpense = expense;
            balance = income.subtract(expense);
            transactionCount = count;
            hasUngroupedExpenses = amounts.get(Group.UNGROUPED).signum() > 0;
            List<GroupSummary> result = new ArrayList<>();
            for (Group group : Group.values()) {
                result.add(new GroupSummary(group, amounts.get(group), expense, income));
            }
            groups = Collections.unmodifiableList(result);
        }
    }

    /** start inklusif, endExclusive eksklusif; batas kalender disediakan pemanggil. */
    public static Summary calculate(List<Entry> entries, long start, long endExclusive) {
        if (start >= endExclusive) throw new IllegalArgumentException("Rentang tanggal tidak valid.");
        BigInteger income = BigInteger.ZERO;
        BigInteger expense = BigInteger.ZERO;
        EnumMap<Group, BigInteger> amounts = new EnumMap<>(Group.class);
        for (Group group : Group.values()) amounts.put(group, BigInteger.ZERO);
        int count = 0;
        for (Entry entry : entries) {
            if (entry.transactionDate < start || entry.transactionDate >= endExclusive) continue;
            if (entry.amount <= 0) throw new IllegalArgumentException("Nominal transaksi tidak valid.");
            BigInteger amount = BigInteger.valueOf(entry.amount);
            if ("income".equals(entry.transactionType)) {
                income = income.add(amount);
            } else if ("expense".equals(entry.transactionType)) {
                expense = expense.add(amount);
                Group group = Group.fromValue(entry.financialGroup);
                amounts.put(group, amounts.get(group).add(amount));
            } else {
                throw new IllegalArgumentException("Jenis transaksi tidak valid.");
            }
            count++;
        }
        return new Summary(income, expense, amounts, count);
    }

    private static BigDecimal percentage(BigInteger amount, BigInteger denominator) {
        if (denominator.signum() == 0) return null;
        return new BigDecimal(amount.multiply(HUNDRED))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
    }

    private static Status status(Group group, BigInteger amount, BigInteger income) {
        if (group == Group.UNGROUPED) return Status.UNCLASSIFIED;
        if (income.signum() == 0) return Status.NO_INCOME;
        // Bandingkan rupiah sebelum pembulatan persentase agar batas tetap tepat.
        int comparison = amount.multiply(HUNDRED)
                .compareTo(income.multiply(BigInteger.valueOf(group.targetPercentage)));
        if (group == Group.SAVINGS) return comparison >= 0 ? Status.TARGET_REACHED : Status.BELOW_TARGET;
        if (comparison > 0) return Status.OVER_LIMIT;
        return comparison == 0 ? Status.AT_LIMIT : Status.WITHIN_LIMIT;
    }
}
