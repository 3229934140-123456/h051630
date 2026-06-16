package bytecodetool.util;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.model.CpInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.pool.ConstantPool;

import java.util.*;

public class Disassembler {

    public static String disassemble(ClassFile cf, ConstantPool pool) {
        StringBuilder sb = new StringBuilder();
        String className = pool.getClassName(cf.thisClass);
        String superName = cf.superClass > 0 ? pool.getClassName(cf.superClass) : "java/lang/Object";

        sb.append("Classfile ").append(className.replace('/', '.')).append("\n");
        sb.append("  ").append(accessFlags(cf.accessFlags)).append(className.replace('/', '.'));
        if (cf.superClass > 0 && !"java/lang/Object".equals(superName)) {
            sb.append(" extends ").append(superName.replace('/', '.'));
        }
        if (cf.interfaces != null && cf.interfaces.length > 0) {
            sb.append(" implements");
            for (int i : cf.interfaces) sb.append(" ").append(pool.getClassName(i).replace('/', '.'));
        }
        sb.append("\n  minor version: ").append(cf.minorVersion).append("\n");
        sb.append("  major version: ").append(cf.majorVersion).append("\n");
        sb.append("  flags: ").append(String.format("(0x%04x)", cf.accessFlags)).append("\n");
        sb.append("  this_class: #").append(cf.thisClass);
        sb.append("  super_class: #").append(cf.superClass);
        sb.append("  interfaces: ").append(cf.interfaces != null ? cf.interfaces.length : 0);
        sb.append(", fields: ").append(cf.fields != null ? cf.fields.length : 0);
        sb.append(", methods: ").append(cf.methods != null ? cf.methods.length : 0);
        sb.append(", attributes: ").append(cf.attributes != null ? cf.attributes.length : 0).append("\n\n");

        sb.append("Constant pool:\n");
        dumpConstantPool(sb, pool);
        sb.append("\n");

        if (cf.fields != null) {
            for (FieldInfo f : cf.fields) {
                sb.append(dumpField(f, pool)).append("\n");
            }
        }

        if (cf.methods != null) {
            for (MethodInfo m : cf.methods) {
                sb.append(dumpMethod(m, pool)).append("\n");
            }
        }

        return sb.toString();
    }

    public static String tagName(int tag) {
        switch (tag) {
            case 1: return "Utf8";
            case 3: return "Integer";
            case 4: return "Float";
            case 5: return "Long";
            case 6: return "Double";
            case 7: return "Class";
            case 8: return "String";
            case 9: return "Fieldref";
            case 10: return "Methodref";
            case 11: return "InterfaceMethodref";
            case 12: return "NameAndType";
            case 15: return "MethodHandle";
            case 16: return "MethodType";
            case 18: return "InvokeDynamic";
            default: return "Unknown_" + tag;
        }
    }

    private static void dumpConstantPool(StringBuilder sb, ConstantPool pool) {
        List<CpInfo> entries = pool.getAll();
        for (int i = 1; i < entries.size(); i++) {
            CpInfo cp = entries.get(i);
            if (cp == null) continue;
            String desc = describeCp(cp, pool);
            sb.append(String.format("  #%d = %-20s %s%n", i, tagName(cp.tag), desc));
            if (cp instanceof LongInfo || cp instanceof DoubleInfo) i++;
        }
    }

