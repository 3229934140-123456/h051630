package bytecodetool.writer;

import bytecodetool.model.*;
import bytecodetool.model.AttributeInfo.*;
import bytecodetool.model.CpInfo.*;
import bytecodetool.pool.ConstantPool;

import java.io.*;
import java.util.List;

public class ClassWriter {
    private final ClassFile classFile;
    private final ConstantPool constantPool;
    private final ByteArrayOutputStream baos;
    private final DataOutputStream out;

    public ClassWriter(ClassFile classFile, ConstantPool constantPool) {
        this.classFile = classFile;
        this.constantPool = constantPool;
        this.baos = new ByteArrayOutputStream();
        this.out = new DataOutputStream(baos);
    }

    public byte[] write() throws IOException {
        writeMagic();
        writeVersion();
        writeConstantPool();
        writeAccessFlags();
        writeThisClass();
        writeSuperClass();
        writeInterfaces();
        writeFields();
        writeMethods();
        writeAttributes(classFile.attributes);
        out.flush();
        return baos.toByteArray();
    }

    public void writeTo(File file) throws IOException {
        byte[] data = write();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private void writeMagic() throws IOException {
        out.writeInt(ClassFile.MAGIC);
    }

    private void writeVersion() throws IOException {
        out.writeShort(classFile.minorVersion);
        out.writeShort(classFile.majorVersion);
    }

    private void writeConstantPool() throws IOException {
        List<CpInfo> constants = constantPool.getAll();
        int count = constants.size();
        out.writeShort(count);
        for (int i = 1; i < count; i++) {
            CpInfo cp = constants.get(i);
            if (cp == null) continue;
            writeCpInfo(cp);
        }
    }

    private void writeCpInfo(CpInfo cp) throws IOException {
        out.writeByte(cp.tag);
        switch (cp.tag) {
            case CpInfo.CONSTANT_Class:
                out.writeShort(((ClassInfo) cp).nameIndex);
                break;
            case CpInfo.CONSTANT_Fieldref:
                out.writeShort(((FieldrefInfo) cp).classIndex);
                out.writeShort(((FieldrefInfo) cp).nameAndTypeIndex);
                break;
            case CpInfo.CONSTANT_Methodref:
                out.writeShort(((MethodrefInfo) cp).classIndex);
                out.writeShort(((MethodrefInfo) cp).nameAndTypeIndex);
                break;
            case CpInfo.CONSTANT_InterfaceMethodref:
                out.writeShort(((InterfaceMethodrefInfo) cp).classIndex);
                out.writeShort(((InterfaceMethodrefInfo) cp).nameAndTypeIndex);
                break;
            case CpInfo.CONSTANT_String:
                out.writeShort(((StringInfo) cp).stringIndex);
                break;
            case CpInfo.CONSTANT_Integer:
                out.writeInt(((IntegerInfo) cp).value);
                break;
            case CpInfo.CONSTANT_Float:
                out.writeFloat(((FloatInfo) cp).value);
                break;
            case CpInfo.CONSTANT_Long:
                out.writeLong(((LongInfo) cp).value);
                break;
            case CpInfo.CONSTANT_Double:
                out.writeDouble(((DoubleInfo) cp).value);
                break;
            case CpInfo.CONSTANT_NameAndType:
                out.writeShort(((NameAndTypeInfo) cp).nameIndex);
                out.writeShort(((NameAndTypeInfo) cp).descriptorIndex);
                break;
            case CpInfo.CONSTANT_Utf8:
                out.writeUTF(((Utf8Info) cp).value);
                break;
            case CpInfo.CONSTANT_MethodHandle:
                out.writeByte(((MethodHandleInfo) cp).referenceKind);
                out.writeShort(((MethodHandleInfo) cp).referenceIndex);
                break;
            case CpInfo.CONSTANT_MethodType:
                out.writeShort(((MethodTypeInfo) cp).descriptorIndex);
                break;
            case CpInfo.CONSTANT_InvokeDynamic:
                out.writeShort(((InvokeDynamicInfo) cp).bootstrapMethodAttrIndex);
                out.writeShort(((InvokeDynamicInfo) cp).nameAndTypeIndex);
                break;
        }
    }

    private void writeAccessFlags() throws IOException {
        out.writeShort(classFile.accessFlags);
    }

    private void writeThisClass() throws IOException {
        out.writeShort(classFile.thisClass);
    }

    private void writeSuperClass() throws IOException {
        out.writeShort(classFile.superClass);
    }

    private void writeInterfaces() throws IOException {
        out.writeShort(classFile.interfaces.length);
        for (int iface : classFile.interfaces) {
            out.writeShort(iface);
        }
    }

    private void writeFields() throws IOException {
        out.writeShort(classFile.fields.length);
        for (FieldInfo field : classFile.fields) {
            writeFieldInfo(field);
        }
    }

    private void writeFieldInfo(FieldInfo field) throws IOException {
        out.writeShort(field.accessFlags);
        out.writeShort(field.nameIndex);
        out.writeShort(field.descriptorIndex);
        writeAttributes(field.attributes);
    }

    private void writeMethods() throws IOException {
        out.writeShort(classFile.methods.length);
        for (MethodInfo method : classFile.methods) {
            writeMethodInfo(method);
        }
    }

    private void writeMethodInfo(MethodInfo method) throws IOException {
        out.writeShort(method.accessFlags);
        out.writeShort(method.nameIndex);
        out.writeShort(method.descriptorIndex);
        writeAttributes(method.attributes);
    }

    private void writeAttributes(AttributeInfo[] attributes) throws IOException {
        out.writeShort(attributes.length);
        for (AttributeInfo attr : attributes) {
            writeAttributeInfo(attr);
        }
    }

    private void writeAttributeInfo(AttributeInfo attr) throws IOException {
        out.writeShort(attr.attributeNameIndex);
        if (attr instanceof CodeAttribute) {
            writeCodeAttribute((CodeAttribute) attr);
        } else if (attr instanceof ConstantValueAttribute) {
            out.writeInt(2);
            out.writeShort(((ConstantValueAttribute) attr).constantValueIndex);
        } else if (attr instanceof SourceFileAttribute) {
            out.writeInt(2);
            out.writeShort(((SourceFileAttribute) attr).sourceFileIndex);
        } else if (attr instanceof ExceptionsAttribute) {
            ExceptionsAttribute ea = (ExceptionsAttribute) attr;
            int length = 2 + 2 * ea.exceptionIndexTable.length;
            out.writeInt(length);
            out.writeShort(ea.exceptionIndexTable.length);
            for (int exIdx : ea.exceptionIndexTable) {
                out.writeShort(exIdx);
            }
        } else if (attr instanceof LineNumberTableAttribute) {
            LineNumberTableAttribute lna = (LineNumberTableAttribute) attr;
            int length = 2 + 4 * lna.lineNumberTable.length;
            out.writeInt(length);
            out.writeShort(lna.lineNumberTable.length);
            for (LineNumberTableAttribute.LineNumberEntry entry : lna.lineNumberTable) {
                out.writeShort(entry.startPc);
                out.writeShort(entry.lineNumber);
            }
        } else if (attr instanceof LocalVariableTableAttribute) {
            LocalVariableTableAttribute lva = (LocalVariableTableAttribute) attr;
            int length = 2 + 10 * lva.localVariableTable.length;
            out.writeInt(length);
            out.writeShort(lva.localVariableTable.length);
            for (LocalVariableTableAttribute.LocalVariableEntry entry : lva.localVariableTable) {
                out.writeShort(entry.startPc);
                out.writeShort(entry.length);
                out.writeShort(entry.nameIndex);
                out.writeShort(entry.descriptorIndex);
                out.writeShort(entry.index);
            }
        } else if (attr instanceof SignatureAttribute) {
            out.writeInt(2);
            out.writeShort(((SignatureAttribute) attr).signatureIndex);
        } else if (attr instanceof StackMapTableAttribute) {
            writeStackMapTableAttribute((StackMapTableAttribute) attr);
        } else if (attr instanceof RawAttribute) {
            RawAttribute ra = (RawAttribute) attr;
            out.writeInt(ra.info.length);
            out.write(ra.info);
        } else {
            out.writeInt(0);
        }
    }

    private void writeCodeAttribute(CodeAttribute code) throws IOException {
        ByteArrayOutputStream codeBaos = new ByteArrayOutputStream();
        DataOutputStream codeOut = new DataOutputStream(codeBaos);

        codeOut.writeShort(code.maxStack);
        codeOut.writeShort(code.maxLocals);
        codeOut.writeInt(code.code.length);
        codeOut.write(code.code);

        codeOut.writeShort(code.exceptionTable.length);
        for (CodeAttribute.ExceptionTableEntry entry : code.exceptionTable) {
            codeOut.writeShort(entry.startPc);
            codeOut.writeShort(entry.endPc);
            codeOut.writeShort(entry.handlerPc);
            codeOut.writeShort(entry.catchType);
        }

        codeOut.writeShort(code.attributes.length);
        for (AttributeInfo attr : code.attributes) {
            writeAttributeInfoToStream(codeOut, attr);
        }

        codeOut.flush();
        byte[] codeData = codeBaos.toByteArray();
        out.writeInt(codeData.length);
        out.write(codeData);
    }

    private void writeStackMapTableAttribute(StackMapTableAttribute smt) throws IOException {
        ByteArrayOutputStream smtBaos = new ByteArrayOutputStream();
        DataOutputStream smtOut = new DataOutputStream(smtBaos);
        smtOut.writeShort(smt.entries.length);
        for (StackMapTableAttribute.StackMapFrame frame : smt.entries) {
            writeStackMapFrame(smtOut, frame);
        }
        smtOut.flush();
        byte[] smtData = smtBaos.toByteArray();
        out.writeInt(smtData.length);
        out.write(smtData);
    }

    private void writeStackMapFrame(DataOutputStream out, StackMapTableAttribute.StackMapFrame frame) throws IOException {
        out.writeByte(frame.frameType);
        if (frame.frameType == 247) {
            out.writeShort(frame.offsetDelta);
            writeVerificationTypeInfo(out, frame.stack[0]);
        } else if (frame.frameType >= 248 && frame.frameType <= 251) {
            out.writeShort(frame.offsetDelta);
        } else if (frame.frameType >= 252 && frame.frameType <= 254) {
            out.writeShort(frame.offsetDelta);
            for (StackMapTableAttribute.VerificationTypeInfo vti : frame.locals) {
                writeVerificationTypeInfo(out, vti);
            }
        } else if (frame.frameType == 255) {
            out.writeShort(frame.offsetDelta);
            out.writeShort(frame.locals.length);
            for (StackMapTableAttribute.VerificationTypeInfo vti : frame.locals) {
                writeVerificationTypeInfo(out, vti);
            }
            out.writeShort(frame.stack.length);
            for (StackMapTableAttribute.VerificationTypeInfo vti : frame.stack) {
                writeVerificationTypeInfo(out, vti);
            }
        } else if (frame.frameType >= 64 && frame.frameType <= 127) {
            writeVerificationTypeInfo(out, frame.stack[0]);
        }
    }

    private void writeVerificationTypeInfo(DataOutputStream out, StackMapTableAttribute.VerificationTypeInfo vti) throws IOException {
        out.writeByte(vti.tag);
        if (vti.tag == StackMapTableAttribute.VerificationTypeInfo.ITEM_Object) {
            out.writeShort(vti.cpoolIndex);
        } else if (vti.tag == StackMapTableAttribute.VerificationTypeInfo.ITEM_Uninitialized) {
            out.writeShort(vti.offset);
        }
    }

    private void writeAttributeInfoToStream(DataOutputStream dos, AttributeInfo attr) throws IOException {
        dos.writeShort(attr.attributeNameIndex);
        if (attr instanceof CodeAttribute) {
            CodeAttribute code = (CodeAttribute) attr;
            ByteArrayOutputStream innerBaos = new ByteArrayOutputStream();
            DataOutputStream innerOut = new DataOutputStream(innerBaos);
            innerOut.writeShort(code.maxStack);
            innerOut.writeShort(code.maxLocals);
            innerOut.writeInt(code.code.length);
            innerOut.write(code.code);
            innerOut.writeShort(code.exceptionTable.length);
            for (CodeAttribute.ExceptionTableEntry entry : code.exceptionTable) {
                innerOut.writeShort(entry.startPc);
                innerOut.writeShort(entry.endPc);
                innerOut.writeShort(entry.handlerPc);
                innerOut.writeShort(entry.catchType);
            }
            innerOut.writeShort(code.attributes.length);
            for (AttributeInfo a : code.attributes) {
                writeAttributeInfoToStream(innerOut, a);
            }
            innerOut.flush();
            byte[] data = innerBaos.toByteArray();
            dos.writeInt(data.length);
            dos.write(data);
        } else if (attr instanceof LineNumberTableAttribute) {
            LineNumberTableAttribute lna = (LineNumberTableAttribute) attr;
            int length = 2 + 4 * lna.lineNumberTable.length;
            dos.writeInt(length);
            dos.writeShort(lna.lineNumberTable.length);
            for (LineNumberTableAttribute.LineNumberEntry entry : lna.lineNumberTable) {
                dos.writeShort(entry.startPc);
                dos.writeShort(entry.lineNumber);
            }
        } else if (attr instanceof LocalVariableTableAttribute) {
            LocalVariableTableAttribute lva = (LocalVariableTableAttribute) attr;
            int length = 2 + 10 * lva.localVariableTable.length;
            dos.writeInt(length);
            dos.writeShort(lva.localVariableTable.length);
            for (LocalVariableTableAttribute.LocalVariableEntry entry : lva.localVariableTable) {
                dos.writeShort(entry.startPc);
                dos.writeShort(entry.length);
                dos.writeShort(entry.nameIndex);
                dos.writeShort(entry.descriptorIndex);
                dos.writeShort(entry.index);
            }
        } else if (attr instanceof StackMapTableAttribute) {
            StackMapTableAttribute smt = (StackMapTableAttribute) attr;
            ByteArrayOutputStream smtBaos = new ByteArrayOutputStream();
            DataOutputStream smtOut = new DataOutputStream(smtBaos);
            smtOut.writeShort(smt.entries.length);
            for (StackMapTableAttribute.StackMapFrame frame : smt.entries) {
                writeStackMapFrame(smtOut, frame);
            }
            smtOut.flush();
            byte[] data = smtBaos.toByteArray();
            dos.writeInt(data.length);
            dos.write(data);
        } else if (attr instanceof RawAttribute) {
            RawAttribute ra = (RawAttribute) attr;
            dos.writeInt(ra.info.length);
            dos.write(ra.info);
        } else if (attr instanceof SourceFileAttribute) {
            dos.writeInt(2);
            dos.writeShort(((SourceFileAttribute) attr).sourceFileIndex);
        } else if (attr instanceof ExceptionsAttribute) {
            ExceptionsAttribute ea = (ExceptionsAttribute) attr;
            int length = 2 + 2 * ea.exceptionIndexTable.length;
            dos.writeInt(length);
            dos.writeShort(ea.exceptionIndexTable.length);
            for (int exIdx : ea.exceptionIndexTable) {
                dos.writeShort(exIdx);
            }
        } else if (attr instanceof SignatureAttribute) {
            dos.writeInt(2);
            dos.writeShort(((SignatureAttribute) attr).signatureIndex);
        } else {
            dos.writeInt(0);
        }
    }
}
