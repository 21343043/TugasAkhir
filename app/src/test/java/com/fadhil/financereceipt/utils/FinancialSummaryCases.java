package com.fadhil.financereceipt.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import static com.fadhil.financereceipt.utils.FinancialSummaryCalculator.*;

/** Fixture sintetis untuk logika dashboard; bukan bukti hasil penelitian. */
public final class FinancialSummaryCases {
    private static int assertions;
    private static final long START = date(2026, 9, 1);
    private static final long END = date(2026, 10, 1);
    public static void main(String[] args) {
        assertions = 0;
        Summary balanced = calc(income(10000000), expense(5000000, "NEEDS"),
                expense(3000000, "WANTS"), expense(2000000, "SAVINGS"));
        eq("pemasukan", BigInteger.valueOf(10000000), balanced.totalIncome);
        eq("pengeluaran", BigInteger.valueOf(10000000), balanced.totalExpense);
        eq("saldo", BigInteger.ZERO, balanced.balance);
        eq("jumlah", 4, balanced.transactionCount);
        eq("needs tepat", Status.AT_LIMIT, group(balanced, Group.NEEDS).status);
        eq("wants tepat", Status.AT_LIMIT, group(balanced, Group.WANTS).status);
        eq("savings tepat", Status.TARGET_REACHED, group(balanced, Group.SAVINGS).status);
        percent("pie needs", "50.00", group(balanced, Group.NEEDS).expensePercentage);
        percent("pie wants", "30.00", group(balanced, Group.WANTS).expensePercentage);
        percent("pie savings", "20.00", group(balanced, Group.SAVINGS).expensePercentage);

        Summary differing = calc(income(10000000), expense(2500000,"NEEDS"),
                expense(1500000,"WANTS"), expense(1000000,"SAVINGS"));
        percent("komposisi 50", "50.00", group(differing,Group.NEEDS).expensePercentage);
        percent("terhadap income 25", "25.00", group(differing,Group.NEEDS).incomePercentage);
        eq("pie savings 20 bukan target tercapai", Status.BELOW_TARGET, group(differing,Group.SAVINGS).status);
        eq("saldo positif", BigInteger.valueOf(5000000), differing.balance);

        Summary over = calc(income(10000000), expense(5000001,"NEEDS"),
                expense(3000001,"WANTS"), expense(1999999,"SAVINGS"));
        eq("needs 1 rupiah lewat", Status.OVER_LIMIT, group(over,Group.NEEDS).status);
        eq("wants 1 rupiah lewat", Status.OVER_LIMIT, group(over,Group.WANTS).status);
        eq("savings 1 rupiah kurang", Status.BELOW_TARGET, group(over,Group.SAVINGS).status);
        percent("tampilan pembulatan tetap 50", "50.00", group(over,Group.NEEDS).incomePercentage);
        eq("saldo negatif", BigInteger.valueOf(-1), over.balance);
        eq("savings di atas target", Status.TARGET_REACHED,
                group(calc(income(100),expense(21,"SAVINGS")),Group.SAVINGS).status);
        eq("needs di bawah", Status.WITHIN_LIMIT,
                group(calc(income(100),expense(49,"NEEDS")),Group.NEEDS).status);

        Summary noIncome = calc(expense(100,"NEEDS"));
        eq("income nol", BigInteger.ZERO, noIncome.totalIncome);
        eq("tidak dinilai", Status.NO_INCOME, group(noIncome,Group.NEEDS).status);
        eq("rasio undefined", null, group(noIncome,Group.NEEDS).incomePercentage);
        percent("single slice 100", "100.00", group(noIncome,Group.NEEDS).expensePercentage);
        Summary empty = calc();
        eq("empty count",0,empty.transactionCount);
        eq("empty expense",BigInteger.ZERO,empty.totalExpense);
        for (GroupSummary g:empty.groups) {
            eq("empty percentage " + g.group,null,g.expensePercentage);
            eq("empty fraction " + g.group,0f,g.expenseFraction);
        }
        Summary incomeOnly=calc(income(100));
        eq("tanpa pengeluaran",BigInteger.valueOf(100),incomeOnly.balance);
        eq("savings belum",Status.BELOW_TARGET,group(incomeOnly,Group.SAVINGS).status);

        Summary unknown=calc(income(1000),expense(200,"NEEDS"),expense(100,null),expense(100,"UNKNOWN"));
        eq("unknown flag",true,unknown.hasUngroupedExpenses);
        eq("unknown amount",BigInteger.valueOf(200),group(unknown,Group.UNGROUPED).amount);
        percent("unknown share included","50.00",group(unknown,Group.UNGROUPED).expensePercentage);
        eq("unknown status",Status.UNCLASSIFIED,group(unknown,Group.UNGROUPED).status);
        eq("semua kelompok cocok total",unknown.totalExpense,unknown.groups.stream()
                .map(g->g.amount).reduce(BigInteger.ZERO,BigInteger::add));
        // Nama kelompok yang salah tidak diam-diam dipetakan ke Needs.
        eq("case unknown",Group.UNGROUPED,Group.fromValue("needs"));
        eq("income tidak ikut pie",BigInteger.ZERO,
                group(calc(new Entry(START,"income",100,"NEEDS")),Group.NEEDS).amount);

        Summary boundary = calc(new Entry(START-1,"income",7,null),new Entry(START,"income",100,null),
                new Entry(END-1,"expense",30,"WANTS"),new Entry(END,"expense",50,"NEEDS"));
        eq("awal termasuk",BigInteger.valueOf(100),boundary.totalIncome);
        eq("akhir eksklusif",BigInteger.valueOf(30),boundary.totalExpense);
        eq("count batas",2,boundary.transactionCount);
        long feb=date(2024,2,1),mar=date(2024,3,1);
        Summary leap=FinancialSummaryCalculator.calculate(Arrays.asList(
                new Entry(date(2024,2,29),"expense",10,"NEEDS"),
                new Entry(mar,"expense",20,"NEEDS")),feb,mar);
        eq("tahun kabisat",BigInteger.TEN,leap.totalExpense);
        long dec=date(2026,12,1),jan=date(2027,1,1);
        eq("lintas tahun",BigInteger.TEN,FinancialSummaryCalculator.calculate(Arrays.asList(
                new Entry(jan-1,"expense",10,"NEEDS"),new Entry(jan,"expense",20,"NEEDS")),dec,jan).totalExpense);

        Summary large=calc(income(Long.MAX_VALUE),income(Long.MAX_VALUE),expense(Long.MAX_VALUE,"NEEDS"));
        eq("income tak overflow",BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO),large.totalIncome);
        eq("saldo tak overflow",BigInteger.valueOf(Long.MAX_VALUE),large.balance);
        eq("batas nilai besar",Status.AT_LIMIT,group(large,Group.NEEDS).status);

