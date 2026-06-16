package bytecodetool;

import bytecodetool.model.*;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;
import bytecodetool.transform.ClassTransformer;
import bytecodetool.transform.ClassTransformer.PrintInstrumenter;
import bytecodetool.util.Disassembler;
import bytecodetool.util.DiffTool;
import bytecodetool.writer.ClassWriter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class BytecodeTool {

    public static class CliOptions {
        public String mode;
        public String inputPath;
        public String inputPath2;
        public String methodSelector;
        public String message;
        public Path outputDir;
        public Path outputFile;
        public boolean noVerify;
    }

    public static void main(String[] args) throws Exception {
        CliOptions opts = parseArgs(args);
        if (opts == null) {
            printUsage();
            return;
        }

        if ("diff".equals(opts.mode)) {
            runDiff(opts);
            return;
        }

        Path input = Paths.get(opts.inputPath);
        if (!Files.exists(input)) {
            System.err.println("Error: file not found: " + opts.inputPath);
            System.exit(1);
        }

        byte[] data = Files.readAllBytes(input);
        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();
        String className = pool.getClassName(cf.thisClass).replace('/', '.');

        switch (opts.mode) {
            case "disasm":
            case "d":
                doDisassemble(className, cf, pool);
                break;

            case "roundtrip":
            case "r":
                doRoundtrip(input, className, cf, pool, opts);
                break;

            case "instrument":
            case "i":
                if (opts.methodSelector == null) {
                    System.err.println("Error: instrument mode requires a method selector");
                    System.err.println("       Usage:  BytecodeTool instrument <class> <selector> [message]");
                    System.err.println("       selector can be: name | name(desc) | *");
                    System.exit(1);
                }
                if (opts.message == null) opts.message = "[Instrument " + opts.methodSelector + "]";
                doInstrument(input, className, cf, pool, opts);
                break;

            default:
                System.err.println("Unknown mode: " + opts.mode);
                printUsage();
                System.exit(1);
        }
    }

    private static CliOptions parseArgs(String[] args) {
        if (args.length < 1) return null;
        CliOptions opts = new CliOptions();
        opts.mode = args[0];

        List<String> positional = new ArrayList<>();
        int i = 1;
        while (i < args.length) {
            String a = args[i];
            if ("-o".equals(a) || "--output".equals(a)) {
                if (i + 1 >= args.length) { System.err.println("Missing value for " + a); return null; }
                opts.outputFile = Paths.get(args[++i]);
            } else if ("-d".equals(a) || "--dir".equals(a)) {
                if (i + 1 >= args.length) { System.err.println("Missing value for " + a); return null; }
                opts.outputDir = Paths.get(args[++i]);
            } else if ("--no-verify".equals(a)) {
                opts.noVerify = true;
            } else if (a.startsWith("-")) {
                System.err.println("Unknown option: " + a);
                return null;
            } else {
                positional.add(a);
            }
            i++;
        }

        if (positional.isEmpty()) return null;
        opts.inputPath = positional.get(0);

        if ("diff".equals(opts.mode)) {
            if (positional.size() < 2) { System.err.println("diff mode needs 2 class files"); return null; }
            opts.inputPath2 = positional.get(1);
            return opts;
        }

        if (positional.size() >= 2) opts.methodSelector = positional.get(1);
        if (positional.size() >= 3) opts.message = positional.get(2);
        return opts;
    }

    private static void runDiff(CliOptions opts) throws Exception {
        Path a = Paths.get(opts.inputPath);
        Path b = Paths.get(opts.inputPath2);
        if (!Files.exists(a)) { System.err.println("Not found: " + a); System.exit(1); }
        if (!Files.exists(b)) { System.err.println("Not found: " + b); System.exit(1); }
        DiffTool.diff(a, b);
    }

    private static void doDisassemble(String className, ClassFile cf, ConstantPool pool) {
        System.out.println(Disassembler.disassemble(cf, pool));
    }

    private static Path resolveOutput(Path input, CliOptions opts, String suffix, String className) throws Exception {
        String base = simpleName(className);
        String defName = base + "." + suffix + ".class";
        Path out;
        if (opts.outputFile != null) {
            out = opts.outputFile;
            if (opts.outputDir != null) out = opts.outputDir.resolve(out.getFileName());
        } else if (opts.outputDir != null) {
            Files.createDirectories(opts.outputDir);
            out = opts.outputDir.resolve(defName);
        } else {
            out = input.getParent().resolve(defName);
        }
        if (opts.outputFile == null && Files.exists(out)) {
            int n = 1;
            while (true) {
                String alt = base + "." + suffix + "." + n + ".class";
                Path p = (opts.outputDir != null ? opts.outputDir : input.getParent()).resolve(alt);
                if (!Files.exists(p)) { out = p; break; }
                n++;
            }
        }
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        return out;
    }

    private static boolean verifyLoadable(Path classFile, String className) {
        try {
            byte[] bytes = Files.readAllBytes(classFile);
            Path tmpRoot = Files.createTempDirectory("bctverify_");
            try {
                String pkg = "";
                String simple = className;
                int dot = className.lastIndexOf('.');
                if (dot >= 0) { pkg = className.substring(0, dot); simple = className.substring(dot + 1); }
                Path pkgDir = pkg.isEmpty() ? tmpRoot : tmpRoot.resolve(pkg.replace('.', File.separatorChar));
                Files.createDirectories(pkgDir);
                Files.write(pkgDir.resolve(simple + ".class"), bytes);
                URL url = tmpRoot.toUri().toURL();
                try (URLClassLoader cl = new URLClassLoader(new URL[]{url}, null)) {
                    Class<?> c = cl.loadClass(className);
                    return c != null;
                }
            } finally {
                try {
                    Files.walk(tmpRoot)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (Exception ignore) {} });
                } catch (Exception ignore) {}
            }
        } catch (Throwable t) {
            String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
            if (msg.length() > 80) msg = msg.substring(0, 77) + "...";
            System.out.println("  verify error: " + msg);
            return false;
        }
    }

    private static void writeAndReport(Path input, Path output, byte[] out, String className, CliOptions opts) throws Exception {
        Files.write(output, out);
        System.out.println("Output: " + output.toAbsolutePath());
        System.out.println("  input size : " + Files.size(input) + " bytes");
        System.out.println("  output size: " + out.length + " bytes");
        if (!opts.noVerify) {
            String actualClass = deduceClassName(output);
            boolean ok = verifyLoadable(output, actualClass);
            System.out.println("  JVM load  : " + (ok ? "OK" : "FAILED"));
        }
    }

    private static String deduceClassName(Path p) {
        try {
            byte[] d = Files.readAllBytes(p);
            ClassReader cr = new ClassReader(d);
            ClassFile cf = cr.read();
            ConstantPool pool = new ClassReader(d).readConstantPool();
            return pool.getClassName(cf.thisClass).replace('/', '.');
        } catch (Exception e) { return "Unknown"; }
    }

    private static void doRoundtrip(Path input, String className, ClassFile cf, ConstantPool pool, CliOptions opts) throws Exception {
        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();
        Path output = resolveOutput(input, opts, "roundtrip", className);
        writeAndReport(input, output, out, className, opts);
    }

    static boolean methodMatches(String selector, String name, String desc) {
        if ("*".equals(selector)) return true;
        if (selector.contains("(")) {
            int lp = selector.indexOf('(');
            String selName = selector.substring(0, lp);
            String selDesc = selector.substring(lp);
            return selName.equals(name) && selDesc.equals(desc);
        }
        return selector.equals(name);
    }

    private static void doInstrument(Path input, String className, ClassFile cf, ConstantPool pool, CliOptions opts) throws Exception {
        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter(opts.message, pool);
        List<String> instrumented = new ArrayList<>();
        Set<String> attempted = new LinkedHashSet<>();
        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            String desc = pool.getUtf8(m.descriptorIndex);
            attempted.add(name + desc);
            if (!methodMatches(opts.methodSelector, name, desc)) continue;
            if (m.getCodeAttribute() == null) continue;
            tx.instrumentMethod(name, desc, instr);
            instrumented.add(name + desc);
        }
        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();
        Path output = resolveOutput(input, opts, "instrumented", className);
        writeAndReport(input, output, out, className, opts);
        System.out.println("  instrumented: " + instrumented.size() + " methods");
        for (String s : instrumented) System.out.println("    - " + s);
        if (instrumented.isEmpty()) {
            System.out.println("  (selector '" + opts.methodSelector + "' matched no concrete methods)");
            System.out.println("  available methods:");
            for (String s : attempted) System.out.println("    - " + s);
        }
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    private static void printUsage() {
        System.out.println("BytecodeTool - a minimal Java class file tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  BytecodeTool disasm|d     <classfile>                          disassemble like javap");
        System.out.println("  BytecodeTool roundtrip|r  <classfile> [-o <file>] [-d <dir>]   read and write back");
        System.out.println("  BytecodeTool instrument|i <classfile> <selector> [msg] [opts]  add entry println");
        System.out.println("       selector: name | name(desc) | *");
        System.out.println("       ex : foo | foo(I)V | *");
        System.out.println("  BytecodeTool diff         <class1> <class2>                    show diff of 2 class files");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o, --output <file>     specify output file path");
        System.out.println("  -d, --dir    <dir>      specify output directory");
        System.out.println("      --no-verify         skip JVM class loading verification");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  - Default output names use .roundtrip.class / .instrumented.class suffix");
        System.out.println("  - If default output exists, .1.class .2.class ... is used to avoid overwrite");
    }
}
