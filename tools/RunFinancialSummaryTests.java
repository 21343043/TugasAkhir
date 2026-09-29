import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import javax.tools.ToolProvider;

/** Dari akar project: java tools/RunFinancialSummaryTests.java (JDK 11+). */
class RunFinancialSummaryTests {
    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory("financial-summary-tests-");
        try {
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("Gunakan JDK 11+, bukan JRE saja.");
            String packagePath = "java/com/fadhil/financereceipt/utils/";
            int code = compiler.run(null, System.out, System.err, "--release", "11", "-encoding", "UTF-8",
                    "-d", output.toString(), "app/src/main/" + packagePath + "FinancialSummaryCalculator.java",
                    "app/src/test/" + packagePath + "FinancialSummaryCases.java");
            if (code != 0) throw new IllegalStateException("Kompilasi gagal: " + code);
            try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
                loader.loadClass("com.fadhil.financereceipt.utils.FinancialSummaryCases")
                        .getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            }
        } finally {
            try (var files = Files.walk(output)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }
}
