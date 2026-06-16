package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;

import java.nio.file.*;
import java.util.*;

public class DebugMulti {
    public static void main(String[] args) throws Exception {
        Path work = Paths.get("target/verify");
        Files.createDirectories(work);

        Path src = work.resolve("Multi.java");
        Files.write(src, (
            "package p;\n" +
            "public class Multi {\n" +
            "  public int[][] create() { return new int[2][3]; }\n" +
            "}\n").getBytes("UTF-8"));
        String javac = "C:\\Program Files\\JetBrains\\PyCharm 2026.1.2\\jbr\\bin\\javac.exe";
        ProcessBuilder pb = new ProcessBuilder(javac, "-d", work.toString(), src.toString());
        pb.redirectErrorStream(true);
        pb.start().waitFor();

        Path cls = work.resolve("p").resolve("Multi.class");
        byte[] data = Files.readAllBytes(cls);

        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            System.out.println("Method: " + name);
            CodeAttribute ca = m.getCodeAttribute();
            if (ca == null) continue;
            System.out.println("  code length: " + ca.code.length);
            System.out.println("  code bytes: " + bytesHex(ca.code));

            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction inst : insns) {
                System.out.printf("  pc=%3d  op=0x%02X  len=%d  %s",
                    inst.offset, inst.opcode, inst.length, Opcodes.getOpcodeName(inst.opcode));
                if (inst.operands != null && inst.operands.length > 0) {
                    System.out.print("  ops=[");
                    for (int i = 0; i < inst.operands.length; i++) {
                        if (i > 0) System.out.print(",");
                        Object o = inst.operands[i];
                        if (o instanceof int[]) {
                            System.out.print(Arrays.toString((int[])o));
                        } else {
                            System.out.print(o);
                        }
                    }
                    System.out.print("]");
                }
                System.out.println();
            }
        }
    }

    static String bytesHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        return sb.toString();
    }
}