        Summary thirds=calc(income(10),expense(1,"NEEDS"),expense(1,"WANTS"),expense(1,"SAVINGS"));
        percent("persen sepertiga","33.33",group(thirds,Group.NEEDS).expensePercentage);
        float sum=0f;for(GroupSummary g:thirds.groups) sum+=g.expenseFraction;
        eq("fraksi tidak 99.99 persen",true,Math.abs(sum-1f)<0.00001f);

        List<Entry> changing=new ArrayList<>();changing.add(income(100));changing.add(expense(50,"NEEDS"));
        eq("sebelum edit",Status.AT_LIMIT,group(FinancialSummaryCalculator.calculate(changing,START,END),Group.NEEDS).status);
        changing.set(1,expense(60,"WANTS"));
        Summary edited=FinancialSummaryCalculator.calculate(changing,START,END);
        eq("edit pindah kelompok",BigInteger.ZERO,group(edited,Group.NEEDS).amount);
        eq("edit indikator",Status.OVER_LIMIT,group(edited,Group.WANTS).status);
        changing.remove(1);
        eq("hapus menghitung ulang",BigInteger.ZERO,FinancialSummaryCalculator.calculate(changing,START,END).totalExpense);
        rejected("nominal nol",()->calc(expense(0,"NEEDS")));
        rejected("nominal negatif",()->calc(expense(-1,"NEEDS")));
        rejected("tipe tidak dikenal",()->calc(new Entry(START,"transfer",100,null)));
        rejected("periode invalid",()->FinancialSummaryCalculator.calculate(Collections.emptyList(),END,START));
        System.out.println("PASS: " + assertions + " financial-summary assertions.");
    }
    private static Entry income(long amount){return new Entry(START,"income",amount,null);}
    private static Entry expense(long amount,String group){return new Entry(START,"expense",amount,group);}
    private static Summary calc(Entry... rows){return FinancialSummaryCalculator.calculate(Arrays.asList(rows),START,END);}
    private static GroupSummary group(Summary summary,Group group){return summary.groups.stream().filter(g->g.group==group).findFirst().orElseThrow();}
    private static void eq(String label,Object expected,Object actual){assertions++;if(!Objects.equals(expected,actual))throw new AssertionError(label+": expected="+expected+", actual="+actual);}
    private static void percent(String label,String expected,BigDecimal actual){eq(label,new BigDecimal(expected),actual);}
    private static void rejected(String label,Runnable action){boolean failed=false;try{action.run();}catch(IllegalArgumentException expected){failed=true;}eq(label,true,failed);}
    private static long date(int year,int month,int day){Calendar c=new GregorianCalendar(TimeZone.getTimeZone("Asia/Jakarta"));c.clear();c.set(year,month-1,day,0,0,0);return c.getTimeInMillis();}
}
