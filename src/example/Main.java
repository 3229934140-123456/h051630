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
        System.out.println("=== Java 字节码工具演示 ===");
        System.out.println();

        compileTargetClass();

        Path classPath = Paths.get("target", "classes", "example", "TargetClass.class");
        byte[] originalBytes = Files.readAllBytes(classPath);

        System.out.println("=== 1. Class 文件解析 ===");
        demoClassParsing(originalBytes);

        System.out.println("\n=== 2. 常量池分析 ===");
        demoConstantPool(originalBytes);

        System.out.println("\n=== 3. 字节码指令解析 ===");
        demoBytecodeParsing(originalBytes);

        System.out.println("\n=== 4. 方法插桩（Print Instrumentation） ===");
        byte[] printInstrumented = demoPrintInstrumentation(originalBytes);

        System.out.println("\n=== 5. 方法插桩（Timing Instrumentation） ===");
        byte[] timingInstrumented = demoTimingInstrumentation(originalBytes);

        System.out.println("\n=== 6. Class 文件写回 ===");
        demoWriteClass(printInstrumented, timingInstrumented);

        System.out.println("\n=== 演示完成 ===");
    }

    private static void compileTargetClass() throws Exception {
        Path srcDir = Paths.get("src", "example");
        Path outDir = Paths.get("target", "classes");
        Files.createDirectories(outDir);

        ProcessBuilder pb = new ProcessBuilder(
            "javac", "-d", outDir.toString(),
            srcDir.resolve("TargetClass.java").toString()
        );
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
    }

    private static void demoClassParsing(byte[] bytes) throws IOException {
        ClassReader reader = new ClassReader(bytes);
        ClassFile cf = reader.read();

        System.out.println("Magic: 0x" + Integer.toHexString(cf.magic));
        System.out.println("Version: " + cf.majorVersion + "." + cf.minorVersion);
        System.out.println("Access Flags: 0x" + Integer.toHexString(cf.accessFlags));

        ConstantPool pool = readerToPool(bytes);
        System.out.println("This Class: " + pool.getClassName(cf.thisClass));
        System.out.println("Super Class: " + pool.getClassName(cf.superClass));
        System.out.println("Interfaces: " + cf.interfaces.length);
        System.out.println("Fields: " + cf.fields.length);
        System.out.println("Methods: " + cf.methods.length);
        System.out.println("Attributes: " + cf.attributes.length);

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            String desc = pool.getUtf8(m.descriptorIndex);
            if (name == null) name = "<unknown>";
            if (desc == null) desc = "<unknown>";
            System.out.println("  Method: " + name + desc + " flags=0x" + Integer.toHexString(m.accessFlags));
        }
    }

    private static void demoConstantPool(byte[] bytes) throws IOException {
        ConstantPool pool = readerToPool(bytes);
        List<CpInfo> constants = pool.getAll();

        System.out.println("常量池大小: " + (constants.size() - 1));
        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp == null) {
                System.out.println("  #" + i + " (reserved for long/double)");
                continue;
            }
            String desc = describeCp(cp, pool);
            System.out.println("  #" + i + " " + getCpTagName(cp.tag) + " " + desc);
        }
    }

    private static void demoBytecodeParsing(byte[] bytes) throws IOException {
        ClassReader reader = new ClassReader(bytes);
        ClassFile cf = reader.read();
        ConstantPool pool = readerToPool(bytes);

        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            CodeAttribute code = m.getCodeAttribute();
            if (code == null) continue;

            System.out.println("方法 " + name + ":");
            System.out.println("  Max Stack: " + code.maxStack);
            System.out.println("  Max Locals: " + code.maxLocals);
            System.out.println("  Code Length: " + code.code.length);

            List<Instruction> instructions = BytecodeParser.parse(code.code);
            for (Instruction inst : instructions) {
                System.out.println("    " + BytecodeParser.formatInstruction(inst));
            }
            System.out.println("  Computed Max Stack: " + BytecodeParser.computeMaxStack(instructions));
        }
    }

    private static byte[] demoPrintInstrumentation(byte[] bytes) throws Exception {
        ClassReader reader = new ClassReader(bytes);
        ClassFile cf = reader.read();
        ConstantPool pool = readerToPool(bytes);

        ClassTransformer transformer = new ClassTransformer(cf, pool);
        PrintInstrumenter instrumenter = new PrintInstrumenter(
            "[Instrumented] Method called!", pool);
        transformer.instrumentAllMethods(instrumenter);

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] result = writer.write();

        System.out.println("原始大小: " + bytes.length + " bytes");
        System.out.println("变换后大小: " + result.length + " bytes");
        System.out.println("已在所有方法入口插入打印语句");

        return result;
    }

    private static byte[] demoTimingInstrumentation(byte[] bytes) throws Exception {
        ClassReader reader = new ClassReader(bytes);
        ClassFile cf = reader.read();
        ConstantPool pool = readerToPool(bytes);

        ClassTransformer transformer = new ClassTransformer(cf, pool);
        String className = pool.getClassName(cf.thisClass);
        TimingInstrumenter instrumenter = new TimingInstrumenter(pool, className, "totalTime");
        transformer.instrumentMethod("add", null, instrumenter);
        transformer.instrumentMethod("greet", null, instrumenter);

        ClassWriter writer = new ClassWriter(cf, pool);
        byte[] result = writer.write();

        System.out.println("原始大小: " + bytes.length + " bytes");
        System.out.println("变换后大小: " + result.length + " bytes");
        System.out.println("已在 add/greet 方法插入耗时统计");

        return result;
    }

    private static void demoWriteClass(byte[] printInstrumented, byte[] timingInstrumented) throws Exception {
        Path outDir = Paths.get("target", "instrumented");
        Files.createDirectories(outDir);

        Files.write(outDir.resolve("TargetClass_print.class"), printInstrumented);
        Files.write(outDir.resolve("TargetClass_timing.class"), timingInstrumented);

        System.out.println("已写出到: " + outDir.toAbsolutePath());

        verifyClassValidity(outDir.resolve("TargetClass_print.class").toString());
    }

    private static void verifyClassValidity(String classFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-p", classFile);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            System.out.println("--- javap 输出 (验证 class 合法性) ---");
            int count = 0;
            while ((line = br.readLine()) != null && count < 50) {
                System.out.println("  " + line);
                count++;
            }
        }
        p.waitFor();
        System.out.println("  javap 退出码: " + p.exitValue() + " (0 表示合法)");
    }

    private static ConstantPool readerToPool(byte[] bytes) throws IOException {
        return new ClassReader(bytes).readConstantPool();
    }

    private static String getCpTagName(int tag) {
        switch (tag) {
            case CpInfo.CONSTANT_Class: return "Class";
            case CpInfo.CONSTANT_Fieldref: return "Fieldref";
            case CpInfo.CONSTANT_Methodref: return "Methodref";
            case CpInfo.CONSTANT_InterfaceMethodref: return "InterfaceMethodref";
            case CpInfo.CONSTANT_String: return "String";
            case CpInfo.CONSTANT_Integer: return "Integer";
            case CpInfo.CONSTANT_Float: return "Float";
            case CpInfo.CONSTANT_Long: return "Long";
            case CpInfo.CONSTANT_Double: return "Double";
            case CpInfo.CONSTANT_NameAndType: return "NameAndType";
            case CpInfo.CONSTANT_Utf8: return "Utf8";
            case CpInfo.CONSTANT_MethodHandle: return "MethodHandle";
            case CpInfo.CONSTANT_MethodType: return "MethodType";
            case CpInfo.CONSTANT_InvokeDynamic: return "InvokeDynamic";
            default: return "Unknown(" + tag + ")";
        }
    }

    private static String describeCp(CpInfo cp, ConstantPool pool) {
        try {
            switch (cp.tag) {
                case CpInfo.CONSTANT_Class:
                    return "#" + ((ClassInfo) cp).nameIndex + " // " + pool.getUtf8(((ClassInfo) cp).nameIndex);
                case CpInfo.CONSTANT_Fieldref:
                case CpInfo.CONSTANT_Methodref:
                case CpInfo.CONSTANT_InterfaceMethodref: {
                    int ci, nai;
                    if (cp instanceof FieldrefInfo) { ci = ((FieldrefInfo) cp).classIndex; nai = ((FieldrefInfo) cp).nameAndTypeIndex; }
                    else if (cp instanceof MethodrefInfo) { ci = ((MethodrefInfo) cp).classIndex; nai = ((MethodrefInfo) cp).nameAndTypeIndex; }
                    else { ci = ((InterfaceMethodrefInfo) cp).classIndex; nai = ((InterfaceMethodrefInfo) cp).nameAndTypeIndex; }
                    NameAndTypeInfo nat = (NameAndTypeInfo) pool.get(nai);
                    String cname = pool.getClassName(ci);
                    String natName = pool.getUtf8(nat.nameIndex);
                    String natDesc = pool.getUtf8(nat.descriptorIndex);
                    return "#" + ci + ".#" + nai + " // " + (cname != null ? cname + "." + natName + ":" + natDesc : "");
                }
                case CpInfo.CONSTANT_String:
                    return "#" + ((StringInfo) cp).stringIndex + " // " + pool.getUtf8(((StringInfo) cp).stringIndex);
                case CpInfo.CONSTANT_Integer:
                    return String.valueOf(((IntegerInfo) cp).value);
                case CpInfo.CONSTANT_Float:
                    return String.valueOf(((FloatInfo) cp).value);
                case CpInfo.CONSTANT_Long:
                    return String.valueOf(((LongInfo) cp).value);
                case CpInfo.CONSTANT_Double:
                    return String.valueOf(((DoubleInfo) cp).value);
                case CpInfo.CONSTANT_NameAndType: {
                    NameAndTypeInfo nat = (NameAndTypeInfo) cp;
                    return "#" + nat.nameIndex + ":#" + nat.descriptorIndex + " // " + pool.getUtf8(nat.nameIndex) + ":" + pool.getUtf8(nat.descriptorIndex);
                }
                case CpInfo.CONSTANT_Utf8:
                    return ((Utf8Info) cp).value;
            }
        } catch (Exception e) {}
        return "";
    }
}
