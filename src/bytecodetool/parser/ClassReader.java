package bytecodetool.parser;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.model.CpInfo.*;
import bytecodetool.pool.ConstantPool;

import java.io.*;

public class ClassReader {
    private final DataInputStream in;

    public ClassReader(InputStream is) {
        this.in = new DataInputStream(new BufferedInputStream(is));
    }

    public ClassReader(byte[] bytes) {
        this.in = new DataInputStream(new ByteArrayInputStream(bytes));
    }

    public ClassFile read() throws IOException {
        ClassFile cf = new ClassFile();
        cf.magic = in.readInt();
        if (cf.magic != ClassFile.MAGIC) {
            throw new IOException("Invalid magic number: 0x" + Integer.toHexString(cf.magic));
        }
        cf.minorVersion = in.readUnsignedShort();
        cf.majorVersion = in.readUnsignedShort();

        int constantPoolCount = in.readUnsignedShort();
        CpInfo[] constantPool = new CpInfo[constantPoolCount];
        for (int i = 1; i < constantPoolCount; i++) {
            constantPool[i] = readCpInfo();
            if (constantPool[i] instanceof LongInfo || constantPool[i] instanceof DoubleInfo) {
                i++;
            }
        }
        ConstantPool pool = new ConstantPool(constantPool);

        cf.accessFlags = in.readUnsignedShort();
        cf.thisClass = in.readUnsignedShort();
        cf.superClass = in.readUnsignedShort();

        int interfacesCount = in.readUnsignedShort();
        cf.interfaces = new int[interfacesCount];
        for (int i = 0; i < interfacesCount; i++) {
            cf.interfaces[i] = in.readUnsignedShort();
        }

        int fieldsCount = in.readUnsignedShort();
        cf.fields = new FieldInfo[fieldsCount];
        for (int i = 0; i < fieldsCount; i++) {
            cf.fields[i] = readFieldInfo(pool);
        }

        int methodsCount = in.readUnsignedShort();
        cf.methods = new MethodInfo[methodsCount];
        for (int i = 0; i < methodsCount; i++) {
            cf.methods[i] = readMethodInfo(pool);
        }

        int attributesCount = in.readUnsignedShort();
        cf.attributes = new AttributeInfo[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            cf.attributes[i] = readAttributeInfo(pool);
        }

        return cf;
    }

    public ConstantPool readConstantPool() throws IOException {
        if (in.readInt() != ClassFile.MAGIC) {
            throw new IOException("Invalid magic number");
        }
        in.readUnsignedShort();
        in.readUnsignedShort();
        int constantPoolCount = in.readUnsignedShort();
        CpInfo[] constantPool = new CpInfo[constantPoolCount];
        for (int i = 1; i < constantPoolCount; i++) {
            constantPool[i] = readCpInfo();
            if (constantPool[i] instanceof LongInfo || constantPool[i] instanceof DoubleInfo) {
                i++;
            }
        }
        return new ConstantPool(constantPool);
    }

    private CpInfo readCpInfo() throws IOException {
        int tag = in.readUnsignedByte();
        switch (tag) {
            case CpInfo.CONSTANT_Class:
                return new ClassInfo(in.readUnsignedShort());
            case CpInfo.CONSTANT_Fieldref:
                return new FieldrefInfo(in.readUnsignedShort(), in.readUnsignedShort());
            case CpInfo.CONSTANT_Methodref:
                return new MethodrefInfo(in.readUnsignedShort(), in.readUnsignedShort());
            case CpInfo.CONSTANT_InterfaceMethodref:
                return new InterfaceMethodrefInfo(in.readUnsignedShort(), in.readUnsignedShort());
            case CpInfo.CONSTANT_String:
                return new StringInfo(in.readUnsignedShort());
            case CpInfo.CONSTANT_Integer:
                return new IntegerInfo(in.readInt());
            case CpInfo.CONSTANT_Float:
                return new FloatInfo(in.readFloat());
            case CpInfo.CONSTANT_Long:
                return new LongInfo(in.readLong());
            case CpInfo.CONSTANT_Double:
                return new DoubleInfo(in.readDouble());
            case CpInfo.CONSTANT_NameAndType:
                return new NameAndTypeInfo(in.readUnsignedShort(), in.readUnsignedShort());
            case CpInfo.CONSTANT_Utf8:
                return new Utf8Info(in.readUTF());
            case CpInfo.CONSTANT_MethodHandle:
                return new MethodHandleInfo(in.readUnsignedByte(), in.readUnsignedShort());
            case CpInfo.CONSTANT_MethodType:
                return new MethodTypeInfo(in.readUnsignedShort());
            case CpInfo.CONSTANT_InvokeDynamic:
                return new InvokeDynamicInfo(in.readUnsignedShort(), in.readUnsignedShort());
            default:
                throw new IOException("Unknown constant pool tag: " + tag);
        }
    }

