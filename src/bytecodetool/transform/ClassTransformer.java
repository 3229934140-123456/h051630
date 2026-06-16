package bytecodetool.transform;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.pool.ConstantPool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassTransformer {
    private final ClassFile classFile;
    private final ConstantPool constantPool;

    public ClassTransformer(ClassFile classFile, ConstantPool constantPool) {
        this.classFile = classFile;
        this.constantPool = constantPool;
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public ConstantPool getConstantPool() {
        return constantPool;
    }

    public void addMethod(MethodInfo method) {
        MethodInfo[] newMethods = new MethodInfo[classFile.methods.length + 1];
        System.arraycopy(classFile.methods, 0, newMethods, 0, classFile.methods.length);
        newMethods[classFile.methods.length] = method;
        classFile.methods = newMethods;
    }

    public void addField(FieldInfo field) {
        FieldInfo[] newFields = new FieldInfo[classFile.fields.length + 1];
        System.arraycopy(classFile.fields, 0, newFields, 0, classFile.fields.length);
        newFields[classFile.fields.length] = field;
        classFile.fields = newFields;
    }

    public void instrumentAllMethods(MethodInstrumenter instrumenter) {
        for (MethodInfo method : classFile.methods) {
            CodeAttribute code = method.getCodeAttribute();
            if (code != null) {
                instrumenter.instrument(method, code, constantPool);
            }
        }
    }

    public void instrumentMethod(String methodName, String descriptor, MethodInstrumenter instrumenter) {
        for (MethodInfo method : classFile.methods) {
            String name = constantPool.getUtf8(method.nameIndex);
            String desc = constantPool.getUtf8(method.descriptorIndex);
            if (methodName.equals(name) && (descriptor == null || descriptor.equals(desc))) {
                CodeAttribute code = method.getCodeAttribute();
                if (code != null) {
                    instrumenter.instrument(method, code, constantPool);
                }
            }
        }
    }

    public static class MethodInstrumenter {
        protected byte[] prologue;
        protected byte[] epilogue;
        protected int extraStack;

        public MethodInstrumenter() {
            this.prologue = new byte[0];
            this.epilogue = new byte[0];
            this.extraStack = 0;
        }

        public void instrument(MethodInfo method, CodeAttribute code, ConstantPool pool) {
            adjustExceptionTable(code, prologue.length);
            adjustLineNumberTable(code, prologue.length);
            adjustLocalVariableTable(code, prologue.length);
            adjustStackMapTable(code, prologue.length);
            code.maxStack += extraStack;
            code.code = buildNewCode(code.code);
        }

        protected void adjustExceptionTable(CodeAttribute code, int delta) {
            if (delta == 0) return;
            for (CodeAttribute.ExceptionTableEntry entry : code.exceptionTable) {
                entry.startPc += delta;
                entry.endPc += delta;
                entry.handlerPc += delta;
            }
        }

        protected void adjustLineNumberTable(CodeAttribute code, int delta) {
            if (delta == 0) return;
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LineNumberTableAttribute) {
                    LineNumberTableAttribute ln = (LineNumberTableAttribute) attr;
                    for (LineNumberTableAttribute.LineNumberEntry entry : ln.lineNumberTable) {
                        entry.startPc += delta;
                    }
                }
            }
        }

        protected void adjustLocalVariableTable(CodeAttribute code, int delta) {
            if (delta == 0) return;
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LocalVariableTableAttribute) {
                    LocalVariableTableAttribute lv = (LocalVariableTableAttribute) attr;
                    for (LocalVariableTableAttribute.LocalVariableEntry entry : lv.localVariableTable) {
                        entry.startPc += delta;
                    }
                }
            }
        }

        protected void adjustStackMapTable(CodeAttribute code, int delta) {
            if (delta == 0) return;
            for (int i = 0; i < code.attributes.length; i++) {
                AttributeInfo attr = code.attributes[i];
                if (attr instanceof StackMapTableAttribute) {
                    code.attributes[i] = rebuildStackMapTable((StackMapTableAttribute) attr, delta);
                }
            }
        }

        private StackMapTableAttribute rebuildStackMapTable(StackMapTableAttribute smt, int delta) {
            StackMapTableAttribute result = new StackMapTableAttribute(smt.attributeNameIndex, smt.attributeName);
            result.entries = new StackMapTableAttribute.StackMapFrame[smt.entries.length];
            int runningOffset = 0;
            for (int i = 0; i < smt.entries.length; i++) {
                StackMapTableAttribute.StackMapFrame oldFrame = smt.entries[i];
                int oldAbsolutePc = runningOffset + getFrameOffsetDelta(oldFrame);
                int newAbsolutePc = oldAbsolutePc + delta;
                int newOffsetDelta = newAbsolutePc - runningOffset;
                result.entries[i] = recreateFrame(oldFrame, newOffsetDelta);
                runningOffset = newAbsolutePc;
            }
            return result;
        }

        private int getFrameOffsetDelta(StackMapTableAttribute.StackMapFrame frame) {
            int ft = frame.frameType;
            if (ft >= 0 && ft <= 63) return ft;
            if (ft >= 64 && ft <= 127) return ft - 64;
            return frame.offsetDelta;
        }

        private StackMapTableAttribute.StackMapFrame recreateFrame(StackMapTableAttribute.StackMapFrame old, int newOffsetDelta) {
            StackMapTableAttribute.StackMapFrame f = new StackMapTableAttribute.StackMapFrame(old.frameType);
            f.locals = old.locals;
            f.stack = old.stack;
            int ft = old.frameType;
            if (ft >= 0 && ft <= 63) {
                if (newOffsetDelta >= 0 && newOffsetDelta <= 63) {
                    f.frameType = newOffsetDelta;
                } else {
                    f.frameType = 251;
                    f.offsetDelta = newOffsetDelta;
                }
            } else if (ft >= 64 && ft <= 127) {
                if (newOffsetDelta >= 0 && newOffsetDelta <= 63) {
                    f.frameType = 64 + newOffsetDelta;
                } else {
                    f.frameType = 247;
                    f.offsetDelta = newOffsetDelta;
                }
            } else {
                f.frameType = ft;
                f.offsetDelta = newOffsetDelta;
            }
            return f;
        }

        protected byte[] buildNewCode(byte[] originalCode) {
            List<Byte> newCode = new ArrayList<>();
            for (byte b : prologue) newCode.add(b);

            int originalLength = originalCode.length;
            int prologueLen = prologue.length;
            int epilogueLen = epilogue.length;

            int pc = 0;
            while (pc < originalLength) {
                int opcode = originalCode[pc] & 0xFF;
                int instLen = BytecodeParser.getInstructionLength(originalCode, pc);

                if (opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH) {
                    rewriteSwitch(newCode, originalCode, pc, prologueLen);
                    pc += instLen;
                    continue;
                }

                boolean isShortBranch = (opcode == Opcodes.GOTO || opcode == Opcodes.JSR
                    || (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE)
                    || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL);

                boolean isLongBranch = (opcode == Opcodes.GOTO_W || opcode == Opcodes.JSR_W);

                if (isShortBranch) {
                    int offset = (short) ((originalCode[pc + 1] & 0xFF) << 8 | (originalCode[pc + 2] & 0xFF));
                    int target = pc + offset;
                    int newTarget = target + prologueLen;
                    int newPc = pc + prologueLen;
                    int newOffset = newTarget - newPc;
                    newCode.add(originalCode[pc]);
                    newCode.add((byte) ((newOffset >> 8) & 0xFF));
                    newCode.add((byte) (newOffset & 0xFF));
                    pc += instLen;
                    continue;
                }

                if (isLongBranch) {
                    int offset = (originalCode[pc + 1] & 0xFF) << 24 | (originalCode[pc + 2] & 0xFF) << 16
                               | (originalCode[pc + 3] & 0xFF) << 8 | (originalCode[pc + 4] & 0xFF);
                    int target = pc + offset;
                    int newTarget = target + prologueLen;
                    int newPc = pc + prologueLen;
                    int newOffset = newTarget - newPc;
                    newCode.add(originalCode[pc]);
                    newCode.add((byte) ((newOffset >> 24) & 0xFF));
                    newCode.add((byte) ((newOffset >> 16) & 0xFF));
                    newCode.add((byte) ((newOffset >> 8) & 0xFF));
                    newCode.add((byte) (newOffset & 0xFF));
                    pc += instLen;
                    continue;
                }

                if (Opcodes.isReturn(opcode)) {
                    for (byte b : epilogue) newCode.add(b);
                    newCode.add(originalCode[pc]);
                    pc += instLen;
                    continue;
                }

                for (int i = 0; i < instLen; i++) {
                    newCode.add(originalCode[pc + i]);
                }
                pc += instLen;
            }

            byte[] result = new byte[newCode.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = newCode.get(i);
            }
            return result;
        }

        private void rewriteSwitch(List<Byte> newCode, byte[] originalCode, int switchPc, int delta) {
            int opcode = originalCode[switchPc] & 0xFF;
            newCode.add(originalCode[switchPc]);

            int padStart = switchPc + 1;
            int aligned = padStart;
            while (aligned % 4 != 0) aligned++;
            int padding = aligned - padStart;
            for (int i = 0; i < padding; i++) newCode.add((byte) 0);

            int p = aligned;
            int defOffset = (originalCode[p] & 0xFF) << 24 | (originalCode[p + 1] & 0xFF) << 16
                          | (originalCode[p + 2] & 0xFF) << 8 | (originalCode[p + 3] & 0xFF);
            int newDef = defOffset + delta;
            writeInt(newCode, newDef);
            p += 4;

            if (opcode == Opcodes.TABLESWITCH) {
                int low = readInt(originalCode, p); p += 4;
                int high = readInt(originalCode, p); p += 4;
                writeInt(newCode, low);
                writeInt(newCode, high);
                int count = high - low + 1;
                for (int i = 0; i < count; i++) {
                    int off = readInt(originalCode, p); p += 4;
                    writeInt(newCode, off + delta);
                }
            } else {
                int npairs = readInt(originalCode, p); p += 4;
                writeInt(newCode, npairs);
                for (int i = 0; i < npairs; i++) {
                    int key = readInt(originalCode, p); p += 4;
                    int off = readInt(originalCode, p); p += 4;
                    writeInt(newCode, key);
                    writeInt(newCode, off + delta);
                }
            }
        }

        private static int readInt(byte[] code, int pos) {
            return (code[pos] & 0xFF) << 24 | (code[pos + 1] & 0xFF) << 16
                 | (code[pos + 2] & 0xFF) << 8 | (code[pos + 3] & 0xFF);
        }

        private static void writeInt(List<Byte> list, int v) {
            list.add((byte) ((v >> 24) & 0xFF));
            list.add((byte) ((v >> 16) & 0xFF));
            list.add((byte) ((v >> 8) & 0xFF));
            list.add((byte) (v & 0xFF));
        }
    }

    public static class PrintInstrumenter extends MethodInstrumenter {
        private final String message;

        public PrintInstrumenter(String message, ConstantPool pool) {
            super();
            this.message = message;
            buildPrologue(pool);
            this.extraStack = 2;
        }

        private void buildPrologue(ConstantPool pool) {
            int outFieldIdx = pool.addFieldref("java/lang/System", "out", "Ljava/io/PrintStream;");
            int printlnMethodIdx = pool.addMethodref("java/io/PrintStream", "println", "(Ljava/lang/String;)V");
            int msgIdx = pool.addString(message);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(Opcodes.GETSTATIC);
                baos.write((outFieldIdx >> 8) & 0xFF);
                baos.write(outFieldIdx & 0xFF);

                if (msgIdx <= 0xFF) {
                    baos.write(Opcodes.LDC);
                    baos.write(msgIdx & 0xFF);
                } else {
                    baos.write(Opcodes.LDC_W);
                    baos.write((msgIdx >> 8) & 0xFF);
                    baos.write(msgIdx & 0xFF);
                }

                baos.write(Opcodes.INVOKEVIRTUAL);
                baos.write((printlnMethodIdx >> 8) & 0xFF);
                baos.write(printlnMethodIdx & 0xFF);
                this.prologue = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class TimingInstrumenter extends MethodInstrumenter {
        private final String fieldName;

        public TimingInstrumenter(ConstantPool pool, String ownerClass, String timingFieldName) {
            super();
            this.fieldName = timingFieldName;
            buildPrologue(pool, ownerClass);
            buildEpilogue(pool, ownerClass);
            this.extraStack = 4;
        }

        private void buildPrologue(ConstantPool pool, String ownerClass) {
            int fieldIdx = pool.addFieldref(ownerClass, fieldName, "J");
            int nanoTimeIdx = pool.addMethodref("java/lang/System", "nanoTime", "()J");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                writeGetStatic(baos, fieldIdx);
                baos.write(Opcodes.INVOKESTATIC);
                baos.write((nanoTimeIdx >> 8) & 0xFF);
                baos.write(nanoTimeIdx & 0xFF);
                baos.write(Opcodes.LSUB);
                writePutStatic(baos, fieldIdx);
                this.prologue = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void buildEpilogue(ConstantPool pool, String ownerClass) {
            int fieldIdx = pool.addFieldref(ownerClass, fieldName, "J");
            int nanoTimeIdx = pool.addMethodref("java/lang/System", "nanoTime", "()J");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(Opcodes.INVOKESTATIC);
                baos.write((nanoTimeIdx >> 8) & 0xFF);
                baos.write(nanoTimeIdx & 0xFF);
                writeGetStatic(baos, fieldIdx);
                baos.write(Opcodes.LADD);
                writePutStatic(baos, fieldIdx);
                this.epilogue = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void writeGetStatic(ByteArrayOutputStream baos, int idx) {
            baos.write(Opcodes.GETSTATIC);
            baos.write((idx >> 8) & 0xFF);
            baos.write(idx & 0xFF);
        }

        private void writePutStatic(ByteArrayOutputStream baos, int idx) {
            baos.write(Opcodes.PUTSTATIC);
            baos.write((idx >> 8) & 0xFF);
            baos.write(idx & 0xFF);
        }
    }

    public static int recomputeMaxStack(byte[] code) {
        List<Instruction> instructions = BytecodeParser.parse(code);
        return BytecodeParser.computeMaxStack(instructions);
    }
}
