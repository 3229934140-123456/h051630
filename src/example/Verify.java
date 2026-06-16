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
import java.util.*;

public class Verify {

    static final String JAVAC = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\javac.exe";
    static final String JAVA  = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\java.exe";

    public static void main(String[] args) throws Exception {
        Path work = Paths.get("target/verify");
        Files.createDirectories(work);
        int pass = 0, fail = 0;

        System.out.println("====== 1. \u6700\u5c0fclass: \u8bfb\u53d6->\u5199\u56de->JVM\u52a0\u8f7d ======");
        try { testRoundtrip(work); System.out.println("  [PASS]\n"); pass++; }
        catch (Exception e) { System.out.println("  [FAIL] " + e.getMessage() + "\n"); fail++; }

        System.out.println("====== 2. multianewarray \u89e3\u6790+\u5199\u56de ======");
        try { testMultiArray(work); System.out.println("  [PASS]\n"); pass++; }
        catch (Exception e) { System.out.println("  [FAIL] " + e.getMessage() + "\n"); fail++; }

        System.out.println("====== 3. switch \u63d2\u6869\u540e\u8df3\u8f6c\u6b63\u786e ======");
        try { testSwitch(work); System.out.println("  [PASS]\n"); pass++; }
        catch (Exception e) { System.out.println("  [FAIL] " + e.getMessage() + "\n"); fail++; }

        System.out.println("====== 4. if/catch/finally \u63d2\u6869\u540e JVM \u52a0\u8f7d\u9a8c\u8bc1 ======");
        try { testIfCatchFinally(work); System.out.println("  [PASS]\n"); pass++; }
        catch (Exception e) { System.out.println("  [FAIL] " + e.getMessage() + "\n"); fail++; }

        System.out.println("====== \u7ed3\u679c: PASS=" + pass + " FAIL=" + fail + " ======");
    }

    static Object loadAndInvoke(Path classDir, String className, String methodName, Class<?>[] paramTypes, Object[] args) throws Exception {
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

            if (multi.length != 4) throw new Exception("multianewarray should be 4 bytes, got " + multi.length);
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
            "  public int process(int x) {\n" +
            "    int r = 0;\n" +
            "    try {\n" +
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

        Object pos = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class}, new Object[]{5});
        Object neg = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class}, new Object[]{-3});
        Object zero = loadAndInvoke(outDir, "p.ICF", "process", new Class[]{int.class}, new Object[]{0});
        System.out.println("  process(5)=" + pos + " (-3)=" + neg + " (0)=" + zero);
        if (!Integer.valueOf(11).equals(pos)) throw new Exception("if-branch: expected 11, got " + pos);
        if (!Integer.valueOf(4).equals(neg)) throw new Exception("else-branch: expected 4, got " + neg);
        if (!Integer.valueOf(1).equals(zero)) throw new Exception("zero-branch: expected 1, got " + zero);
        System.out.println("  All branches (if/else/catch/finally) verified correct");
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
}
