package bytecodetool.util;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.CodeAttribute;
import bytecodetool.model.AttributeInfo.StackMapTableAttribute;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;

import java.nio.file.Path;
import java.util.*;

public class DiffTool {

    public static void diff(Path pathA, Path pathB) throws Exception {
        byte[] dataA = java.nio.file.Files.readAllBytes(pathA);
        byte[] dataB = java.nio.file.Files.readAllBytes(pathB);
        ClassFile cfA = new ClassReader(dataA).read();
        ClassFile cfB = new ClassReader(dataB).read();
        ConstantPool poolA = new ClassReader(dataA).readConstantPool();
        ConstantPool poolB = new ClassReader(dataB).readConstantPool();

        System.out.println("====== Class file diff ======");
        System.out.println("  A: " + pathA);
        System.out.println("  B: " + pathB);
        System.out.println();

        diffHeader(cfA, cfB);
        diffConstantPool(poolA, poolB);
        diffMethods(cfA, cfB, poolA, poolB);

        System.out.println("====== End diff ======");
    }

    private static void diffHeader(ClassFile a, ClassFile b) {
        System.out.println("-- Header --");
        if (a.accessFlags != b.accessFlags) System.out.println("  access: 0x" + Integer.toHexString(a.accessFlags) + " -> 0x" + Integer.toHexString(b.accessFlags));
        if (a.majorVersion != b.majorVersion || a.minorVersion != b.minorVersion)
            System.out.println("  version: " + a.majorVersion + "." + a.minorVersion + " -> " + b.majorVersion + "." + b.minorVersion);
        int ifA = a.interfaces == null ? 0 : a.interfaces.length;
        int ifB = b.interfaces == null ? 0 : b.interfaces.length;
        if (ifA != ifB) System.out.println("  interfaces: " + ifA + " -> " + ifB);
        System.out.println();
    }