    public static String describeCp(CpInfo cp, ConstantPool pool) {
        if (cp instanceof Utf8Info) return "\"" + ((Utf8Info) cp).value + "\"";
        if (cp instanceof IntegerInfo) return String.valueOf(((IntegerInfo) cp).value);
        if (cp instanceof FloatInfo) return String.valueOf(((FloatInfo) cp).value);
        if (cp instanceof LongInfo) return String.valueOf(((LongInfo) cp).value);
        if (cp instanceof DoubleInfo) return String.valueOf(((DoubleInfo) cp).value);
        if (cp instanceof ClassInfo) return "#" + ((ClassInfo) cp).nameIndex + "  // " + pool.getUtf8(((ClassInfo) cp).nameIndex);
        if (cp instanceof StringInfo) return "#" + ((StringInfo) cp).stringIndex + "  // " + pool.getUtf8(((StringInfo) cp).stringIndex);
        if (cp instanceof FieldrefInfo) return "#" + ((FieldrefInfo) cp).classIndex + ".#" + ((FieldrefInfo) cp).nameAndTypeIndex;
        if (cp instanceof MethodrefInfo) return "#" + ((MethodrefInfo) cp).classIndex + ".#" + ((MethodrefInfo) cp).nameAndTypeIndex;
        if (cp instanceof InterfaceMethodrefInfo) return "#" + ((InterfaceMethodrefInfo) cp).classIndex + ".#" + ((InterfaceMethodrefInfo) cp).nameAndTypeIndex;
        if (cp instanceof NameAndTypeInfo) return "#" + ((NameAndTypeInfo) cp).nameIndex + ":#" + ((NameAndTypeInfo) cp).descriptorIndex;
        if (cp instanceof MethodHandleInfo) return ((MethodHandleInfo) cp).referenceKind + ".#" + ((MethodHandleInfo) cp).referenceIndex;
        if (cp instanceof MethodTypeInfo) return "#" + ((MethodTypeInfo) cp).descriptorIndex;
        if (cp instanceof InvokeDynamicInfo) return "#" + ((InvokeDynamicInfo) cp).bootstrapMethodAttrIndex + ":#" + ((InvokeDynamicInfo) cp).nameAndTypeIndex;
        return "?";
    }

