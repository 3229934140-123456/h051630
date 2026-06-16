package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;

import java.nio.file.*;

public class DebugRawSMT {
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
            for (AttributeInfo a : ca.attributes) {
                if (a instanceof StackMapTableAttribute) {
                    StackMapTableAttribute smt = (StackMapTableAttribute) a;
                    System.out.println("intSwitch StackMapTable raw:");
                    System.out.println("  number_of_entries = " + smt.entries.length);
                    for (int i = 0; i < smt.entries.length; i++) {
                        StackMapTableAttribute.StackMapFrame f = smt.entries[i];
                        System.out.printf("  frame[%d]: frameType=%d offsetDelta=%d locals=%d stack=%d%n",
                            i, f.frameType, f.offsetDelta,
                            f.locals != null ? f.locals.length : -1,
                            f.stack != null ? f.stack.length : -1);
                    }
                }
            }
        }
    }
}
