package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;

import java.nio.file.*;

public class DebugBytes {
    public static void main(String[] args) throws Exception {
        Path cls = Paths.get("target/debugsw/p/Sw.class");
        byte[] data = Files.readAllBytes(cls);
        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (!"intSwitch".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            System.out.println("intSwitch code bytes:");
            for (int i = 0; i < ca.code.length; i++) {
                System.out.printf("%02x ", ca.code[i] & 0xFF);
                if ((i+1) % 16 == 0) System.out.println();
            }
            System.out.println();

            System.out.println("\nDetailed:");
            int pc = 0;
            while (pc < ca.code.length) {
                int opcode = ca.code[pc] & 0xFF;
                int len = BytecodeParser.getInstructionLength(ca.code, pc);
                System.out.printf("  pc=%d op=0x%02x(%s) len=%d raw:", pc, opcode, Opcodes.getOpcodeName(opcode), len);
                for (int i = 0; i < Math.min(len, 12); i++) {
                    System.out.printf(" %02x", ca.code[pc+i] & 0xFF);
                }
                if (len > 12) System.out.print(" ...");
                System.out.println();
                pc += len;
            }
        }
    }
}
