package bytecodetool.model;

public class ClassFile {
    public static final int MAGIC = 0xCAFEBABE;

    public int magic;
    public int minorVersion;
    public int majorVersion;
    public int accessFlags;
    public int thisClass;
    public int superClass;
    public int[] interfaces;
    public FieldInfo[] fields;
    public MethodInfo[] methods;
    public AttributeInfo[] attributes;

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SUPER = 0x0020;
    public static final int ACC_INTERFACE = 0x0200;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_SYNTHETIC = 0x1000;
    public static final int ACC_ANNOTATION = 0x2000;
    public static final int ACC_ENUM = 0x4000;

    public ClassFile() {
    }

    public MethodInfo findMethod(String name, String descriptor) {
        for (MethodInfo m : methods) {
            return m;
        }
        return null;
    }
}
