package bytecodetool.model;

public abstract class AttributeInfo {
    public static final String CODE = "Code";
    public static final String CONSTANT_VALUE = "ConstantValue";
    public static final String EXCEPTIONS = "Exceptions";
    public static final String INNER_CLASSES = "InnerClasses";
    public static final String LINE_NUMBER_TABLE = "LineNumberTable";
    public static final String LOCAL_VARIABLE_TABLE = "LocalVariableTable";
    public static final String SOURCE_FILE = "SourceFile";
    public static final String STACK_MAP_TABLE = "StackMapTable";
    public static final String SIGNATURE = "Signature";
    public static final String DEPRECATED = "Deprecated";
    public static final String SYNTHETIC = "Synthetic";

    public int attributeNameIndex;
    public String attributeName;

    protected AttributeInfo(int attributeNameIndex, String attributeName) {
        this.attributeNameIndex = attributeNameIndex;
        this.attributeName = attributeName;
    }

    public static class CodeAttribute extends AttributeInfo {
        public int maxStack;
        public int maxLocals;
        public byte[] code;
        public ExceptionTableEntry[] exceptionTable;
        public AttributeInfo[] attributes;

        public CodeAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }

        public static class ExceptionTableEntry {
            public int startPc;
            public int endPc;
            public int handlerPc;
            public int catchType;

            public ExceptionTableEntry(int startPc, int endPc, int handlerPc, int catchType) {
                this.startPc = startPc;
                this.endPc = endPc;
                this.handlerPc = handlerPc;
                this.catchType = catchType;
            }
        }
    }

    public static class ConstantValueAttribute extends AttributeInfo {
        public int constantValueIndex;

        public ConstantValueAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }
    }

    public static class SourceFileAttribute extends AttributeInfo {
        public int sourceFileIndex;

        public SourceFileAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }
    }

    public static class ExceptionsAttribute extends AttributeInfo {
        public int[] exceptionIndexTable;

        public ExceptionsAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }
    }

    public static class LineNumberTableAttribute extends AttributeInfo {
        public LineNumberEntry[] lineNumberTable;

        public LineNumberTableAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }

        public static class LineNumberEntry {
            public int startPc;
            public int lineNumber;

            public LineNumberEntry(int startPc, int lineNumber) {
                this.startPc = startPc;
                this.lineNumber = lineNumber;
            }
        }
    }

    public static class LocalVariableTableAttribute extends AttributeInfo {
        public LocalVariableEntry[] localVariableTable;

        public LocalVariableTableAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }

        public static class LocalVariableEntry {
            public int startPc;
            public int length;
            public int nameIndex;
            public int descriptorIndex;
            public int index;

            public LocalVariableEntry(int startPc, int length, int nameIndex, int descriptorIndex, int index) {
                this.startPc = startPc;
                this.length = length;
                this.nameIndex = nameIndex;
                this.descriptorIndex = descriptorIndex;
                this.index = index;
            }
        }
    }

    public static class StackMapTableAttribute extends AttributeInfo {
        public StackMapFrame[] entries;

        public StackMapTableAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }

        public static class StackMapFrame {
            public int frameType;
            public int offsetDelta;
            public VerificationTypeInfo[] locals;
            public VerificationTypeInfo[] stack;

            public StackMapFrame(int frameType) {
                this.frameType = frameType;
            }
        }

        public static class VerificationTypeInfo {
            public static final int ITEM_Top = 0;
            public static final int ITEM_Integer = 1;
            public static final int ITEM_Float = 2;
            public static final int ITEM_Double = 3;
            public static final int ITEM_Long = 4;
            public static final int ITEM_Null = 5;
            public static final int ITEM_UninitializedThis = 6;
            public static final int ITEM_Object = 7;
            public static final int ITEM_Uninitialized = 8;

            public int tag;
            public int cpoolIndex;
            public int offset;

            public VerificationTypeInfo(int tag) {
                this.tag = tag;
            }
        }
    }

    public static class SignatureAttribute extends AttributeInfo {
        public int signatureIndex;

        public SignatureAttribute(int attributeNameIndex, String attributeName) {
            super(attributeNameIndex, attributeName);
        }
    }

    public static class RawAttribute extends AttributeInfo {
        public byte[] info;

        public RawAttribute(int attributeNameIndex, String attributeName, byte[] info) {
            super(attributeNameIndex, attributeName);
            this.info = info;
        }
    }
}