    private FieldInfo readFieldInfo(ConstantPool pool) throws IOException {
        int accessFlags = in.readUnsignedShort();
        int nameIndex = in.readUnsignedShort();
        int descriptorIndex = in.readUnsignedShort();
        int attributesCount = in.readUnsignedShort();
        AttributeInfo[] attributes = new AttributeInfo[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = readAttributeInfo(pool);
        }
        return new FieldInfo(accessFlags, nameIndex, descriptorIndex, attributes);
    }

    private MethodInfo readMethodInfo(ConstantPool pool) throws IOException {
        int accessFlags = in.readUnsignedShort();
        int nameIndex = in.readUnsignedShort();
        int descriptorIndex = in.readUnsignedShort();
        int attributesCount = in.readUnsignedShort();
        AttributeInfo[] attributes = new AttributeInfo[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = readAttributeInfo(pool);
        }
        return new MethodInfo(accessFlags, nameIndex, descriptorIndex, attributes);
    }

    private AttributeInfo readAttributeInfo(ConstantPool pool) throws IOException {
        int attributeNameIndex = in.readUnsignedShort();
        int attributeLength = in.readInt();
        String attributeName = pool.getUtf8(attributeNameIndex);

        if (attributeName == null) {
            byte[] info = new byte[attributeLength];
            in.readFully(info);
            return new RawAttribute(attributeNameIndex, "unknown", info);
        }

        switch (attributeName) {
            case AttributeInfo.CODE:
                return readCodeAttribute(attributeNameIndex, attributeName, pool);
            case AttributeInfo.CONSTANT_VALUE:
                ConstantValueAttribute cv = new ConstantValueAttribute(attributeNameIndex, attributeName);
                cv.constantValueIndex = in.readUnsignedShort();
                return cv;
            case AttributeInfo.SOURCE_FILE:
                SourceFileAttribute sf = new SourceFileAttribute(attributeNameIndex, attributeName);
                sf.sourceFileIndex = in.readUnsignedShort();
                return sf;
            case AttributeInfo.EXCEPTIONS:
                ExceptionsAttribute ex = new ExceptionsAttribute(attributeNameIndex, attributeName);
                int numExceptions = in.readUnsignedShort();
                ex.exceptionIndexTable = new int[numExceptions];
                for (int i = 0; i < numExceptions; i++) {
                    ex.exceptionIndexTable[i] = in.readUnsignedShort();
                }
                return ex;
            case AttributeInfo.LINE_NUMBER_TABLE:
                LineNumberTableAttribute ln = new LineNumberTableAttribute(attributeNameIndex, attributeName);
                int lnLength = in.readUnsignedShort();
                ln.lineNumberTable = new LineNumberTableAttribute.LineNumberEntry[lnLength];
                for (int i = 0; i < lnLength; i++) {
                    ln.lineNumberTable[i] = new LineNumberTableAttribute.LineNumberEntry(
                        in.readUnsignedShort(), in.readUnsignedShort()
                    );
                }
                return ln;
            case AttributeInfo.LOCAL_VARIABLE_TABLE:
                LocalVariableTableAttribute lv = new LocalVariableTableAttribute(attributeNameIndex, attributeName);
                int lvLength = in.readUnsignedShort();
                lv.localVariableTable = new LocalVariableTableAttribute.LocalVariableEntry[lvLength];
                for (int i = 0; i < lvLength; i++) {
                    lv.localVariableTable[i] = new LocalVariableTableAttribute.LocalVariableEntry(
                        in.readUnsignedShort(), in.readUnsignedShort(),
                        in.readUnsignedShort(), in.readUnsignedShort(),
                        in.readUnsignedShort()
                    );
                }
                return lv;
            case AttributeInfo.SIGNATURE:
                SignatureAttribute sig = new SignatureAttribute(attributeNameIndex, attributeName);
                sig.signatureIndex = in.readUnsignedShort();
                return sig;
            case AttributeInfo.STACK_MAP_TABLE:
                return readStackMapTableAttribute(attributeNameIndex, attributeName);
            default:
                byte[] info = new byte[attributeLength];
                in.readFully(info);
                return new RawAttribute(attributeNameIndex, attributeName, info);
        }
    }

