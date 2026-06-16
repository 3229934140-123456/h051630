package bytecodetool.transform;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.pool.ConstantPool;

import java.io.ByteArrayOutputStream;
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
            adjustExceptionTable(code);
            adjustLineNumberTable(code);
            adjustLocalVariableTable(code);
            code.maxStack += extraStack;
            code.code = buildNewCode(code.code);
        }

        protected void adjustExceptionTable(CodeAttribute code) {
            int prologueLen = prologue.length;
            if (prologueLen == 0) return;
            for (CodeAttribute.ExceptionTableEntry entry : code.exceptionTable) {
                entry.startPc += prologueLen;
                entry.endPc += prologueLen;
                entry.handlerPc += prologueLen;
            }
        }

        protected void adjustLineNumberTable(CodeAttribute code) {
            int prologueLen = prologue.length;
            if (prologueLen == 0) return;
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LineNumberTableAttribute) {
                    LineNumberTableAttribute ln = (LineNumberTableAttribute) attr;
                    for (LineNumberTableAttribute.LineNumberEntry entry : ln.lineNumberTable) {
                        entry.startPc += prologueLen;
                    }
                }
            }
        }

        protected void adjustLocalVariableTable(CodeAttribute code) {
            int prologueLen = prologue.length;
            if (prologueLen == 0) return;
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LocalVariableTableAttribute) {
                    LocalVariableTableAttribute lv = (LocalVariableTableAttribute) attr;
                    for (LocalVariableTableAttribute.LocalVariableEntry entry : lv.localVariableTable) {
                        entry.startPc += prologueLen;
                    }
                }
            }
        }

        protected byte[] buildNewCode(byte[] originalCode) {
            List<Byte> newCode = new ArrayList<>();
            for (byte b : prologue) newCode.add(b);

            int originalLength = originalCode.length;
            int prologueLen = prologue.length;

            int pc = 0;
            while (pc < originalLength) {
                int opcode = originalCode[pc] & 0xFF;
                int instStart = pc;

                if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR
                    || (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE)
                    || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
                    int offset = (short) ((originalCode[pc + 1] & 0xFF) << 8 | (originalCode[pc + 2] & 0xFF));
                    int target = pc + offset;
                    newCode.add(originalCode[pc]);
                    int newOffset = target + prologueLen - (pc + prologueLen);
                    newCode.add((byte) ((newOffset >> 8) & 0xFF));
                    newCode.add((byte) (newOffset & 0xFF));
                    pc += 3;
                    continue;
                }

                if (opcode == Opcodes.GOTO_W || opcode == Opcodes.JSR_W) {
                    int offset = (originalCode[pc + 1] & 0xFF) << 24 | (originalCode[pc + 2] & 0xFF) << 16
                               | (originalCode[pc + 3] & 0xFF) << 8 | (originalCode[pc + 4] & 0xFF);
                    int target = pc + offset;
                    newCode.add(originalCode[pc]);
                    int newOffset = target + prologueLen - (pc + prologueLen);
                    newCode.add((byte) ((newOffset >> 24) & 0xFF));
                    newCode.add((byte) ((newOffset >> 16) & 0xFF));
                    newCode.add((byte) ((newOffset >> 8) & 0xFF));
                    newCode.add((byte) (newOffset & 0xFF));
                    pc += 5;
                    continue;
                }

                if (opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH) {
                    int instructionLength = getSwitchInstructionLength(originalCode, pc);
                    for (int i = 0; i < instructionLength; i++) {
                        newCode.add(originalCode[pc + i]);
                    }
                    pc += instructionLength;
                    continue;
                }

                if (opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                    || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
                    for (byte b : epilogue) newCode.add(b);
                    newCode.add(originalCode[pc]);
                    pc += 1;
                    continue;
                }

                int instLen = getSimpleInstructionLength(originalCode, pc);
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

        protected int getSimpleInstructionLength(byte[] code, int pc) {
            int opcode = code[pc] & 0xFF;
            if (opcode == Opcodes.WIDE) {
                int wideOp = code[pc + 1] & 0xFF;
                return wideOp == Opcodes.IINC ? 6 : 4;
            }
            if (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.MULTIANEWARRAY
                || opcode == Opcodes.INVOKEDYNAMIC) {
                return 5;
            }
            if (opcode == Opcodes.IINC) return 3;
            if (opcode == Opcodes.SIPUSH || opcode == Opcodes.LDC_W || opcode == Opcodes.LDC2_W
                || (opcode >= Opcodes.GETSTATIC && opcode <= Opcodes.INVOKESTATIC)
                || opcode == Opcodes.NEW || opcode == Opcodes.ANEWARRAY
                || opcode == Opcodes.CHECKCAST || opcode == Opcodes.INSTANCEOF
                || opcode == Opcodes.GOTO || opcode == Opcodes.JSR
                || (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE)
                || opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL
                || opcode == Opcodes.BIPUSH || opcode == Opcodes.LDC
                || (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD)
                || (opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE)
                || opcode == Opcodes.RET || opcode == Opcodes.NEWARRAY) {
                return opcode == Opcodes.BIPUSH || opcode == Opcodes.LDC
                    || (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD)
                    || (opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE)
                    || opcode == Opcodes.RET || opcode == Opcodes.NEWARRAY ? 2 : 3;
            }
            return 1;
        }

        protected int getSwitchInstructionLength(byte[] code, int pc) {
            int padStart = pc + 1;
            int aligned = padStart;
            while (aligned % 4 != 0) aligned++;
            int padding = aligned - padStart;
            aligned += 4;
            if ((code[pc] & 0xFF) == Opcodes.TABLESWITCH) {
                int low = (code[aligned] & 0xFF) << 24 | (code[aligned + 1] & 0xFF) << 16
                        | (code[aligned + 2] & 0xFF) << 8 | (code[aligned + 3] & 0xFF);
                aligned += 4;
                int high = (code[aligned] & 0xFF) << 24 | (code[aligned + 1] & 0xFF) << 16
                         | (code[aligned + 2] & 0xFF) << 8 | (code[aligned + 3] & 0xFF);
                aligned += 4;
                int count = high - low + 1;
                return 1 + padding + 4 + 4 + 4 + count * 4;
            } else {
                int npairs = (code[aligned] & 0xFF) << 24 | (code[aligned + 1] & 0xFF) << 16
                           | (code[aligned + 2] & 0xFF) << 8 | (code[aligned + 3] & 0xFF);
                aligned += 4;
                return 1 + padding + 4 + 4 + npairs * 8;
            }
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
            int systemIdx = pool.addClass("java/lang/System");
            int outNatIdx = pool.addNameAndType("out", "Ljava/io/PrintStream;");
            int outFieldIdx = pool.addFieldref("java/lang/System", "out", "Ljava/io/PrintStream;");
            int psIdx = pool.addClass("java/io/PrintStream");
            int printlnNatIdx = pool.addNameAndType("println", "(Ljava/lang/String;)V");
            int printlnMethodIdx = pool.addMethodref("java/io/PrintStream", "println", "(Ljava/lang/String;)V");
            int msgIdx = pool.addString(message);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(Opcodes.GETSTATIC);
                baos.write((outFieldIdx >> 8) & 0xFF);
                baos.write(outFieldIdx & 0xFF);
                baos.write(Opcodes.LDC);
                baos.write(msgIdx & 0xFF);
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(Opcodes.INVOKESTATIC);
                int nanoTimeIdx = pool.addMethodref("java/lang/System", "nanoTime", "()J");
                baos.write((nanoTimeIdx >> 8) & 0xFF);
                baos.write(nanoTimeIdx & 0xFF);
                baos.write(Opcodes.GETSTATIC);
                baos.write((fieldIdx >> 8) & 0xFF);
                baos.write(fieldIdx & 0xFF);
                baos.write(Opcodes.LSUB);
                baos.write(Opcodes.PUTSTATIC);
                baos.write((fieldIdx >> 8) & 0xFF);
                baos.write(fieldIdx & 0xFF);
                this.prologue = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void buildEpilogue(ConstantPool pool, String ownerClass) {
            int fieldIdx = pool.addFieldref(ownerClass, fieldName, "J");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(Opcodes.INVOKESTATIC);
                int nanoTimeIdx = pool.addMethodref("java/lang/System", "nanoTime", "()J");
                baos.write((nanoTimeIdx >> 8) & 0xFF);
                baos.write(nanoTimeIdx & 0xFF);
                baos.write(Opcodes.GETSTATIC);
                baos.write((fieldIdx >> 8) & 0xFF);
                baos.write(fieldIdx & 0xFF);
                baos.write(Opcodes.LADD);
                baos.write(Opcodes.PUTSTATIC);
                baos.write((fieldIdx >> 8) & 0xFF);
                baos.write(fieldIdx & 0xFF);
                this.epilogue = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int recomputeMaxStack(byte[] code) {
        List<Instruction> instructions = BytecodeParser.parse(code);
        return BytecodeParser.computeMaxStack(instructions);
    }
}
