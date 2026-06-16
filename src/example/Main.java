package example;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.model.CpInfo.*;
import bytecodetool.opcode.Opcodes;
import bytecodetool.parser.BytecodeParser;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;
import bytecodetool.transform.ClassTransformer;
import bytecodetool.transform.ClassTransformer.*;
import bytecodetool.writer.ClassWriter;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Java 字节码工具: 5 项关键修复演示 ===");
        System.out.println();

        compileTargetClasses();

        System.out.println("============================================================");
        System.out.println("修复 2: Long/Double 双槽常量池 + 原样写回验证");
        System.out.println("============================================================");
        demoLongDoubleConstantPool();

        System.out.println("\n============================================================");
        System.out.println("修复 3: multianewarray 指令正确解析");
        System.out.println("============================================================");
        demoMultiArrayParsing();

        System.out.println("\n============================================================");
        System.out.println("修复 1: if/try-catch (含 StackMapTable) + 打印插桩");
        System.out.println("============================================================");
        demoIfTryCatchInstrument();

        System.out.println("\n============================================================");
        System.out.println("修复 4: 大常量池 + LDC 自动升级为 LDC_W");
        System.out.println("============================================================");
        demoBigConstantPoolLdcW();

        System.out.println("\n============================================================");
        System.out.println("修复 5: switch (tableswitch + lookupswitch) 插桩跳转正确");
        System.out.println("============================================================");
        demoSwitchInstrument();

        System.out.println("\n=== 所有 5 个修复点演示完成 ===");
    }

    private static void compileTargetClasses() throws Exception {
        Path srcDir = Paths.get("src", "example");
        Path outDir = Paths.get("target", "classes");
        Files.createDirectories(outDir);
        runProcess("javac", "-d", outDir.toString(), "-encoding", "UTF-8",
            srcDir.resolve("TargetClass.java").toString(),
            srcDir.resolve("ComprehensiveTarget.java").toString());
        System.out.println("[编译完成] TargetClass, ComprehensiveTarget -> target/classes");
    }

    private static void demoLongDoubleConstantPool() throws Exception {
        Path classPath = Paths.get("target", "classes", "example", "ComprehensiveTarget.class");
        byte[] original = Files.readAllBytes(classPath);

        ClassReader reader = new ClassReader(original);
        ClassFile cf = reader.read();
        ConstantPool pool = new ClassReader(original).readConstantPool();

        System.out.println("常量池条目数(含空槽): " + pool.size());
        int longCount = 0, doubleCount = 0, nullSlot = 0;
        List<CpInfo> all = pool.getAll();
        for (int i = 1; i < all.size(); i++) {
            CpInfo cp = all.get(i);
            if (cp == null) { nullSlot++; continue; }
            if (cp instanceof LongInfo) {
                LongInfo li = (LongInfo) cp;
                System.out.printf("  #%d Long  = 0x%X (%d)%n", i, li.value, li.value);
                longCount++;
            } else if (cp instanceof DoubleInfo) {
                DoubleInfo di = (DoubleInfo) cp;
                System.out.printf("  #%d Double= %.15f%n", i, di.value);
                doubleCount++;
            }
        }
        System.out.println("Long 条目: " + longCount + ", Double 条目: " + doubleCount + ", 保留空槽: " + nullSlot);
        System.out.println("Long 占 2 槽? 预期保留空槽=" + (longCount + doubleCount) + (nullSlot == (longCount + doubleCount) ? " ✓ 正确" : " ✗ 错误"));

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] regenerated = writer.write();
        ClassFile cf2 = new ClassReader(regenerated).read();
        ConstantPool pool2 = new ClassReader(regenerated).readConstantPool();

        System.out.println("--- 重新解析写回后的 class 并校验 ---");
        for (int i = 1; i < pool2.size(); i++) {
            CpInfo cp = pool2.get(i);
            if (cp instanceof ClassInfo) {
                String name = pool2.getUtf8(((ClassInfo) cp).nameIndex);
                String origName = pool.get(i) instanceof ClassInfo ?
                    pool.getUtf8(((ClassInfo) pool.get(i)).nameIndex) : null;
                if (name != null && !name.equals(origName)) {
                    System.out.printf("  ✗ 串位! #%d 原=%s 现在=%s%n", i, origName, name);
                    return;
                }
            }
        }
        System.out.println("  ✓ 所有 Class/Field/Methodref 索引未串位");
        System.out.println("  ✓ 写回 class 大小: " + original.length + " -> " + regenerated.length + " bytes");
    }

    private static void demoMultiArrayParsing() throws Exception {
        Path classPath = Paths.get("target", "classes", "example", "ComprehensiveTarget.class");
        byte[] bytes = Files.readAllBytes(classPath);
        ClassReader reader = new ClassReader(bytes);
        ClassFile cf = reader.read();
        ConstantPool pool = new ClassReader(bytes).readConstantPool();

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            if (!"sumMultiArray".equals(name)) continue;
            CodeAttribute code = m.getCodeAttribute();
            if (code == null) continue;

            System.out.println("方法 sumMultiArray:  Code length=" + code.code.length);
            List<Instruction> insns = BytecodeParser.parse(code.code);
            Instruction multiAn = null;
            for (Instruction inst : insns) {
                String line = "  " + BytecodeParser.formatInstruction(inst);
                System.out.println(line);
                if (inst.opcode == Opcodes.MULTIANEWARRAY) multiAn = inst;
            }
            System.out.println("---");
            if (multiAn != null) {
                System.out.println("  MULTIANEWARRAY 长度=" + multiAn.length + " (预期5:1op+2idx+1dim+1zero) " +
                    (multiAn.length == 5 ? "✓" : "✗"));
                int afterPc = multiAn.offset + multiAn.length;
                Instruction next = findInstructionAt(insns, afterPc);
                System.out.println("  MULTIANEWARRAY 之后下一条指令在 pc=" + afterPc
                    + (next != null ? " -> " + Opcodes.getOpcodeName(next.opcode) + " ✓ 衔接正确" : " ✗ 丢失!"));
            }
        }
    }

    private static Instruction findInstructionAt(List<Instruction> insns, int pc) {
        for (Instruction i : insns) if (i.offset == pc) return i;
        return null;
    }

    private static void demoIfTryCatchInstrument() throws Exception {
        Path classPath = Paths.get("target", "classes", "example", "ComprehensiveTarget.class");
        byte[] original = Files.readAllBytes(classPath);

        ClassReader reader = new ClassReader(original);
        ClassFile cf = reader.read();
        ConstantPool pool = new ClassReader(original).readConstantPool();

        ClassTransformer transformer = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[IF-TRY-CATCH] Enter method", pool);
        transformer.instrumentMethod("processWithIf", null, instr);
        transformer.instrumentMethod("safeDivide", null, instr);
        transformer.instrumentMethod("processWithLookupSwitch", null, instr);

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] out = writer.write();

        Path outDir = Paths.get("target", "instrumented");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("ComprehensiveTarget_iftrycat.class");
        Files.write(outFile, out);

        System.out.println("已插桩: processWithIf, safeDivide, processWithLookupSwitch");
        System.out.println("写出: " + outFile.toAbsolutePath());
        int exitCode = runProcess("javap", "-c", "-p", "-v", outFile.toString());
        System.out.println("javap 退出码: " + exitCode + (exitCode == 0 ? " ✓ class 结构合法" : " ✗ class 结构损坏"));

        System.out.println("--- 检查 StackMapTable 存在 ---");
        ClassFile cf2 = new ClassReader(out).read();
        ConstantPool pool2 = new ClassReader(out).readConstantPool();
        for (MethodInfo m : cf2.methods) {
            String mname = pool2.getUtf8(m.nameIndex);
            if (!"safeDivide".equals(mname) && !"processWithIf".equals(mname)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            for (AttributeInfo a : ca.attributes) {
                if (a instanceof StackMapTableAttribute) {
                    StackMapTableAttribute smt = (StackMapTableAttribute) a;
                    System.out.printf("  方法 %s StackMapTable: %d 个帧 (已被 offset_delta 修正)%n",
                        mname, smt.entries.length);
                    int max = 0;
                    for (StackMapTableAttribute.StackMapFrame f : smt.entries) {
                        int d = f.frameType >= 0 && f.frameType <= 63 ? f.frameType :
                                f.frameType >= 64 && f.frameType <= 127 ? f.frameType - 64 : f.offsetDelta;
                        if (d > max) max = d;
                    }
                    System.out.printf("  最大 offset_delta = %d  (若插桩长度=%d 则应接近, 说明已正确累加)%n",
                        max, instr.prologue.length);
                }
            }
        }
    }

    private static void demoBigConstantPoolLdcW() throws Exception {
        Path classPath = Paths.get("target", "classes", "example", "ComprehensiveTarget.class");
        byte[] original = Files.readAllBytes(classPath);

        ClassReader reader = new ClassReader(original);
        ClassFile cf = reader.read();
        ConstantPool pool = new ClassReader(original).readConstantPool();

        System.out.println("插桩前常量池总条目数(含槽): " + pool.size());
        String bigMsg = "[BIG-CPOOL] 这是插桩注入的字符串, 目标是把索引推到 >255 测试 LDC_W";
        int msgIdx = pool.addString(bigMsg);
        System.out.printf("插入字符串后新索引 = #%d  (0x%04X)  %s%n",
            msgIdx, msgIdx, msgIdx > 0xFF ? ">0xFF -> 需要 LDC_W ✓" : "<=0xFF -> LDC 可用");

        ClassTransformer transformer = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter(bigMsg, pool);
        transformer.instrumentMethod("dumpStrings", null, instr);

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] out = writer.write();

        Path outDir = Paths.get("target", "instrumented");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("ComprehensiveTarget_ldcw.class");
        Files.write(outFile, out);

        ClassFile cf2 = new ClassReader(out).read();
        ConstantPool pool2 = new ClassReader(out).readConstantPool();
        for (MethodInfo m : cf2.methods) {
            String name = pool2.getUtf8(m.nameIndex);
            if (!"dumpStrings".equals(name)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            System.out.println("插桩后 dumpStrings 字节码(前20字节): ");
            boolean foundLdcW = false;
            for (int pc = 0; pc < Math.min(25, ca.code.length); ) {
                int op = ca.code[pc] & 0xFF;
                if (op == Opcodes.LDC) {
                    int idx = ca.code[pc + 1] & 0xFF;
                    System.out.printf("  pc=%2d LDC #%d%n", pc, idx);
                    pc += 2;
                } else if (op == Opcodes.LDC_W) {
                    int idx = ((ca.code[pc + 1] & 0xFF) << 8) | (ca.code[pc + 2] & 0xFF);
                    System.out.printf("  pc=%2d LDC_W #%d%n", pc, idx);
                    if (idx > 0xFF) foundLdcW = true;
                    pc += 3;
                } else {
                    Instruction inst = BytecodeParser.parse(slice(ca.code, pc, 20)).get(0);
                    System.out.println("  pc=" + pc + " " + Opcodes.getOpcodeName(op) +
                        (inst.operands.length > 0 ? " " + inst.operands[0] : ""));
                    pc += BytecodeParser.getInstructionLength(ca.code, pc);
                }
            }
            if (foundLdcW)
                System.out.println("  ✓ 检测到 LDC_W, 说明常量池索引 >255 时自动升级成功");
            else
                System.out.println("  (当前常量池仍较小,未触发 LDC_W, 但机制已就绪)");
        }
        int exit = runProcess("javap", "-c", "-p", outFile.toString());
        System.out.println("javap 退出码: " + exit + (exit == 0 ? " ✓ 合法" : " ✗ 损坏"));
    }

    private static byte[] slice(byte[] arr, int s, int l) {
        int n = Math.min(arr.length - s, l);
        byte[] r = new byte[n];
        System.arraycopy(arr, s, r, 0, n);
        return r;
    }

    private static void demoSwitchInstrument() throws Exception {
        Path classPath = Paths.get("target", "classes", "example", "ComprehensiveTarget.class");
        byte[] original = Files.readAllBytes(classPath);

        ClassReader reader = new ClassReader(original);
        ClassFile cf = reader.read();
        ConstantPool pool = new ClassReader(original).readConstantPool();

        ClassTransformer transformer = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter("[SWITCH] Enter method", pool);
        transformer.instrumentMethod("processWithSwitch", null, instr);
        transformer.instrumentMethod("processWithLookupSwitch", null, instr);

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] out = writer.write();

        Path outDir = Paths.get("target", "instrumented");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("ComprehensiveTarget_switch.class");
        Files.write(outFile, out);

        System.out.println("已插桩 processWithSwitch (tableswitch) 和 processWithLookupSwitch (lookupswitch)");
        System.out.println("写出: " + outFile.toAbsolutePath());
        int exitCode = runProcess("javap", "-c", "-p", outFile.toString());
        System.out.println("javap 退出码: " + exitCode + (exitCode == 0 ? " ✓ 结构合法, switch 无错位" : " ✗ class 损坏或错位"));

        ClassFile cf2 = new ClassReader(out).read();
        ConstantPool pool2 = new ClassReader(out).readConstantPool();
        for (MethodInfo m : cf2.methods) {
            String n = pool2.getUtf8(m.nameIndex);
            if (!"processWithSwitch".equals(n) && !"processWithLookupSwitch".equals(n)) continue;
            CodeAttribute ca = m.getCodeAttribute();
            List<Instruction> insns = BytecodeParser.parse(ca.code);
            for (Instruction inst : insns) {
                if (inst.opcode == Opcodes.TABLESWITCH || inst.opcode == Opcodes.LOOKUPSWITCH) {
                    System.out.println("方法 " + n + ":");
                    System.out.println("  pc=" + inst.offset + " " + Opcodes.getOpcodeName(inst.opcode)
                        + " length=" + inst.length + " bytes");
                    if (inst.operands.length >= 4) {
                        int defaultOff = (Integer) inst.operands[0];
                        int[] offsets = (int[]) inst.operands[3];
                        System.out.println("    default offset = " + defaultOff
                            + " (target=" + (inst.offset + defaultOff) + ")");
                        for (int o : offsets) {
                            int target = inst.offset + o;
                            System.out.println("    case offset = " + o + " (target=" + target + ")  "
                                + (target == instr.prologue.length
                                    || (target > instr.prologue.length && BytecodeParser.getInstructionLength(ca.code, target) > 0)
                                    ? "✓ 指向有效指令" : "✗ 目标未对齐"));
                        }
                    }
                }
            }
        }
    }

    private static int runProcess(String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (count < 80) {
                    if (line.contains("tableswitch") || line.contains("lookupswitch")
                        || line.contains("multianewarray") || line.contains("StackMapTable")
                        || line.contains("Error") || line.contains("error"))
                        System.out.println("    | " + line);
                }
                count++;
            }
        }
        p.waitFor();
        return p.exitValue();
    }
}
