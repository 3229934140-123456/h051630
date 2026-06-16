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

import java.nio.file.*;
import java.util.*;

public class DebugSwitch {
    public static void main(String[] args) throws Exception {
        String javac = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\javac.exe";
        Path work = Paths.get("target/debugsw");
        Files.createDirectories(work);

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
        new ProcessBuilder(javac, "-d", work.toString(), src.toString()).inheritIO().start().waitFor();

        Path cls = work.resolve("p").resolve("Sw.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (!"intSwitch".equals(name) && !"strSwitch".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            System.out.println("\n=== Original " + name + " (codeLen=" + ca.code.length + ") ===");
            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction i : insns) {
                System.out.printf("  pc=%3d %s len=%d%n", i.offset, Opcodes.getOpcodeName(i.opcode), i.length);
            }
            for (AttributeInfo a : ca.attributes) {
                if (a instanceof StackMapTableAttribute) {
                    StackMapTableAttribute smt = (StackMapTableAttribute) a;
                    System.out.println("  StackMapTable entries: " + smt.entries.length);
                    int running = 0;
                    for (int i = 0; i < smt.entries.length; i++) {
                        StackMapTableAttribute.StackMapFrame f = smt.entries[i];
                        int delta = getDelta(f);
                        running += delta;
                        System.out.printf("    frame[%d]: type=%d delta=%d abs_pc=%d%n", i, f.frameType, delta, running);
                    }
                }
            }
        }

        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[SW]", pool);
        tx.instrumentMethod("intSwitch", null, instr);
        tx.instrumentMethod("strSwitch", null, instr);

        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();

        ClassFile cf2 = new ClassReader(out).read();
        ConstantPool pool2 = new ClassReader(out).readConstantPool();

        for (MethodInfo m : cf2.methods) {
            String name = pool2.getUtf8(m.nameIndex);
            if (!"intSwitch".equals(name) && !"strSwitch".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            System.out.println("\n=== Instrumented " + name + " (codeLen=" + ca.code.length + ") ===");
            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction i : insns) {
                System.out.printf("  pc=%3d %s len=%d%n", i.offset, Opcodes.getOpcodeName(i.opcode), i.length);
            }
            for (AttributeInfo a : ca.attributes) {
                if (a instanceof StackMapTableAttribute) {
                    StackMapTableAttribute smt = (StackMapTableAttribute) a;
                    System.out.println("  StackMapTable entries: " + smt.entries.length);
                    int running = 0;
                    for (int i = 0; i < smt.entries.length; i++) {
                        StackMapTableAttribute.StackMapFrame f = smt.entries[i];
                        int delta = getDelta(f);
                        running += delta;
                        System.out.printf("    frame[%d]: type=%d delta=%d abs_pc=%d%n", i, f.frameType, delta, running);
                    }
                }
            }
        }
    }

    static int getDelta(StackMapTableAttribute.StackMapFrame f) {
        int ft = f.frameType;
        if (ft >= 0 && ft <= 63) return ft;
        if (ft >= 64 && ft <= 127) return ft - 64;
        return f.offsetDelta;
    }
}
