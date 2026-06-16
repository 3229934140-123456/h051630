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
        public byte[] prologue;
        public byte[] epilogue;
        public int extraStack;

        public MethodInstrumenter() {
            this.prologue = new byte[0];
            this.epilogue = new byte[0];
            this.extraStack = 0;
        }

        public void instrument(MethodInfo method, CodeAttribute code, ConstantPool pool) {
            BuildResult br = buildNewCode(code.code);
            int[] oldToNew = br.oldToNew;
            adjustExceptionTable(code, oldToNew);
            adjustLineNumberTable(code, oldToNew);
            adjustLocalVariableTable(code, oldToNew);
            adjustStackMapTable(code, oldToNew);
            code.maxStack += extraStack;
            code.code = br.newCode;
        }

        protected void adjustExceptionTable(CodeAttribute code, int[] oldToNew) {
            for (CodeAttribute.ExceptionTableEntry entry : code.exceptionTable) {
                entry.startPc = mapPc(oldToNew, entry.startPc);
                entry.endPc = mapPc(oldToNew, entry.endPc);
                entry.handlerPc = mapPc(oldToNew, entry.handlerPc);
            }
        }

        protected void adjustLineNumberTable(CodeAttribute code, int[] oldToNew) {
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LineNumberTableAttribute) {
                    LineNumberTableAttribute ln = (LineNumberTableAttribute) attr;
                    for (LineNumberTableAttribute.LineNumberEntry entry : ln.lineNumberTable) {
                        entry.startPc = mapPc(oldToNew, entry.startPc);
                    }
                }
            }
        }

        protected void adjustLocalVariableTable(CodeAttribute code, int[] oldToNew) {
            for (AttributeInfo attr : code.attributes) {
                if (attr instanceof LocalVariableTableAttribute) {
                    LocalVariableTableAttribute lv = (LocalVariableTableAttribute) attr;
                    for (LocalVariableTableAttribute.LocalVariableEntry entry : lv.localVariableTable) {
                        entry.startPc = mapPc(oldToNew, entry.startPc);
                    }
                }
            }
        }

        protected void adjustStackMapTable(CodeAttribute code, int[] oldToNew) {
            for (int i = 0; i < code.attributes.length; i++) {
                AttributeInfo attr = code.attributes[i];
                if (attr instanceof StackMapTableAttribute) {
                    code.attributes[i] = rebuildStackMapTable((StackMapTableAttribute) attr, oldToNew);
                }
            }
        }

        private static int mapPc(int[] oldToNew, int oldPc) {
            if (oldPc < 0 || oldPc >= oldToNew.length) return oldPc;
            int mapped = oldToNew[oldPc];
            return mapped >= 0 ? mapped : oldPc;
        }

        private StackMapTableAttribute rebuildStackMapTable(StackMapTableAttribute smt, int[] oldToNew) {
            StackMapTableAttribute result = new StackMapTableAttribute(smt.attributeNameIndex, smt.attributeName);
            result.entries = new StackMapTableAttribute.StackMapFrame[smt.entries.length];
            int origOffset = 0;
            int newOffset = 0;
            for (int i = 0; i < smt.entries.length; i++) {
                StackMapTableAttribute.StackMapFrame oldFrame = smt.entries[i];
                int oldDelta = getFrameOffsetDelta(oldFrame);
                origOffset += oldDelta;
                int newAbsPc = mapPc(oldToNew, origOffset);
                int newDelta = newAbsPc - newOffset;
                result.entries[i] = recreateFrame(oldFrame, newDelta);
                newOffset = newAbsPc;
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

        private static class BuildResult {
            final byte[] newCode;
            final int[] oldToNew;
            BuildResult(byte[] newCode, int[] oldToNew) {
                this.newCode = newCode;
                this.oldToNew = oldToNew;
            }
        }

        protected BuildResult buildNewCode(byte[] originalCode) {
            List<Instruction> insns = BytecodeParser.parse(originalCode);
            int n = insns.size();
            int[] newPc = new int[n];
            int curNew = prologue.length;
            for (int i = 0; i < n; i++) {
                newPc[i] = curNew;
                Instruction inst = insns.get(i);
                if (inst.opcode == Opcodes.TABLESWITCH || inst.opcode == Opcodes.LOOKUPSWITCH) {
                    int padStart = curNew + 1;
                    int aligned = padStart;
                    while (aligned % 4 != 0) aligned++;
                    int newPad = aligned - padStart;
                    int origPadStart = inst.offset + 1;
                    int origAligned = origPadStart;
                    while (origAligned % 4 != 0) origAligned++;
                    int origPad = origAligned - origPadStart;
                    int newLen = 1 + newPad + (inst.length - 1 - origPad);
                    curNew += newLen;
                } else {
                    curNew += inst.length;
                }
            }
            int[] epilogueShift = new int[n + 1];
            int epilogueSoFar = 0;
            for (int i = 0; i < n; i++) {
                epilogueShift[i] = epilogueSoFar;
                if (Opcodes.isReturn(insns.get(i).opcode)) {
                    epilogueSoFar += epilogue.length;
                }
            }
            epilogueShift[n] = epilogueSoFar;
            for (int i = 0; i < n; i++) {
                newPc[i] += epilogueShift[i];
            }
            int totalNew = curNew + epilogueSoFar;

            int[] origPcToIndex = new int[originalCode.length + 1];
            java.util.Arrays.fill(origPcToIndex, -1);
            for (int i = 0; i < n; i++) {
                origPcToIndex[insns.get(i).offset] = i;
            }

            int[] oldToNew = new int[originalCode.length + 1];
            java.util.Arrays.fill(oldToNew, -1);
            for (int i = 0; i < n; i++) {
                int start = insns.get(i).offset;
                int endPc = (i + 1 < n) ? insns.get(i + 1).offset : originalCode.length;
                int newStart = newPc[i];
                for (int pc = start; pc < endPc; pc++) {
                    oldToNew[pc] = newStart + (pc - start);
                }
            }
            oldToNew[originalCode.length] = totalNew;

            byte[] result = new byte[totalNew];
            int pos = 0;
            for (byte b : prologue) result[pos++] = b;

            for (int i = 0; i < n; i++) {
                Instruction inst = insns.get(i);
                int np = newPc[i];

                if (inst.opcode == Opcodes.TABLESWITCH || inst.opcode == Opcodes.LOOKUPSWITCH) {
                    pos = writeSwitch(result, pos, originalCode, inst.offset, inst.opcode, newPc, origPcToIndex);
                    continue;
                }

                if (Opcodes.isReturn(inst.opcode)) {
                    for (byte b : epilogue) result[pos++] = b;
                    result[pos++] = (byte) inst.opcode;
                    continue;
                }

                boolean isShortBranch = (inst.opcode == Opcodes.GOTO || inst.opcode == Opcodes.JSR
                    || (inst.opcode >= Opcodes.IFEQ && inst.opcode <= Opcodes.IF_ACMPNE)
                    || inst.opcode == Opcodes.IFNULL || inst.opcode == Opcodes.IFNONNULL);
                boolean isLongBranch = (inst.opcode == Opcodes.GOTO_W || inst.opcode == Opcodes.JSR_W);

                if (isShortBranch) {
                    int origOffset = (short) (((originalCode[inst.offset + 1] & 0xFF) << 8) | (originalCode[inst.offset + 2] & 0xFF));
                    int origTarget = inst.offset + origOffset;
                    int targetIdx = origPcToIndex[origTarget];
                    int newTarget = newPc[targetIdx];
                    int newOffset = newTarget - np;
                    result[pos++] = originalCode[inst.offset];
                    result[pos++] = (byte) ((newOffset >> 8) & 0xFF);
                    result[pos++] = (byte) (newOffset & 0xFF);
                    continue;
                }

                if (isLongBranch) {
                    int origOffset = ((originalCode[inst.offset + 1] & 0xFF) << 24) | ((originalCode[inst.offset + 2] & 0xFF) << 16)
                                   | ((originalCode[inst.offset + 3] & 0xFF) << 8) | (originalCode[inst.offset + 4] & 0xFF);
                    int origTarget = inst.offset + origOffset;
                    int targetIdx = origPcToIndex[origTarget];
                    int newTarget = newPc[targetIdx];
                    int newOffset = newTarget - np;
                    result[pos++] = originalCode[inst.offset];
                    result[pos++] = (byte) ((newOffset >> 24) & 0xFF);
                    result[pos++] = (byte) ((newOffset >> 16) & 0xFF);
                    result[pos++] = (byte) ((newOffset >> 8) & 0xFF);
                    result[pos++] = (byte) (newOffset & 0xFF);
                    continue;
                }

                for (int j = 0; j < inst.length; j++) {
                    result[pos++] = originalCode[inst.offset + j];
                }
            }

            return new BuildResult(result, oldToNew);
        }

        private int writeSwitch(byte[] result, int pos, byte[] originalCode, int switchPc, int opcode, int[] newPc, int[] origPcToIndex) {
            int newSwitchPc = pos;
            result[pos++] = originalCode[switchPc];
            while (pos % 4 != 0) result[pos++] = 0;

            int p = switchPc + 1;
            while (p % 4 != 0) p++;

            int defOffset = readInt(originalCode, p); p += 4;
            int defTarget = switchPc + defOffset;
            int defIdx = origPcToIndex[defTarget];
            if (defIdx < 0) defIdx = 0;
            int newDefTarget = newPc[defIdx];
            int newDefOffset = newDefTarget - newSwitchPc;
            writeInt(result, pos, newDefOffset); pos += 4;

            if (opcode == Opcodes.TABLESWITCH) {
                int low = readInt(originalCode, p); p += 4;
                int high = readInt(originalCode, p); p += 4;
                writeInt(result, pos, low); pos += 4;
                writeInt(result, pos, high); pos += 4;
                int count = high - low + 1;
                for (int i = 0; i < count; i++) {
                    int off = readInt(originalCode, p); p += 4;
                    int target = switchPc + off;
                    int tIdx = origPcToIndex[target];
                    if (tIdx < 0) tIdx = 0;
                    int newTarget = newPc[tIdx];
                    int newOff = newTarget - newSwitchPc;
                    writeInt(result, pos, newOff); pos += 4;
                }
            } else {
                int npairs = readInt(originalCode, p); p += 4;
                writeInt(result, pos, npairs); pos += 4;
                for (int i = 0; i < npairs; i++) {
                    int key = readInt(originalCode, p); p += 4;
                    int off = readInt(originalCode, p); p += 4;
                    int target = switchPc + off;
                    int tIdx = origPcToIndex[target];
                    if (tIdx < 0) tIdx = 0;
                    int newTarget = newPc[tIdx];
                    int newOff = newTarget - newSwitchPc;
                    writeInt(result, pos, key); pos += 4;
                    writeInt(result, pos, newOff); pos += 4;
                }
            }
            return pos;
        }

        private static void writeInt(byte[] arr, int pos, int value) {
            arr[pos] = (byte) ((value >> 24) & 0xFF);
            arr[pos + 1] = (byte) ((value >> 16) & 0xFF);
            arr[pos + 2] = (byte) ((value >> 8) & 0xFF);
            arr[pos + 3] = (byte) (value & 0xFF);
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
