import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;
import javax.tools.ToolProvider;

/** Jalankan dari akar proyek: java tools/RunReceiptParserTests.java (JDK 11+). */
class RunReceiptParserTests {
    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory("receipt-parser-tests-");
        try {
            String root = "app/src/";
            String source = "java/com/fadhil/financereceipt/receipt/";
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException("Gunakan JDK, bukan JRE saja.");
            int code = compiler.run(null, System.out, System.err, "--release", "11", "-encoding", "UTF-8",
                    "-d", output.toString(), root + "main/" + source + "ReceiptParser.java",
                    root + "test/" + source + "ReceiptParserCases.java");
            if (code != 0) throw new IllegalStateException("Kompilasi parser gagal: " + code);
            try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
                loader.loadClass("com.fadhil.financereceipt.receipt.ReceiptParserCases")
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
