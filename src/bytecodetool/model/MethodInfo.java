package bytecodetool.model;

public class MethodInfo {
    public int accessFlags;
    public int nameIndex;
    public int descriptorIndex;
    public AttributeInfo[] attributes;

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_PRIVATE = 0x0002;
    public static final int ACC_PROTECTED = 0x0004;
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SYNCHRONIZED = 0x0020;
    public static final int ACC_BRIDGE = 0x0040;
    public static final int ACC_VARARGS = 0x0080;
    public static final int ACC_NATIVE = 0x0100;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_STRICT = 0x0800;
    public static final int ACC_SYNTHETIC = 0x1000;

    public MethodInfo() {
    }

    public MethodInfo(int accessFlags, int nameIndex, int descriptorIndex, AttributeInfo[] attributes) {
        this.accessFlags = accessFlags;
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
        this.attributes = attributes;
    }

    public AttributeInfo.CodeAttribute getCodeAttribute() {
        for (AttributeInfo attr : attributes) {
            if (attr instanceof AttributeInfo.CodeAttribute) {
                return (AttributeInfo.CodeAttribute) attr;
            }
        }
        return null;
    }
}
