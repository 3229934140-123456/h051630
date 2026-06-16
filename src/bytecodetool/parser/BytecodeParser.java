package bytecodetool.parser;

import bytecodetool.model.Instruction;
import bytecodetool.opcode.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class BytecodeParser {

    public static List<Instruction> parse(byte[] code) {
        List<Instruction> instructions = new ArrayList<>();
        int pc = 0;
        while (pc < code.length) {
            int offset = pc;
            int opcode = code[pc] & 0xFF;
            pc++;
            Instruction inst;
            switch (opcode) {
                case Opcodes.NOP:
                case Opcodes.ACONST_NULL:
                case Opcodes.ICONST_M1:
                case Opcodes.ICONST_0:
                case Opcodes.ICONST_1:
                case Opcodes.ICONST_2:
                case Opcodes.ICONST_3:
                case Opcodes.ICONST_4:
                case Opcodes.ICONST_5:
                case Opcodes.LCONST_0:
                case Opcodes.LCONST_1:
                case Opcodes.FCONST_0:
                case Opcodes.FCONST_1:
                case Opcodes.FCONST_2:
                case Opcodes.DCONST_0:
                case Opcodes.DCONST_1:
                case Opcodes.ILOAD_0: case Opcodes.ILOAD_1: case Opcodes.ILOAD_2: case Opcodes.ILOAD_3:
                case Opcodes.LLOAD_0: case Opcodes.LLOAD_1: case Opcodes.LLOAD_2: case Opcodes.LLOAD_3:
                case Opcodes.FLOAD_0: case Opcodes.FLOAD_1: case Opcodes.FLOAD_2: case Opcodes.FLOAD_3:
                case Opcodes.DLOAD_0: case Opcodes.DLOAD_1: case Opcodes.DLOAD_2: case Opcodes.DLOAD_3:
                case Opcodes.ALOAD_0: case Opcodes.ALOAD_1: case Opcodes.ALOAD_2: case Opcodes.ALOAD_3:
                case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD: case Opcodes.DALOAD:
                case Opcodes.AALOAD: case Opcodes.BALOAD: case Opcodes.CALOAD: case Opcodes.SALOAD:
                case Opcodes.ISTORE_0: case Opcodes.ISTORE_1: case Opcodes.ISTORE_2: case Opcodes.ISTORE_3:
                case Opcodes.LSTORE_0: case Opcodes.LSTORE_1: case Opcodes.LSTORE_2: case Opcodes.LSTORE_3:
                case Opcodes.FSTORE_0: case Opcodes.FSTORE_1: case Opcodes.FSTORE_2: case Opcodes.FSTORE_3:
                case Opcodes.DSTORE_0: case Opcodes.DSTORE_1: case Opcodes.DSTORE_2: case Opcodes.DSTORE_3:
                case Opcodes.ASTORE_0: case Opcodes.ASTORE_1: case Opcodes.ASTORE_2: case Opcodes.ASTORE_3:
                case Opcodes.IASTORE: case Opcodes.LASTORE: case Opcodes.FASTORE: case Opcodes.DASTORE:
                case Opcodes.AASTORE: case Opcodes.BASTORE: case Opcodes.CASTORE: case Opcodes.SASTORE:
                case Opcodes.POP: case Opcodes.POP2:
                case Opcodes.DUP: case Opcodes.DUP_X1: case Opcodes.DUP_X2:
                case Opcodes.DUP2: case Opcodes.DUP2_X1: case Opcodes.DUP2_X2:
                case Opcodes.SWAP:
                case Opcodes.IADD: case Opcodes.LADD: case Opcodes.FADD: case Opcodes.DADD:
                case Opcodes.ISUB: case Opcodes.LSUB: case Opcodes.FSUB: case Opcodes.DSUB:
                case Opcodes.IMUL: case Opcodes.LMUL: case Opcodes.FMUL: case Opcodes.DMUL:
                case Opcodes.IDIV: case Opcodes.LDIV: case Opcodes.FDIV: case Opcodes.DDIV:
                case Opcodes.IREM: case Opcodes.LREM: case Opcodes.FREM: case Opcodes.DREM:
                case Opcodes.INEG: case Opcodes.LNEG: case Opcodes.FNEG: case Opcodes.DNEG:
                case Opcodes.ISHL: case Opcodes.LSHL:
                case Opcodes.ISHR: case Opcodes.LSHR:
                case Opcodes.IUSHR: case Opcodes.LUSHR:
                case Opcodes.IAND: case Opcodes.LAND:
                case Opcodes.IOR: case Opcodes.LOR:
                case Opcodes.IXOR: case Opcodes.LXOR:
                case Opcodes.I2L: case Opcodes.I2F: case Opcodes.I2D:
                case Opcodes.L2I: case Opcodes.L2F: case Opcodes.L2D:
                case Opcodes.F2I: case Opcodes.F2L: case Opcodes.F2D:
                case Opcodes.D2I: case Opcodes.D2L: case Opcodes.D2F:
                case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S:
                case Opcodes.LCMP:
                case Opcodes.FCMPL: case Opcodes.FCMPG:
                case Opcodes.DCMPL: case Opcodes.DCMPG:
                case Opcodes.IRETURN: case Opcodes.LRETURN:
                case Opcodes.FRETURN: case Opcodes.DRETURN:
                case Opcodes.ARETURN: case Opcodes.RETURN:
                case Opcodes.ARRAYLENGTH:
                case Opcodes.ATHROW:
                case Opcodes.MONITORENTER: case Opcodes.MONITOREXIT:
                    inst = new Instruction(opcode, offset, 1);
                    break;
                case Opcodes.BIPUSH:
                case Opcodes.LDC:
                case Opcodes.ILOAD: case Opcodes.LLOAD: case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD:
                case Opcodes.ISTORE: case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
                case Opcodes.RET:
                case Opcodes.NEWARRAY:
                    inst = new Instruction(opcode, offset, 2, new Object[]{code[pc] & 0xFF});
                    pc++;
                    break;
                case Opcodes.SIPUSH:
                case Opcodes.LDC_W: case Opcodes.LDC2_W:
                case Opcodes.GETSTATIC: case Opcodes.PUTSTATIC:
                case Opcodes.GETFIELD: case Opcodes.PUTFIELD:
                case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC:
                case Opcodes.NEW: case Opcodes.ANEWARRAY:
                case Opcodes.CHECKCAST: case Opcodes.INSTANCEOF:
                case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE: case Opcodes.IFGT: case Opcodes.IFLE:
                case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
                case Opcodes.GOTO: case Opcodes.JSR:
                case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                    {
                        int operand = (short) (((code[pc] & 0xFF) << 8) | (code[pc + 1] & 0xFF));
                        inst = new Instruction(opcode, offset, 3, new Object[]{operand});
                        pc += 2;
                    }
                    break;
                case Opcodes.IINC:
                    {
                        int var = code[pc] & 0xFF;
                        int increment = code[pc + 1];
                        inst = new Instruction(opcode, offset, 3, new Object[]{var, increment});
                        pc += 2;
                    }
                    break;
                case Opcodes.MULTIANEWARRAY:
                    {
                        int index = ((code[pc] & 0xFF) << 8) | (code[pc + 1] & 0xFF);
                        int dimensions = code[pc + 2] & 0xFF;
                        inst = new Instruction(opcode, offset, 4, new Object[]{index, dimensions});
                        pc += 3;
                    }
                    break;
                case Opcodes.INVOKEINTERFACE:
                    {
                        int index = ((code[pc] & 0xFF) << 8) | (code[pc + 1] & 0xFF);
                        int count = code[pc + 2] & 0xFF;
                        int zero = code[pc + 3] & 0xFF;
                        inst = new Instruction(opcode, offset, 5, new Object[]{index, count, zero});
                        pc += 4;
                    }
                    break;
                case Opcodes.INVOKEDYNAMIC:
                    {
                        int index2 = ((code[pc] & 0xFF) << 8) | (code[pc + 1] & 0xFF);
                        int zero1 = code[pc + 2] & 0xFF;
                        int zero2 = code[pc + 3] & 0xFF;
                        inst = new Instruction(opcode, offset, 5, new Object[]{index2, zero1, zero2});
                        pc += 4;
                    }
                    break;
                case Opcodes.GOTO_W:
                case Opcodes.JSR_W:
                    {
                        int operand = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                    | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        inst = new Instruction(opcode, offset, 5, new Object[]{operand});
                        pc += 4;
                    }
                    break;
                case Opcodes.WIDE:
                    {
                        int wideOpcode = code[pc] & 0xFF;
                        pc++;
                        if (wideOpcode == Opcodes.IINC) {
                            int var = (code[pc] & 0xFF) << 8 | (code[pc + 1] & 0xFF);
                            int increment = (short) ((code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF));
                            inst = new Instruction(opcode, offset, 6, new Object[]{wideOpcode, var, increment});
                            pc += 4;
                        } else {
                            int var = (code[pc] & 0xFF) << 8 | (code[pc + 1] & 0xFF);
                            inst = new Instruction(opcode, offset, 4, new Object[]{wideOpcode, var});
                            pc += 2;
                        }
                    }
                    break;
                case Opcodes.TABLESWITCH:
                    {
                        int padStart = pc;
                        while (pc % 4 != 0) pc++;
                        int padding = pc - padStart;
                        int defaultByte = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                        | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        pc += 4;
                        int low = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        pc += 4;
                        int high = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                 | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        pc += 4;
                        int count = high - low + 1;
                        int[] offsets = new int[count];
                        for (int i = 0; i < count; i++) {
                            offsets[i] = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                       | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                            pc += 4;
                        }
                        int length = 1 + padding + 4 + 4 + 4 + count * 4;
                        inst = new Instruction(opcode, offset, length, new Object[]{defaultByte, low, high, offsets, padding});
                    }
                    break;
                case Opcodes.LOOKUPSWITCH:
                    {
                        int padStart = pc;
                        while (pc % 4 != 0) pc++;
                        int padding = pc - padStart;
                        int defaultByte = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                        | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        pc += 4;
                        int npairs = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                   | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                        pc += 4;
                        int[] keys = new int[npairs];
                        int[] offsets = new int[npairs];
                        for (int i = 0; i < npairs; i++) {
                            keys[i] = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                    | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                            pc += 4;
                            offsets[i] = (code[pc] & 0xFF) << 24 | (code[pc + 1] & 0xFF) << 16
                                       | (code[pc + 2] & 0xFF) << 8 | (code[pc + 3] & 0xFF);
                            pc += 4;
                        }
                        int length = 1 + padding + 4 + 4 + npairs * 8;
                        inst = new Instruction(opcode, offset, length, new Object[]{defaultByte, npairs, keys, offsets, padding});
                    }
                    break;
                default:
                    inst = new Instruction(opcode, offset, 1);
                    break;
            }
            instructions.add(inst);
        }
        return instructions;
    }

    public static int getInstructionLength(byte[] code, int pc) {
        int opcode = code[pc] & 0xFF;
        switch (opcode) {
            case Opcodes.BIPUSH:
            case Opcodes.LDC:
            case Opcodes.ILOAD: case Opcodes.LLOAD: case Opcodes.FLOAD: case Opcodes.DLOAD: case Opcodes.ALOAD:
            case Opcodes.ISTORE: case Opcodes.LSTORE: case Opcodes.FSTORE: case Opcodes.DSTORE: case Opcodes.ASTORE:
            case Opcodes.RET:
            case Opcodes.NEWARRAY:
                return 2;
            case Opcodes.SIPUSH:
            case Opcodes.LDC_W: case Opcodes.LDC2_W:
            case Opcodes.GETSTATIC: case Opcodes.PUTSTATIC:
            case Opcodes.GETFIELD: case Opcodes.PUTFIELD:
            case Opcodes.INVOKEVIRTUAL: case Opcodes.INVOKESPECIAL: case Opcodes.INVOKESTATIC:
            case Opcodes.NEW: case Opcodes.ANEWARRAY:
            case Opcodes.CHECKCAST: case Opcodes.INSTANCEOF:
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE: case Opcodes.IFGT: case Opcodes.IFLE:
            case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE: case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
            case Opcodes.GOTO: case Opcodes.JSR:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
            case Opcodes.IINC:
                return 3;
            case Opcodes.MULTIANEWARRAY:
                return 4;
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKEDYNAMIC:
            case Opcodes.GOTO_W:
            case Opcodes.JSR_W:
                return 5;
            case Opcodes.WIDE:
                int wideOp = code[pc + 1] & 0xFF;
                return wideOp == Opcodes.IINC ? 6 : 4;
            case Opcodes.TABLESWITCH:
            case Opcodes.LOOKUPSWITCH: {
                int padStart = pc + 1;
                int aligned = padStart;
                while (aligned % 4 != 0) aligned++;
                int padding = aligned - padStart;
                aligned += 4;
                if (opcode == Opcodes.TABLESWITCH) {
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
            default:
                return 1;
        }
    }

    public static int computeMaxStack(List<Instruction> instructions) {
        int maxStack = 0;
        int currentStack = 0;
        for (Instruction inst : instructions) {
            currentStack += Opcodes.getStackEffect(inst.opcode);
            if (inst.opcode == Opcodes.INVOKEVIRTUAL
                || inst.opcode == Opcodes.INVOKESPECIAL
                || inst.opcode == Opcodes.INVOKESTATIC
                || inst.opcode == Opcodes.INVOKEINTERFACE
                || inst.opcode == Opcodes.INVOKEDYNAMIC) {
            }
            if (currentStack > maxStack) {
                maxStack = currentStack;
            }
            if (currentStack < 0) {
                currentStack = 0;
            }
        }
        return maxStack;
    }

    public static String formatInstruction(Instruction inst) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%5d: %s", inst.offset, Opcodes.getOpcodeName(inst.opcode)));
        if (inst.operands != null && inst.operands.length > 0) {
            sb.append(" ");
            for (int i = 0; i < inst.operands.length; i++) {
                if (i > 0) sb.append(", ");
                Object op = inst.operands[i];
                if (op instanceof int[]) {
                    sb.append("[");
                    int[] arr = (int[]) op;
                    for (int j = 0; j < arr.length; j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(arr[j]);
                    }
                    sb.append("]");
                } else {
                    sb.append(op);
                }
            }
        }
        return sb.toString();
    }
}
