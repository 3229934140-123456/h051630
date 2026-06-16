package bytecodetool.model;

public abstract class CpInfo {
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_NameAndType = 12;
    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_InvokeDynamic = 18;

    public final int tag;

    protected CpInfo(int tag) {
        this.tag = tag;
    }

    public static class ClassInfo extends CpInfo {
        public int nameIndex;
        public ClassInfo(int nameIndex) {
            super(CONSTANT_Class);
            this.nameIndex = nameIndex;
        }
    }

    public static class FieldrefInfo extends CpInfo {
        public int classIndex;
        public int nameAndTypeIndex;
        public FieldrefInfo(int classIndex, int nameAndTypeIndex) {
            super(CONSTANT_Fieldref);
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class MethodrefInfo extends CpInfo {
        public int classIndex;
        public int nameAndTypeIndex;
        public MethodrefInfo(int classIndex, int nameAndTypeIndex) {
            super(CONSTANT_Methodref);
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class InterfaceMethodrefInfo extends CpInfo {
        public int classIndex;
        public int nameAndTypeIndex;
        public InterfaceMethodrefInfo(int classIndex, int nameAndTypeIndex) {
            super(CONSTANT_InterfaceMethodref);
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class StringInfo extends CpInfo {
        public int stringIndex;
        public StringInfo(int stringIndex) {
            super(CONSTANT_String);
            this.stringIndex = stringIndex;
        }
    }

    public static class IntegerInfo extends CpInfo {
        public int value;
        public IntegerInfo(int value) {
            super(CONSTANT_Integer);
            this.value = value;
        }
    }

    public static class FloatInfo extends CpInfo {
        public float value;
        public FloatInfo(float value) {
            super(CONSTANT_Float);
            this.value = value;
        }
    }

    public static class LongInfo extends CpInfo {
        public long value;
        public LongInfo(long value) {
            super(CONSTANT_Long);
            this.value = value;
        }
    }

    public static class DoubleInfo extends CpInfo {
        public double value;
        public DoubleInfo(double value) {
            super(CONSTANT_Double);
            this.value = value;
        }
    }

    public static class NameAndTypeInfo extends CpInfo {
        public int nameIndex;
        public int descriptorIndex;
        public NameAndTypeInfo(int nameIndex, int descriptorIndex) {
            super(CONSTANT_NameAndType);
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static class Utf8Info extends CpInfo {
        public String value;
        public Utf8Info(String value) {
            super(CONSTANT_Utf8);
            this.value = value;
        }
    }

    public static class MethodHandleInfo extends CpInfo {
        public int referenceKind;
        public int referenceIndex;
        public MethodHandleInfo(int referenceKind, int referenceIndex) {
            super(CONSTANT_MethodHandle);
            this.referenceKind = referenceKind;
            this.referenceIndex = referenceIndex;
        }
    }

    public static class MethodTypeInfo extends CpInfo {
        public int descriptorIndex;
        public MethodTypeInfo(int descriptorIndex) {
            super(CONSTANT_MethodType);
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static class InvokeDynamicInfo extends CpInfo {
        public int bootstrapMethodAttrIndex;
        public int nameAndTypeIndex;
        public InvokeDynamicInfo(int bootstrapMethodAttrIndex, int nameAndTypeIndex) {
            super(CONSTANT_InvokeDynamic);
            this.bootstrapMethodAttrIndex = bootstrapMethodAttrIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }
}