    private static void diffConstantPool(ConstantPool pa, ConstantPool pb) {
        System.out.println("-- Constant pool --");
        int sza = pa.size();
        int szb = pb.size();
        if (sza != szb) System.out.println("  size: " + sza + " -> " + szb + " (delta " + (szb - sza) + ")");

        Map<String, Integer> aIdx = new HashMap<>();
        for (int i = 1; i < sza; i++) {
            CpInfo cp = pa.get(i);
            if (cp == null) continue;
            String key = cpKey(cp, pa);
            aIdx.put(key, i);
        }

        List<String> added = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < szb; i++) {
            CpInfo cp = pb.get(i);
            if (cp == null) continue;
            String key = cpKey(cp, pb);
            if (!aIdx.containsKey(key) && !seen.contains(key)) {
                seen.add(key);
                added.add("#" + i + " = " + Disassembler.tagName(cp.tag) + "  " + shortDesc(cp, pb));
            }
        }
        if (added.isEmpty()) {
            System.out.println("  (no new entries)");
        } else {
            System.out.println("  added entries (" + added.size() + "):");
            for (String s : added) System.out.println("    " + s);
        }
        System.out.println();
    }

    private static String cpKey(CpInfo cp, ConstantPool pool) {
        if (cp instanceof CpInfo.Utf8Info) return "Utf8:" + ((CpInfo.Utf8Info) cp).value;
        if (cp instanceof CpInfo.IntegerInfo) return "Integer:" + ((CpInfo.IntegerInfo) cp).value;
        if (cp instanceof CpInfo.FloatInfo) return "Float:" + ((CpInfo.FloatInfo) cp).value;
        if (cp instanceof CpInfo.LongInfo) return "Long:" + ((CpInfo.LongInfo) cp).value;
        if (cp instanceof CpInfo.DoubleInfo) return "Double:" + ((CpInfo.DoubleInfo) cp).value;
        if (cp instanceof CpInfo.ClassInfo) return "Class:" + ((CpInfo.ClassInfo) cp).nameIndex;
        if (cp instanceof CpInfo.StringInfo) return "String:" + ((CpInfo.StringInfo) cp).stringIndex;
        if (cp instanceof CpInfo.FieldrefInfo) { CpInfo.FieldrefInfo f = (CpInfo.FieldrefInfo) cp; return "Fieldref:" + f.classIndex + ":" + f.nameAndTypeIndex; }
        if (cp instanceof CpInfo.MethodrefInfo) { CpInfo.MethodrefInfo m = (CpInfo.MethodrefInfo) cp; return "Methodref:" + m.classIndex + ":" + m.nameAndTypeIndex; }
        if (cp instanceof CpInfo.InterfaceMethodrefInfo) { CpInfo.InterfaceMethodrefInfo m = (CpInfo.InterfaceMethodrefInfo) cp; return "IMethodref:" + m.classIndex + ":" + m.nameAndTypeIndex; }
        if (cp instanceof CpInfo.NameAndTypeInfo) { CpInfo.NameAndTypeInfo n = (CpInfo.NameAndTypeInfo) cp; return "NameAndType:" + n.nameIndex + ":" + n.descriptorIndex; }
        if (cp instanceof CpInfo.MethodHandleInfo) { CpInfo.MethodHandleInfo h = (CpInfo.MethodHandleInfo) cp; return "MH:" + h.referenceKind + ":" + h.referenceIndex; }
        if (cp instanceof CpInfo.MethodTypeInfo) return "MT:" + ((CpInfo.MethodTypeInfo) cp).descriptorIndex;
        if (cp instanceof CpInfo.InvokeDynamicInfo) { CpInfo.InvokeDynamicInfo d = (CpInfo.InvokeDynamicInfo) cp; return "ID:" + d.bootstrapMethodAttrIndex + ":" + d.nameAndTypeIndex; }
        return "?" + cp.tag;
    }

    private static String shortDesc(CpInfo cp, ConstantPool pool) {
        try {
            String d = Disassembler.describeCp(cp, pool);
            return d.length() > 80 ? d.substring(0, 77) + "..." : d;
        } catch (Exception e) { return ""; }
    }

    static class MethodView {
        String name;
        String desc;
        int access;
        List<Instruction> insns;
        byte[] code;
        int maxStack, maxLocals;
        CodeAttribute.ExceptionTableEntry[] excTable;
        List<StackMapTableAttribute.StackMapFrame> stackMap;
    }

    private static Map<String, MethodView> collectMethods(ClassFile cf, ConstantPool pool) {
        Map<String, MethodView> map = new LinkedHashMap<>();
        for (MethodInfo m : cf.methods) {
            MethodView mv = new MethodView();
            mv.name = pool.getUtf8(m.nameIndex);
            mv.desc = pool.getUtf8(m.descriptorIndex);
            mv.access = m.accessFlags;
            CodeAttribute ca = m.getCodeAttribute();
            if (ca != null) {
                mv.maxStack = ca.maxStack;
                mv.maxLocals = ca.maxLocals;
                mv.code = ca.code;
                try {
                    mv.insns = BytecodeParser.parse(ca.code);
                } catch (Exception e) { mv.insns = Collections.emptyList(); }
                mv.excTable = ca.exceptionTable;
                if (ca.attributes != null) {
                    for (AttributeInfo a : ca.attributes) {
                        if (a instanceof StackMapTableAttribute) {
                            mv.stackMap = Arrays.asList(((StackMapTableAttribute) a).entries);
                            break;
                        }
                    }
                }
            }
            map.put(mv.name + mv.desc, mv);
        }
        return map;
    }

    private static void diffMethods(ClassFile a, ClassFile b, ConstantPool pa, ConstantPool pb) {
        Map<String, MethodView> ma = collectMethods(a, pa);
        Map<String, MethodView> mb = collectMethods(b, pb);

        System.out.println("-- Methods --");
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(ma.keySet());
        keys.addAll(mb.keySet());
        for (String key : keys) {
            MethodView va = ma.get(key);
            MethodView vb = mb.get(key);
            if (va == null) {
                System.out.println("  + added   " + key);
                continue;
            }
            if (vb == null) {
                System.out.println("  - removed " + key);
                continue;
            }
            if (va.code == null && vb.code == null) continue;
            if (va.code == null || vb.code == null) {
                System.out.println("  ~ changed " + key + " (code presence)");
                continue;
            }
            MethodDiff md = compareBytecode(va, vb);
            if (md.maxStackChanged || md.maxLocalsChanged || md.codeDiffCount > 0
                    || md.excTableChanged || md.stackMapChanged) {
                System.out.println("  ~ changed " + key);
                if (md.maxStackChanged) System.out.println("    stack : " + va.maxStack + " -> " + vb.maxStack);
                if (md.maxLocalsChanged) System.out.println("    locals: " + va.maxLocals + " -> " + vb.maxLocals);
                if (md.codeDiffCount > 0) printCodeDiff(md, va, vb, pa, pb);
                if (md.excTableChanged) printExcDiff(va, vb, pa, pb);
                if (md.stackMapChanged) printStackMapDiff(va, vb);
            }
        }
        System.out.println();
    }

    static class MethodDiff {
        boolean maxStackChanged;
        boolean maxLocalsChanged;
        boolean excTableChanged;
        boolean stackMapChanged;
        int codeDiffCount;
        List<int[]> diffRanges; // [{Astart, Aend, Bstart, Bend}]
        MethodDiff() { diffRanges = new ArrayList<>(); }
    }

    private static MethodDiff compareBytecode(MethodView a, MethodView b) {
        MethodDiff md = new MethodDiff();
        md.maxStackChanged = a.maxStack != b.maxStack;
        md.maxLocalsChanged = a.maxLocals != b.maxLocals;
        md.excTableChanged = !Arrays.deepEquals(
                a.excTable == null ? new Object[0] : a.excTable,
                b.excTable == null ? new Object[0] : b.excTable);
        if (!Objects.equals(a.stackMap, b.stackMap)) md.stackMapChanged = true;

        int n = a.insns.size(), m = b.insns.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (sameInsn(a.insns.get(i), b.insns.get(j))) dp[i][j] = dp[i + 1][j + 1] + 1;
                else dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        List<int[]> ranges = new ArrayList<>();
        int i = 0, j = 0;
        int curA = -1, curB = -1, startA = -1, startB = -1;
        while (i < n || j < m) {
            if (i < n && j < m && sameInsn(a.insns.get(i), b.insns.get(j))) {
                if (startA >= 0) { ranges.add(new int[]{startA, curA + 1, startB, curB + 1}); startA = startB = -1; }
                i++; j++;
            } else {
                if (startA < 0) { startA = i; startB = j; }
                if (i < n && (j >= m || dp[i + 1][j] >= dp[i][j + 1])) { curA = i++; }
                else { curB = j++; }
            }
        }
        if (startA >= 0) ranges.add(new int[]{startA, n, startB, m});
        md.diffRanges = ranges;
        md.codeDiffCount = ranges.size();
        return md;
    }

    private static boolean sameInsn(Instruction x, Instruction y) {
        if (x.opcode != y.opcode) return false;
        if (x.length != y.length) return false;
        return Arrays.equals(x.operands, y.operands);
    }

    private static void printCodeDiff(MethodDiff md, MethodView va, MethodView vb, ConstantPool pa, ConstantPool pb) {
        System.out.println("    bytecode diff (common omitted):");
        for (int[] r : md.diffRanges) {
            int a0 = r[0], a1 = r[1], b0 = r[2], b1 = r[3];
            if (a0 < a1) {
                for (int k = a0; k < a1 && k < va.insns.size(); k++) {
                    Instruction ins = va.insns.get(k);
                    System.out.println("      A[" + String.format("%4d", ins.offset) + "] -" + Disassembler.formatInstruction(ins, pa, va.code).trim());
                }
            }
            if (b0 < b1) {
                for (int k = b0; k < b1 && k < vb.insns.size(); k++) {
                    Instruction ins = vb.insns.get(k);
                    System.out.println("      B[" + String.format("%4d", ins.offset) + "] +" + Disassembler.formatInstruction(ins, pb, vb.code).trim());
                }
            }
            System.out.println("      ---");
        }
    }

    private static void printExcDiff(MethodView va, MethodView vb, ConstantPool pa, ConstantPool pb) {
        System.out.println("    exception table diff:");
        CodeAttribute.ExceptionTableEntry[] ea = va.excTable == null ? new CodeAttribute.ExceptionTableEntry[0] : va.excTable;
        CodeAttribute.ExceptionTableEntry[] eb = vb.excTable == null ? new CodeAttribute.ExceptionTableEntry[0] : vb.excTable;
        if (ea.length != eb.length) System.out.println("      entries: " + ea.length + " -> " + eb.length);
        for (int i = 0; i < Math.max(ea.length, eb.length); i++) {
            String sA = i < ea.length ? formatExc(ea[i], pa) : "--";
            String sB = i < eb.length ? formatExc(eb[i], pb) : "--";
            if (!sA.equals(sB)) System.out.println("      [" + i + "] " + sA + " -> " + sB);
        }
    }

    private static String formatExc(CodeAttribute.ExceptionTableEntry e, ConstantPool pool) {
        String ct = e.catchType == 0 ? "any" : pool.getClassName(e.catchType);
        return "[" + e.startPc + "," + e.endPc + ")->" + e.handlerPc + " " + ct;
    }

    private static void printStackMapDiff(MethodView va, MethodView vb) {
        List<StackMapTableAttribute.StackMapFrame> fa = va.stackMap == null ? Collections.emptyList() : va.stackMap;
        List<StackMapTableAttribute.StackMapFrame> fb = vb.stackMap == null ? Collections.emptyList() : vb.stackMap;
        System.out.println("    StackMapTable diff:");
        System.out.println("      frames: " + fa.size() + " -> " + fb.size());
        for (int i = 0; i < Math.min(fa.size(), fb.size()); i++) {
            StackMapTableAttribute.StackMapFrame a = fa.get(i), b = fb.get(i);
            boolean same = a.frameType == b.frameType && Objects.equals(a.stack, b.stack) && Objects.equals(a.locals, b.locals);
            if (!same) {
                System.out.println("      [" + i + "] type=" + a.frameType + " -> type=" + b.frameType
                        + " (offset_delta: " + Disassembler.frameDelta(a) + " -> " + Disassembler.frameDelta(b) + ")");
            }
        }
        if (fa.size() > fb.size()) {
            for (int i = fb.size(); i < fa.size(); i++) System.out.println("      [" + i + "] -removed frame type=" + fa.get(i).frameType);
        } else if (fb.size() > fa.size()) {
            for (int i = fa.size(); i < fb.size(); i++) System.out.println("      [" + i + "] +added frame type=" + fb.get(i).frameType);
        }
    }
}
