package bytecodetool;

import bytecodetool.model.*;
import bytecodetool.parser.ClassReader;
import bytecodetool.pool.ConstantPool;
import bytecodetool.transform.ClassTransformer;
import bytecodetool.transform.ClassTransformer.PrintInstrumenter;
import bytecodetool.util.Disassembler;
import bytecodetool.writer.ClassWriter;

import java.nio.file.*;

public class BytecodeTool {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String mode = args[0];
        String inputPath = args[1];
        Path input = Paths.get(inputPath);
        if (!Files.exists(input)) {
            System.err.println("Error: file not found: " + inputPath);
            System.exit(1);
        }

        byte[] data = Files.readAllBytes(input);
        ClassReader cr = new ClassReader(data);
        ClassFile cf = cr.read();
        ConstantPool pool = new ClassReader(data).readConstantPool();
        String className = pool.getClassName(cf.thisClass).replace('/', '.');

        switch (mode) {
            case "disasm":
            case "d":
                doDisassemble(className, cf, pool);
                break;

            case "roundtrip":
            case "r":
                doRoundtrip(input, className, cf, pool);
                break;

            case "instrument":
            case "i":
                if (args.length < 3) {
                    System.err.println("Error: instrument mode requires a method name (or '*' for all)");
                    System.err.println("       Usage:  BytecodeTool instrument <class> <methodName> [message]");
                    System.exit(1);
                }
                String methodName = args[2];
                String message = args.length > 3 ? args[3] : ("[Instrument " + methodName + "]");
                doInstrument(input, className, cf, pool, methodName, message);
                break;

            default:
                System.err.println("Unknown mode: " + mode);
                printUsage();
                System.exit(1);
        }
    }

    private static void doDisassemble(String className, ClassFile cf, ConstantPool pool) {
        System.out.println(Disassembler.disassemble(cf, pool));
    }

    private static void doRoundtrip(Path input, String className, ClassFile cf, ConstantPool pool) throws Exception {
        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();
        Path output = input.getParent().resolve(simpleName(className) + ".roundtrip.class");
        Files.write(output, out);
        System.out.println("Roundtrip OK: " + output.toAbsolutePath());
        System.out.println("  input size : " + Files.size(input) + " bytes");
        System.out.println("  output size: " + out.length + " bytes");
    }

    private static void doInstrument(Path input, String className, ClassFile cf, ConstantPool pool,
                                     String methodName, String message) throws Exception {
        ClassTransformer tx = new ClassTransformer(cf, pool);
        PrintInstrumenter instr = new PrintInstrumenter(message, pool);
        int count = 0;
        for (MethodInfo m : cf.methods) {
            String name = pool.getUtf8(m.nameIndex);
            String desc = pool.getUtf8(m.descriptorIndex);
            boolean match = "*".equals(methodName) || methodName.equals(name)
                         || (methodName + desc).equals(name + desc);
            if (!match) continue;
            if (m.getCodeAttribute() == null) continue;
            tx.instrumentMethod(name, null, instr);
            count++;
            System.out.println("  instrumented: " + name + desc);
        }
        ClassWriter cw = new ClassWriter(cf, pool);
        byte[] out = cw.write();
        Path output = input.getParent().resolve(simpleName(className) + ".instrumented.class");
        Files.write(output, out);
        System.out.println("Instrument OK: " + output.toAbsolutePath() + " (" + count + " methods)");
        System.out.println("  input size : " + Files.size(input) + " bytes");
        System.out.println("  output size: " + out.length + " bytes");
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot >= 0 ? fqn.substring(dot + 1) : fqn;
    }

    private static void printUsage() {
        System.out.println("BytecodeTool - a minimal Java class file tool");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  BytecodeTool disasm       <classfile>            disassemble like javap");
        System.out.println("  BytecodeTool roundtrip    <classfile>            read and write back");
        System.out.println("  BytecodeTool instrument   <classfile> <method>   add entry println to <method>");
        System.out.println("                            (use '*' as method to instrument all concrete methods)");
    }
}
