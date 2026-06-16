package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;
import bytecodetool.transform.ClassTransformer;
import bytecodetool.transform.ClassTransformer.PrintInstrumenter;
import bytecodetool.writer.ClassWriter;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Verify {

    static final String JAVAC = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\javac.exe";
    static final String JAVA  = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\java.exe";

    static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        Path work = Paths.get("target/verify");
        Files.createDirectories(work);

        test(1, "最小class: 读取->写回->JVM加载", () -> testRoundtrip(work));
        test(2, "multianewarray 解析+写回 (4字节为正确长度)", () -> testMultiArray(work));
        test(3, "switch 插桩后跳转正确", () -> testSwitch(work));
        test(4, "if/catch/finally 插桩 (含真正抛出异常的分支)", () -> testIfCatchFinally(work));
        test(5, "构造方法/<init>/static块/<clinit>/多返回类型 插桩", () -> testMoreMethods(work));
        test(6, "CLI 反汇编输出", () -> testCLIDisasm(work));
        test(7, "CLI roundtrip 输出", () -> testCLIRoundtrip(work));
        test(8, "精确方法选择（重载：同名不同描述符只改一个）", () -> testExactMethodSelect(work));
        test(9, "控制流样例反汇编（if/try-catch-finally/字符串switch/稀疏int switch=lookupswitch）", () -> testControlFlowSamples(work));
        test(10, "对比模式 diff（常量池新增/方法字节码差异）", () -> testDiffMode(work));
        test(11, "输出文件管理（不覆盖、指定目录、JVM加载验证）", () -> testOutputMgmt(work));

        System.out.println("====== 结果: PASS=" + pass + " FAIL=" + fail + " ======");
    }

    interface TestBody {
        void run() throws Exception;
    }

    static void test(int idx, String name, TestBody runnable) {
        System.out.println("====== " + idx + ". " + name + " ======");
        try {
            runnable.run();
            System.out.println("  [PASS]\n");
            pass++;
        } catch (Throwable e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            System.out.println("  [FAIL] " + msg + "\n");
            fail++;
        }
    }

    static Object loadAndInvoke(Path classDir, String className, String methodName,
                                Class<?>[] paramTypes, Object[] args) throws Exception {
        URL url = classDir.toUri().toURL();
        URLClassLoader ucl = new URLClassLoader(new URL[]{url}, Verify.class.getClassLoader());
        Class<?> cls = ucl.loadClass(className);
        Object instance = cls.getDeclaredConstructor().newInstance();
        Method m = cls.getMethod(methodName, paramTypes);
        return m.invoke(instance, args);
    }

    static void testRoundtrip(Path work) throws Exception {
        Path src = work.resolve("Tiny.java");
        Files.write(src, (
            "package p;\npublic class Tiny {\n" +
            "  public int m(int x) { return x + 1; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Tiny.class");
        byte[] orig = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(orig);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(orig).readConstantPool();

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] rewritten = cw.write();

        Path outDir = work.resolve("roundtrip");
        Files.createDirectories(outDir.resolve("p"));
        Files.write(outDir.resolve("p").resolve("Tiny.class"), rewritten);

        Object result = loadAndInvoke(outDir, "p.Tiny", "m", new Class[]{int.class}, new Object[]{41});
        if (!Integer.valueOf(42).equals(result))
            throw new Exception("roundtrip m(41) should be 42, got " + result);
        System.out.println("  roundtrip: Tiny.m(41) = " + result + " (expected 42) OK");
    }

    static void testMultiArray(Path work) throws Exception {
        Path src = work.resolve("Multi.java");
        Files.write(src, (
            "package p;\npublic class Multi {\n" +
            "  public int[][] create() { return new int[2][3]; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Multi.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (!"create".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            List<Instruction> insns = BytecodeParser.parse(ca.code);

            Instruction multi = null;
            for (Instruction i : insns) if (i.opcode == Opcodes.MULTIANEWARRAY) multi = i;
            if (multi == null) throw new Exception("multianewarray not found");

            System.out.println("  multianewarray: offset=" + multi.offset + " len=" + multi.length);

            int nextPc = multi.offset + multi.length;
            Instruction next = null;
            for (Instruction i : insns) if (i.offset == nextPc) next = i;
            if (next == null) throw new Exception("next instruction not found at pc=" + nextPc);
            System.out.println("  next insn: pc=" + nextPc + " " + Opcodes.getOpcodeName(next.opcode));

            if (multi.length != 4)
                throw new Exception("multianewarray should be 4 bytes (opcode+2 index+1 dims), got " + multi.length);
            System.out.println("  multianewarray length=4 is correct (matches JVM spec)");
        }

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] rewritten = cw.write();

        Path outDir = work.resolve("multi");
        Files.createDirectories(outDir.resolve("p"));
        Files.write(outDir.resolve("p").resolve("Multi.class"), rewritten);

        Object result = loadAndInvoke(outDir, "p.Multi", "create", new Class[0], new Object[0]);
        int[][] arr = (int[][]) result;
        if (arr.length != 2 || arr[0].length != 3)
            throw new Exception("multi array dims wrong: " + arr.length + "x" + arr[0].length);
        System.out.println("  multi roundtrip: int[2][3] OK");
    }

    static void testSwitch(Path work) throws Exception {
        Path src = work.resolve("Sw.java");
        Files.write(src, (
            "package p;\npublic class Sw {\n" +
            "  public int intSwitch(int x) {\n" +
            "    switch(x) { case 1: return 10; case 2: return 20; case 5: return 50; default: return -1; }\n" +
            "  }\n" +
            "  public String strSwitch(String s) {\n" +
            "    switch(s) { case \"a\": return \"A\"; case \"b\": return \"B\"; default: return \"?\"; }\n" +
            "  }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Sw.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[SW]", pool);
        tx.instrumentMethod("intSwitch", null, instr);
        tx.instrumentMethod("strSwitch", null, instr);

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();

        Path outDir = work.resolve("switch");
        Files.createDirectories(outDir.resolve("p"));
        Files.write(outDir.resolve("p").resolve("Sw.class"), out);

        Object r1 = loadAndInvoke(outDir, "p.Sw", "intSwitch", new Class[]{int.class}, new Object[]{1});
        Object r2 = loadAndInvoke(outDir, "p.Sw", "intSwitch", new Class[]{int.class}, new Object[]{2});
        Object r3 = loadAndInvoke(outDir, "p.Sw", "intSwitch", new Class[]{int.class}, new Object[]{5});
        Object r4 = loadAndInvoke(outDir, "p.Sw", "intSwitch", new Class[]{int.class}, new Object[]{99});
        System.out.println("  intSwitch(1)=" + r1 + " (2)=" + r2 + " (5)=" + r3 + " (99)=" + r4);
        if (!Integer.valueOf(10).equals(r1)) throw new Exception("case 1 should return 10, got " + r1);
        if (!Integer.valueOf(20).equals(r2)) throw new Exception("case 2 should return 20, got " + r2);
        if (!Integer.valueOf(50).equals(r3)) throw new Exception("case 5 should return 50, got " + r3);
        if (!Integer.valueOf(-1).equals(r4)) throw new Exception("default should return -1, got " + r4);

        Object s1 = loadAndInvoke(outDir, "p.Sw", "strSwitch", new Class[]{String.class}, new Object[]{"a"});
        Object s2 = loadAndInvoke(outDir, "p.Sw", "strSwitch", new Class[]{String.class}, new Object[]{"b"});
        Object s3 = loadAndInvoke(outDir, "p.Sw", "strSwitch", new Class[]{String.class}, new Object[]{"c"});
        System.out.println("  strSwitch(a)=" + s1 + " (b)=" + s2 + " (c)=" + s3);
        if (!"A".equals(s1)) throw new Exception("case 'a' should return A, got " + s1);
        if (!"B".equals(s2)) throw new Exception("case 'b' should return B, got " + s2);
        if (!"?".equals(s3)) throw new Exception("default should return ?, got " + s3);
        System.out.println("  All switch cases verified correct");
    }

    static void testIfCatchFinally(Path work) throws Exception {
        Path src = work.resolve("ICF.java");
        Files.write(src, (
            "package p;\npublic class ICF {\n" +
            "  public int process(int x, boolean throwIt) {\n" +
            "    int r = 0;\n" +
            "    try {\n" +
            "      if (throwIt) { throw new RuntimeException(\"bang\"); }\n" +
            "      if (x > 0) { r = x * 2; } else { r = -x; }\n" +
            "    } catch (Exception e) {\n" +
            "      r = -99;\n" +
            "    } finally {\n" +
            "      r += 1;\n" +
            "    }\n" +
            "    return r;\n" +
            "  }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("ICF.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[ICF]", pool);
        tx.instrumentMethod("process", null, instr);

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();

        Path outDir = work.resolve("icf");
        Files.createDirectories(outDir.resolve("p"));
        Files.write(outDir.resolve("p").resolve("ICF.class"), out);

        Object pos = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class, boolean.class}, new Object[]{5, false});
        Object neg = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class, boolean.class}, new Object[]{-3, false});
        Object zero = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class, boolean.class}, new Object[]{0, false});
        Object caught = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class, boolean.class}, new Object[]{5, true});
        System.out.println("  process(5,false)=" + pos + " (-3,false)=" + neg + " (0,false)=" + zero + " (5,true)=" + caught);
        if (!Integer.valueOf(11).equals(pos)) throw new Exception("if-branch: expected 11 (5*2+1), got " + pos);
        if (!Integer.valueOf(4).equals(neg)) throw new Exception("else-branch: expected 4 (-(-3)+1), got " + neg);
        if (!Integer.valueOf(1).equals(zero)) throw new Exception("zero-branch: expected 1 (-0+1), got " + zero);
        if (!Integer.valueOf(-98).equals(caught)) throw new Exception("catch-branch: expected -98 (-99+1), got " + caught);
        System.out.println("  All branches (if/else/catch/finally) verified correct");
    }

    static void testMoreMethods(Path work) throws Exception {
        Path src = work.resolve("Many.java");
        Files.write(src, (
            "package p;\npublic class Many {\n" +
            "  static { System.out.println(\"<clinit> running\"); }\n" +
            "  public Many() { System.out.println(\"<init> running\"); }\n" +
            "  public int retInt() { return 42; }\n" +
            "  public long retLong() { return 123456789012L; }\n" +
            "  public float retFloat() { return 3.14f; }\n" +
            "  public double retDouble() { return 2.71828; }\n" +
            "  public String retString() { return \"hello\"; }\n" +
            "  public void retVoid() { /* noop */ }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Many.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[Many]", pool);

        Set<String> instrumented = new LinkedHashSet<>();
        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (m.getCodeAttribute() != null) {
                tx.instrumentMethod(name, null, instr);
                instrumented.add(name);
            }
        }
        System.out.println("  instrumented methods: " + instrumented);

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();

        Path outDir = work.resolve("many");
        Files.createDirectories(outDir.resolve("p"));
        Files.write(outDir.resolve("p").resolve("Many.class"), out);

        URL url = outDir.toUri().toURL();
        URLClassLoader ucl = new URLClassLoader(new URL[]{url}, Verify.class.getClassLoader());
        Class<?> cls2 = ucl.loadClass("p.Many");
        Object instance = cls2.getDeclaredConstructor().newInstance();

        Object rInt = cls2.getMethod("retInt").invoke(instance);
        Object rLong = cls2.getMethod("retLong").invoke(instance);
        Object rFloat = cls2.getMethod("retFloat").invoke(instance);
        Object rDouble = cls2.getMethod("retDouble").invoke(instance);
        Object rString = cls2.getMethod("retString").invoke(instance);
        cls2.getMethod("retVoid").invoke(instance);

        System.out.println("  retInt   = " + rInt);
        System.out.println("  retLong  = " + rLong);
        System.out.println("  retFloat = " + rFloat);
        System.out.println("  retDouble= " + rDouble);
        System.out.println("  retString= " + rString);

        if (!Integer.valueOf(42).equals(rInt)) throw new Exception("retInt wrong: " + rInt);
        if (!Long.valueOf(123456789012L).equals(rLong)) throw new Exception("retLong wrong: " + rLong);
        if (!(rFloat instanceof Float && Math.abs((Float) rFloat - 3.14f) < 0.0001f))
            throw new Exception("retFloat wrong: " + rFloat);
        if (!(rDouble instanceof Double && Math.abs((Double) rDouble - 2.71828) < 0.000001))
            throw new Exception("retDouble wrong: " + rDouble);
        if (!"hello".equals(rString)) throw new Exception("retString wrong: " + rString);

        System.out.println("  All return types + constructor + static block verified");
    }

    static void testCLIDisasm(Path work) throws Exception {
        Path src = work.resolve("Demo.java");
        Files.write(src, (
            "package p;\npublic class Demo {\n" +
            "  public int m(int x) { return x + 1; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Demo.class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{"d", cls.toString()});
        } finally {
            System.setOut(old);
        }
        String out = baos.toString("UTF-8");
        if (!out.contains("Constant pool:"))
            throw new Exception("CLI disasm missing Constant pool");
        if (!out.contains("Code:"))
            throw new Exception("CLI disasm missing Code section");
        if (!out.contains("stack="))
            throw new Exception("CLI disasm missing stack/locals info");
        if (!out.contains("iload_1") && !out.contains("ireturn"))
            throw new Exception("CLI disasm missing bytecode instructions");
        System.out.println("  CLI disasm output looks OK (" + out.split("\n").length + " lines)");
    }

    static void testCLIRoundtrip(Path work) throws Exception {
        Path src = work.resolve("Demo2.java");
        Files.write(src, (
            "package p;\npublic class Demo2 {\n" +
            "  public int m(int x) { return x + 1; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Demo2.class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{"r", cls.toString()});
        } finally {
            System.setOut(old);
        }
        String out = baos.toString("UTF-8");
        if (!out.contains("Output:") || !out.contains("roundtrip"))
            throw new Exception("CLI roundtrip not OK, got: " + out);
        Path rt = work.resolve("p").resolve("Demo2.roundtrip.class");
        Path rt2 = work.resolve("p").resolve("Demo2.roundtrip.1.class");
        if (!Files.exists(rt) && !Files.exists(rt2))
            throw new Exception("CLI roundtrip output file not found at " + rt + " or .1.class variant");
        System.out.println("  CLI roundtrip OK, output at " + (Files.exists(rt) ? rt : rt2));
    }

    static int run(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), "UTF-8");
        int rc = p.waitFor();
        if (rc != 0 && out.length() > 0) {
            for (String line : out.split("\n")) {
                String t = line.trim();
                if (t.contains("Error") || t.contains("error"))
                    System.out.println("    | " + t);
            }
        }
        return rc;
    }

    static void testExactMethodSelect(Path work) throws Exception {
        Path src = work.resolve("Overload.java");
        Files.write(src, (
            "package p;\npublic class Overload {\n" +
            "  public int calc(int x) { return x + 1; }\n" +
            "  public int calc(int x, int y) { return x + y; }\n" +
            "  public String calc(String s) { return s + \"!\"; }\n" +
            "  public int unused() { return 99; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Overload.class");

        Path outDir = work.resolve("exact");
        Files.createDirectories(outDir);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{
                "i", cls.toString(), "calc(II)I", "[CALC_II]", "-d", outDir.toString()
            });
        } finally {
            System.setOut(old);
        }
        String out = baos.toString("UTF-8");
        System.out.println("  instrument II report: " + out.replace("\n", " ").replace("\r", " ").substring(0, Math.min(200, out.length())) + "...");
        if (!out.contains("calc(II)I"))
            throw new Exception("calc(II)I not reported as instrumented");
        if (out.contains("calc(I)I") || out.contains("calc(Ljava/lang/String;)Ljava/lang/String;"))
            throw new Exception("other overloads should NOT be instrumented");

        Path instFile = outDir.resolve("Overload.instrumented.class");
        Path targetDir = outDir.resolve("p");
        Files.createDirectories(targetDir);
        Path moved = targetDir.resolve("Overload.class");
        Files.copy(instFile, moved, StandardCopyOption.REPLACE_EXISTING);

        Object r1 = loadAndInvoke(outDir, "p.Overload", "calc", new Class[]{int.class}, new Object[]{7});
        Object r2 = loadAndInvoke(outDir, "p.Overload", "calc", new Class[]{int.class, int.class}, new Object[]{3, 4});
        Object r3 = loadAndInvoke(outDir, "p.Overload", "calc", new Class[]{String.class}, new Object[]{"hi"});
        Object r4 = loadAndInvoke(outDir, "p.Overload", "unused", new Class[0], new Object[0]);

        System.out.println("  calc(7)=" + r1 + " (int only, no-print overload)");
        System.out.println("  calc(3,4)=" + r2 + " (int,int, printed overload)");
        System.out.println("  calc('hi')=" + r3 + " (String, no-print overload)");
        System.out.println("  unused()=" + r4 + " (not matched at all)");

        if (!Integer.valueOf(8).equals(r1)) throw new Exception("calc(int) wrong: " + r1);
        if (!Integer.valueOf(7).equals(r2)) throw new Exception("calc(int,int) wrong: " + r2);
        if (!"hi!".equals(r3)) throw new Exception("calc(String) wrong: " + r3);
        if (!Integer.valueOf(99).equals(r4)) throw new Exception("unused() wrong: " + r4);
        System.out.println("  exact method select: only calc(II)I instrumented; returns all correct");
    }

    static void testControlFlowSamples(Path work) throws Exception {
        Path src = work.resolve("Flow.java");
        Files.write(src, (
            "package p;\npublic class Flow {\n" +
            "  public int branch(int x) {\n" +
            "    if (x > 10) return x - 10;\n" +
            "    else if (x > 5) return x * 2;\n" +
            "    else return -x;\n" +
            "  }\n" +
            "  public int guarded(String s) {\n" +
            "    try { int n = s.length(); return n + s.charAt(0); }\n" +
            "    catch (NullPointerException e) { return -1; }\n" +
            "    catch (Exception e) { return -2; }\n" +
            "    finally { System.out.println(\"guarded-end\"); }\n" +
            "  }\n" +
            "  public String strSwitch(String k) {\n" +
            "    switch(k) {\n" +
            "      case \"red\": return \"R\"; case \"green\": return \"G\"; case \"blue\": return \"B\";\n" +
            "      default: return \"?\";\n" +
            "    }\n" +
            "  }\n" +
            "  public int sparseSwitch(int x) {\n" +
            "    switch(x) {\n" +
            "      case 1: return 10; case 100: return 20; case 10000: return 30; case -500: return 40;\n" +
            "      default: return 0;\n" +
            "    }\n" +
            "  }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path cls = work.resolve("p").resolve("Flow.class");
        byte[] data = Files.readAllBytes(cls);
        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        String fullDisasm = bytecodetool.util.Disassembler.disassemble(cf, pool);
        String[] lines = fullDisasm.split("\n");
        System.out.println("  disassembled " + lines.length + " lines total");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{"d", cls.toString()});
        } finally {
            System.setOut(old);
        }
        String cliOut = baos.toString("UTF-8");
        if (!cliOut.contains("Exception table"))
            throw new Exception("CLI disasm missing Exception table (should appear in guarded())");
        if (!cliOut.contains("tableswitch") && !cliOut.contains("lookupswitch"))
            throw new Exception("CLI disasm missing switch instructions");

        boolean hasLookup = false;
        for (MethodInfo m : cf.methods) {
            String n = pool.getUtf8(m.nameIndex);
            CodeAttribute ca = m.getCodeAttribute();
            if (ca == null) continue;
            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction in : insns) {
                if (in.opcode == Opcodes.LOOKUPSWITCH) hasLookup = true;
                if (in.opcode == Opcodes.LOOKUPSWITCH || in.opcode == Opcodes.TABLESWITCH) {
                    String fmt = bytecodetool.util.Disassembler.formatInstruction(in, pool, ca.code);
                    if (!fmt.contains("default:"))
                        throw new Exception(n + " switch missing default: in format -> " + fmt);
                }
                if (in.opcode >= Opcodes.IFEQ && in.opcode <= Opcodes.IF_ACMPNE) {
                    String fmt = bytecodetool.util.Disassembler.formatInstruction(in, pool, ca.code);
                    Object op0 = in.operands[0];
                    int off = op0 instanceof Short ? (Short) op0 : ((Number) op0).intValue();
                    int target = in.offset + off;
                    if (!fmt.contains(String.valueOf(target)))
                        throw new Exception(n + " jump target " + target + " missing in: " + fmt);
                }
            }
        }
        if (!hasLookup)
            throw new Exception("sparse switch should compile to lookupswitch but none found");
        System.out.println("  control flow disasm OK (if/ExceptionTable/lookupswitch+tableswitch/jump targets all visible)");
    }

    static void testDiffMode(Path work) throws Exception {
        Path src = work.resolve("DiffBase.java");
        Files.write(src, (
            "package p;\npublic class DiffBase {\n" +
            "  public int foo(int x) { return x + 1; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());

        Path orig = work.resolve("p").resolve("DiffBase.class");
        Path outDir = work.resolve("diffwork");
        Files.createDirectories(outDir);

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos1, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{
                "i", orig.toString(), "foo", "[FOO]", "-d", outDir.toString()
            });
        } finally {
            System.setOut(old);
        }

        Path inst = outDir.resolve("DiffBase.instrumented.class");
        if (!Files.exists(inst)) throw new Exception("instrumented file not found");

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(baos2, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{"diff", orig.toString(), inst.toString()});
        } finally {
            System.setOut(old);
        }
        String out = baos2.toString("UTF-8");
        System.out.println("  diff output " + out.split("\n").length + " lines");
        if (!out.contains("Constant pool"))
            throw new Exception("diff missing constant pool section");
        if (!out.contains("Methods"))
            throw new Exception("diff missing methods section");
        if (!out.contains("getstatic") || !out.contains("println"))
            throw new Exception("diff not showing inserted bytecode (getstatic/println missing)");
        if (!out.contains("added entries"))
            throw new Exception("diff should report added constant pool entries");
        System.out.println("  diff mode OK (shows cp additions, method bytecode changes)");
    }

    static void testOutputMgmt(Path work) throws Exception {
        Path src = work.resolve("Mgmt.java");
        Files.write(src, (
            "package p;\npublic class Mgmt {\n" +
            "  public int m(int x) { return x * 2; }\n" +
            "}\n").getBytes("UTF-8"));
        run(JAVAC, "-d", work.toString(), src.toString());
        Path cls = work.resolve("p").resolve("Mgmt.class");

        Path customDir = work.resolve("custom");
        Path customFile = customDir.resolve("MyRenamed.class");

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        PrintStream old = System.out;
        try {
            System.setOut(new PrintStream(baos1, true, "UTF-8"));
            bytecodetool.BytecodeTool.main(new String[]{
                "r", cls.toString(), "-o", customFile.toString()
            });
        } finally {
            System.setOut(old);
        }
        String r1 = baos1.toString("UTF-8");
        if (!Files.exists(customFile))
            throw new Exception("custom output file not created");
        if (!r1.contains("JVM load") || !r1.contains("OK"))
            throw new Exception("custom file missing JVM load report, got: " + r1);
        System.out.println("  custom -o output file created + JVM loaded OK");

        Path noOverwriteDir = work.resolve("noover");
        Files.createDirectories(noOverwriteDir);
        for (int k = 0; k < 3; k++) {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            try {
                System.setOut(new PrintStream(ba, true, "UTF-8"));
                bytecodetool.BytecodeTool.main(new String[]{
                    "r", cls.toString(), "-d", noOverwriteDir.toString()
                });
            } finally {
                System.setOut(old);
            }
        }
        List<Path> produced = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(noOverwriteDir, "*.class")) {
            for (Path p : ds) produced.add(p);
        }
        if (produced.size() < 3)
            throw new Exception("expected at least 3 non-overwritten outputs, got " + produced.size());
        boolean hasNumbered = false;
        for (Path p : produced) {
            String fn = p.getFileName().toString();
            if (fn.contains(".1.") || fn.contains(".2.")) hasNumbered = true;
            byte[] d = Files.readAllBytes(p);
            ClassReader cr = new ClassReader(d);
            if (cr.read() == null) throw new Exception("output file invalid: " + fn);
        }
        if (!hasNumbered)
            throw new Exception("expected .1. .2. suffixed files to prevent overwrite, got: " + produced);
        System.out.println("  non-overwrite naming OK (got " + produced.size() + " outputs, includes numbered suffixes)");
    }
}
