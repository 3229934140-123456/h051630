package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;
import bytecodetool.writer.ClassWriter;

import java.nio.file.*;
import java.util.*;

public class DebugSMT {
    public static void main(String[] args) throws Exception {
        String javac = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\javac.exe";
        Path work = Paths.get("target/debug");
        Files.createDirectories(work);

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
        new ProcessBuilder(javac, "-d", work.toString(), src.toString()).start().waitFor();

        Path cls = work.resolve("p").resolve("ICF.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (!"process".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            System.out.println("=== Original method process ===");
            System.out.println("code length: " + ca.code.length);
            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction i : insns) {
                System.out.printf("  pc=%3d %s%n", i.offset, Opcodes.getOpcodeName(i.opcode));
            }
            for (AttributeInfo a : ca.attributes) {
                if (a instanceof StackMapTableAttribute) {
                    StackMapTableAttribute smt = (StackMapTableAttribute) a;
                    System.out.println("\nStackMapTable entries: " + smt.entries.length);
                    int running = 0;
                    for (int i = 0; i < smt.entries.length; i++) {
                        StackMapTableAttribute.StackMapFrame f = smt.entries[i];
                        int delta;
                        if (f.frameType >= 0 && f.frameType <= 63) delta = f.frameType;
                        else if (f.frameType >= 64 && f.frameType <= 127) delta = f.frameType - 64;
                        else delta = f.offsetDelta;
                        running += delta;
                        System.out.printf("  frame[%d]: type=%d delta=%d abs_pc=%d locals=%d stack=%d%n",
                            i, f.frameType, delta, running,
                            f.locals != null ? f.locals.length : -1,
                            f.stack != null ? f.stack.length : -1);
                    }
                }
            }
        }
    }
}