    private CodeAttribute readCodeAttribute(int attributeNameIndex, String attributeName, ConstantPool pool) throws IOException {
        CodeAttribute code = new CodeAttribute(attributeNameIndex, attributeName);
        code.maxStack = in.readUnsignedShort();
        code.maxLocals = in.readUnsignedShort();
        int codeLength = in.readInt();
        code.code = new byte[codeLength];
        in.readFully(code.code);

        int exceptionTableLength = in.readUnsignedShort();
        code.exceptionTable = new CodeAttribute.ExceptionTableEntry[exceptionTableLength];
        for (int i = 0; i < exceptionTableLength; i++) {
            code.exceptionTable[i] = new CodeAttribute.ExceptionTableEntry(
                in.readUnsignedShort(), in.readUnsignedShort(),
                in.readUnsignedShort(), in.readUnsignedShort()
            );
        }

        int attributesCount = in.readUnsignedShort();
        code.attributes = new AttributeInfo[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            code.attributes[i] = readAttributeInfo(pool);
        }
        return code;
    }

    private StackMapTableAttribute readStackMapTableAttribute(int attributeNameIndex, String attributeName) throws IOException {
        StackMapTableAttribute smt = new StackMapTableAttribute(attributeNameIndex, attributeName);
        int numEntries = in.readUnsignedShort();
        smt.entries = new StackMapTableAttribute.StackMapFrame[numEntries];
        for (int i = 0; i < numEntries; i++) {
            int frameType = in.readUnsignedByte();
            StackMapTableAttribute.StackMapFrame frame = new StackMapTableAttribute.StackMapFrame(frameType);

            if (frameType >= 0 && frameType <= 63) {
                frame.offsetDelta = frameType;
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[0];
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[0];
            } else if (frameType >= 64 && frameType <= 127) {
                frame.offsetDelta = frameType - 64;
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[0];
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[]{readVerificationTypeInfo()};
            } else if (frameType == 247) {
                frame.offsetDelta = in.readUnsignedShort();
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[0];
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[]{readVerificationTypeInfo()};
            } else if (frameType >= 248 && frameType <= 250) {
                frame.offsetDelta = in.readUnsignedShort();
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[0];
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[0];
            } else if (frameType == 251) {
                frame.offsetDelta = in.readUnsignedShort();
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[0];
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[0];
            } else if (frameType >= 252 && frameType <= 254) {
                frame.offsetDelta = in.readUnsignedShort();
                int numLocals = frameType - 251;
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[numLocals];
                for (int j = 0; j < numLocals; j++) {
                    frame.locals[j] = readVerificationTypeInfo();
                }
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[0];
            } else if (frameType == 255) {
                frame.offsetDelta = in.readUnsignedShort();
                int numLocals = in.readUnsignedShort();
                frame.locals = new StackMapTableAttribute.VerificationTypeInfo[numLocals];
                for (int j = 0; j < numLocals; j++) {
                    frame.locals[j] = readVerificationTypeInfo();
                }
                int numStack = in.readUnsignedShort();
                frame.stack = new StackMapTableAttribute.VerificationTypeInfo[numStack];
                for (int j = 0; j < numStack; j++) {
                    frame.stack[j] = readVerificationTypeInfo();
                }
            }
            smt.entries[i] = frame;
        }
        return smt;
    }

    private StackMapTableAttribute.VerificationTypeInfo readVerificationTypeInfo() throws IOException {
        int tag = in.readUnsignedByte();
        StackMapTableAttribute.VerificationTypeInfo vti = new StackMapTableAttribute.VerificationTypeInfo(tag);
        if (tag == StackMapTableAttribute.VerificationTypeInfo.ITEM_Object) {
            vti.cpoolIndex = in.readUnsignedShort();
        } else if (tag == StackMapTableAttribute.VerificationTypeInfo.ITEM_Uninitialized) {
            vti.offset = in.readUnsignedShort();
        }
        return vti;
    }
}