    private static String accessFlags(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 0x0001) != 0) sb.append("public ");
        if ((flags & 0x0002) != 0) sb.append("private ");
        if ((flags & 0x0004) != 0) sb.append("protected ");
        if ((flags & 0x0008) != 0) sb.append("static ");
        if ((flags & 0x0010) != 0) sb.append("final ");
        if ((flags & 0x0020) != 0) sb.append("synchronized ");
        if ((flags & 0x0040) != 0) sb.append("bridge ");
        if ((flags & 0x0080) != 0) sb.append("varargs ");
        if ((flags & 0x0100) != 0) sb.append("native ");
        if ((flags & 0x0200) != 0) sb.append("interface ");
        if ((flags & 0x0400) != 0) sb.append("abstract ");
        if ((flags & 0x0800) != 0) sb.append("strict ");
        if ((flags & 0x1000) != 0) sb.append("synthetic ");
        return sb.toString();
    }

    private static String dumpField(FieldInfo f, ConstantPool pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(accessFlags(f.accessFlags));
        sb.append(pool.getUtf8(f.descriptorIndex)).append(" ");
        sb.append(pool.getUtf8(f.nameIndex));
        sb.append(";").append(String.format(" // descriptor: %s%n", pool.getUtf8(f.descriptorIndex)));
        sb.append("    flags: ").append(String.format("(0x%04x)", f.accessFlags)).append("\n");
        if (f.attributes != null) {
            for (AttributeInfo a : f.attributes) {
                if (a instanceof ConstantValueAttribute) {
                    ConstantValueAttribute cv = (ConstantValueAttribute) a;
                    sb.append("    ConstantValue: ").append(cpValue(cv.constantValueIndex, pool)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String cpValue(int idx, ConstantPool pool) {
        CpInfo cp = pool.get(idx);
        if (cp instanceof IntegerInfo) return "int " + ((IntegerInfo) cp).value;
        if (cp instanceof LongInfo) return "long " + ((LongInfo) cp).value;
        if (cp instanceof FloatInfo) return "float " + ((FloatInfo) cp).value;
        if (cp instanceof DoubleInfo) return "double " + ((DoubleInfo) cp).value;
        if (cp instanceof StringInfo) return "String \"" + pool.getUtf8(((StringInfo) cp).stringIndex) + "\"";
        return "#" + idx;
    }

    private static String dumpMethod(MethodInfo m, ConstantPool pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(accessFlags(m.accessFlags));
        String name = pool.getUtf8(m.nameIndex);
        String desc = pool.getUtf8(m.descriptorIndex);
        sb.append(prettySignature(name, desc)).append(";\n");
        sb.append("    descriptor: ").append(desc).append("\n");
        sb.append("    flags: ").append(String.format("(0x%04x)", m.accessFlags)).append("\n");

        CodeAttribute ca = m.getCodeAttribute();
        if (ca != null) {
            sb.append("    Code:\n");
            sb.append("      stack=").append(ca.maxStack).append(", locals=").append(ca.maxLocals).append(", args_size=").append(argsSize(desc, m.accessFlags)).append("\n");
            dumpBytecode(sb, ca, pool);
            if (ca.exceptionTable != null && ca.exceptionTable.length > 0) {
                sb.append("      Exception table:\n");
                sb.append("         from    to  target type\n");
                for (CodeAttribute.ExceptionTableEntry e : ca.exceptionTable) {
                    String catchType = e.catchType == 0 ? "any" : pool.getClassName(e.catchType);
                    sb.append(String.format("         %4d %4d %4d   Class %s%n",
                        e.startPc, e.endPc, e.handlerPc, catchType));
                }
            }
            if (ca.attributes != null) {
                for (AttributeInfo a : ca.attributes) {
                    if (a instanceof LineNumberTableAttribute) {
                        LineNumberTableAttribute ln = (LineNumberTableAttribute) a;
                        sb.append("      LineNumberTable:\n");
                        sb.append("        line  ").append(ln.lineNumberTable.length).append("\n");
                        for (LineNumberTableAttribute.LineNumberEntry e : ln.lineNumberTable) {
                            sb.append(String.format("          line %d: %d%n", e.lineNumber, e.startPc));
                        }
                    } else if (a instanceof LocalVariableTableAttribute) {
                        LocalVariableTableAttribute lv = (LocalVariableTableAttribute) a;
                        sb.append("      LocalVariableTable:\n");
                        sb.append(String.format("        Start  Length  Slot  Name   Signature%n"));
                        for (LocalVariableTableAttribute.LocalVariableEntry e : lv.localVariableTable) {
                            sb.append(String.format("          %5d %6d %4d %-10s %s%n",
                                e.startPc, e.length, e.index, pool.getUtf8(e.nameIndex), pool.getUtf8(e.descriptorIndex)));
                        }
                    } else if (a instanceof StackMapTableAttribute) {
                        dumpStackMapTable(sb, (StackMapTableAttribute) a, pool);
                    }
                }
            }
        }

        if (m.attributes != null) {
            for (AttributeInfo a : m.attributes) {
                if (a instanceof ExceptionsAttribute) {
                    ExceptionsAttribute ex = (ExceptionsAttribute) a;
                    sb.append("    Exceptions:\n");
                    for (int idx : ex.exceptionIndexTable) {
                        sb.append("      throws ").append(pool.getClassName(idx).replace('/', '.')).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private static int argsSize(String desc, int accessFlags) {
        int n = (accessFlags & 0x0008) != 0 ? 0 : 1;
        int i = 1;
        while (i < desc.length() && desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == 'L') { i = desc.indexOf(';', i) + 1; n++; }
            else if (c == '[') { i++; }
            else if (c == 'J' || c == 'D') { n += 2; i++; }
            else { n++; i++; }
        }
        return n;
    }

    private static String prettySignature(String name, String desc) {
        StringBuilder sb = new StringBuilder();
        int paren = desc.indexOf(')');
        String ret = desc.substring(paren + 1);
        sb.append(typeToName(ret)).append(" ").append(name).append("(");
        List<String> params = new ArrayList<>();
        int i = 1;
        while (i < paren) {
            int arr = 0;
            while (desc.charAt(i) == '[') { arr++; i++; }
            String t;
            if (desc.charAt(i) == 'L') {
                int end = desc.indexOf(';', i);
                t = desc.substring(i, end + 1);
                i = end + 1;
            } else {
                t = String.valueOf(desc.charAt(i));
                i++;
            }
            for (int a = 0; a < arr; a++) t += "[]";
            params.add(typeToName(t));
        }
        sb.append(String.join(", ", params)).append(")");
        return sb.toString();
    }

    private static String typeToName(String t) {
        if (t.startsWith("[")) {
            int arr = 0;
            while (t.startsWith("[")) { t = t.substring(1); arr++; }
            String base = typeToName(t);
            for (int a = 0; a < arr; a++) base += "[]";
            return base;
        }
        if (t.startsWith("L") && t.endsWith(";")) return t.substring(1, t.length() - 1).replace('/', '.');
        switch (t) {
            case "V": return "void";
            case "Z": return "boolean";
            case "B": return "byte";
            case "C": return "char";
            case "S": return "short";
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
            default: return t;
        }
    }

    private static void dumpBytecode(StringBuilder sb, CodeAttribute ca, ConstantPool pool) {
        List<Instruction> insns = BytecodeParser.parse(ca.code);
        for (Instruction inst : insns) {
            sb.append(formatInstruction(inst, pool, ca.code));
        }
    }

    public static String formatInstruction(Instruction inst, ConstantPool pool, byte[] rawCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%8d: ", inst.offset));
        sb.append(Opcodes.getOpcodeName(inst.opcode));
        if (inst.operands != null && inst.operands.length > 0) {
            sb.append("\t");
            int opcode = inst.opcode;
            Object[] ops = inst.operands;

            if (opcode == Opcodes.LDC || opcode == Opcodes.LDC_W) {
                sb.append("#").append(ops[0]).append("  // ").append(describeCp(pool.get((Integer) ops[0]), pool));
            } else if (opcode == Opcodes.LDC2_W) {
                sb.append("#").append(ops[0]).append("  // ").append(describeCp(pool.get((Integer) ops[0]), pool));
            } else if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC
                    || opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                int idx = (Integer) ops[0];
                CpInfo.FieldrefInfo fr = (CpInfo.FieldrefInfo) pool.get(idx);
                CpInfo.NameAndTypeInfo nt = (CpInfo.NameAndTypeInfo) pool.get(fr.nameAndTypeIndex);
                sb.append("#").append(idx).append("  // Field ").append(pool.getUtf8(nt.nameIndex)).append(":").append(pool.getUtf8(nt.descriptorIndex));
            } else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESTATIC
                    || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEINTERFACE
                    || opcode == Opcodes.INVOKEDYNAMIC) {
                int idx = (Integer) ops[0];
                CpInfo cp = pool.get(idx);
                sb.append("#").append(idx).append("  // Method ");
                if (cp instanceof CpInfo.MethodrefInfo) {
                    CpInfo.MethodrefInfo mr = (CpInfo.MethodrefInfo) cp;
                    CpInfo.NameAndTypeInfo nt = (CpInfo.NameAndTypeInfo) pool.get(mr.nameAndTypeIndex);
                    sb.append(pool.getUtf8(nt.nameIndex)).append(":").append(pool.getUtf8(nt.descriptorIndex));
                } else if (cp instanceof CpInfo.InterfaceMethodrefInfo) {
                    CpInfo.InterfaceMethodrefInfo ir = (CpInfo.InterfaceMethodrefInfo) cp;
                    CpInfo.NameAndTypeInfo nt = (CpInfo.NameAndTypeInfo) pool.get(ir.nameAndTypeIndex);
                    sb.append(pool.getUtf8(nt.nameIndex)).append(":").append(pool.getUtf8(nt.descriptorIndex));
                } else if (cp instanceof CpInfo.InvokeDynamicInfo) {
                    CpInfo.InvokeDynamicInfo id = (CpInfo.InvokeDynamicInfo) cp;
                    CpInfo.NameAndTypeInfo nt = (CpInfo.NameAndTypeInfo) pool.get(id.nameAndTypeIndex);
                    sb.append(pool.getUtf8(nt.nameIndex)).append(":").append(pool.getUtf8(nt.descriptorIndex));
                }
            } else if (opcode == Opcodes.NEW) {
                int idx = (Integer) ops[0];
                sb.append("#").append(idx).append("  // class ").append(pool.getClassName(idx));
            } else if (opcode == Opcodes.ANEWARRAY) {
                int idx = (Integer) ops[0];
                sb.append("#").append(idx).append("  // class ").append(pool.getClassName(idx));
            } else if (opcode == Opcodes.CHECKCAST) {
                int idx = (Integer) ops[0];
                sb.append("#").append(idx).append("  // class ").append(pool.getClassName(idx));
            } else if (opcode == Opcodes.INSTANCEOF) {
                int idx = (Integer) ops[0];
                sb.append("#").append(idx).append("  // class ").append(pool.getClassName(idx));
            } else if (opcode == Opcodes.MULTIANEWARRAY) {
                int idx = (Integer) ops[0];
                int dim = (Integer) ops[1];
                sb.append("#").append(idx).append(", ").append(dim).append("  // ").append(pool.getClassName(idx));
            } else if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR) {
                int off = (Integer) ops[0];
                sb.append(String.format("%-20s // %d", "", inst.offset + off));
            } else if (opcode == Opcodes.GOTO_W || opcode == Opcodes.JSR_W) {
                int off = (Integer) ops[0];
                sb.append(String.format("%-20s // %d", "", inst.offset + off));
            } else if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE) {
                int off = (Integer) ops[0];
                sb.append(String.format("%-20s // %d", "", inst.offset + off));
            } else if (opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL) {
                int off = (Integer) ops[0];
                sb.append(String.format("%-20s // %d", "", inst.offset + off));
            } else if (opcode == Opcodes.TABLESWITCH) {
                sb.append("\n");
                int padding = (Integer) ops[ops.length - 1];
                int p = inst.offset + 1 + padding;
                int defOff = (Integer) ops[0];
                int low = (Integer) ops[1];
                int high = (Integer) ops[2];
                int[] offs = (int[]) ops[3];
                sb.append(String.format("%12s %d + default: %d%n", "", 0, inst.offset + defOff));
                for (int k = low; k <= high; k++) {
                    sb.append(String.format("%12s %d: %d%n", "", k, inst.offset + offs[k - low]));
                }
            } else if (opcode == Opcodes.LOOKUPSWITCH) {
                sb.append("\n");
                int defOff = (Integer) ops[0];
                int npairs = (Integer) ops[1];
                int[] keys = (int[]) ops[2];
                int[] offs = (int[]) ops[3];
                sb.append(String.format("%12s default: %d%n", "", inst.offset + defOff));
                for (int i = 0; i < keys.length; i++) {
                    sb.append(String.format("%12s %d: %d%n", "", keys[i], inst.offset + offs[i]));
                }
            } else if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                sb.append(ops[0]);
            } else if (opcode == Opcodes.IINC) {
                sb.append(ops[0]).append(", ").append(ops[1]);
            } else {
                for (int i = 0; i < ops.length; i++) {
                    if (i > 0) sb.append(", ");
                    Object op = ops[i];
                    if (op instanceof int[]) sb.append(Arrays.toString((int[]) op));
                    else sb.append(op);
                }
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static void dumpStackMapTable(StringBuilder sb, StackMapTableAttribute smt, ConstantPool pool) {
        sb.append("      StackMapTable: number_of_entries = ").append(smt.entries.length).append("\n");
        int running = 0;
        for (int i = 0; i < smt.entries.length; i++) {
            StackMapTableAttribute.StackMapFrame f = smt.entries[i];
            int delta = frameDelta(f);
            running += delta;
            String frameType = frameTypeName(f);
            sb.append(String.format("        frame_type = %d /* %s */ offset_delta=%d abs_pc=%d%n",
                f.frameType, frameType, delta, running));
            if (f.locals != null && f.locals.length > 0) {
                sb.append("          locals: [ ");
                for (int j = 0; j < f.locals.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(vtiName(f.locals[j], pool));
                }
                sb.append(" ]\n");
            }
            if (f.stack != null && f.stack.length > 0) {
                sb.append("          stack: [ ");
                for (int j = 0; j < f.stack.length; j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(vtiName(f.stack[j], pool));
                }
                sb.append(" ]\n");
            }
        }
    }

    public static int frameDelta(StackMapTableAttribute.StackMapFrame f) {
        int ft = f.frameType;
        if (ft >= 0 && ft <= 63) return ft;
        if (ft >= 64 && ft <= 127) return ft - 64;
        return f.offsetDelta;
    }

    private static String frameTypeName(StackMapTableAttribute.StackMapFrame f) {
        int ft = f.frameType;
        if (ft >= 0 && ft <= 63) return "same_frame";
        if (ft >= 64 && ft <= 127) return "same_locals_1_stack_item_frame";
        switch (ft) {
            case 247: return "same_locals_1_stack_item_frame_extended";
            case 248: return "chop_frame(1)";
            case 249: return "chop_frame(2)";
            case 250: return "chop_frame(3)";
            case 251: return "same_frame_extended";
            case 252: return "append_frame(1)";
            case 253: return "append_frame(2)";
            case 254: return "append_frame(3)";
            case 255: return "full_frame";
            default: return "unknown_" + ft;
        }
    }

    private static String vtiName(StackMapTableAttribute.VerificationTypeInfo vti, ConstantPool pool) {
        switch (vti.tag) {
            case 0: return "top";
            case 1: return "integer";
            case 2: return "float";
            case 3: return "double";
            case 4: return "long";
            case 5: return "null";
            case 6: return "uninitialized_this";
            case 7: return "Object " + pool.getClassName(vti.cpoolIndex).replace('/', '.');
            case 8: return "uninitialized @" + vti.offset;
            default: return "?tag=" + vti.tag;
        }
    }
}
