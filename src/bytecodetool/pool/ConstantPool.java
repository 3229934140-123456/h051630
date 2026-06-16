package bytecodetool.pool;

import bytecodetool.model.CpInfo;
import bytecodetool.model.CpInfo.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantPool {
    private final List<CpInfo> constants;
    private final Map<String, Integer> utf8Index;
    private final Map<String, Integer> classIndex;
    private final Map<String, Integer> nameAndTypeIndex;
    private final Map<String, Integer> methodrefIndex;
    private final Map<String, Integer> fieldrefIndex;
    private final Map<String, Integer> stringIndex;

    public ConstantPool() {
        this.constants = new ArrayList<>();
        this.constants.add(null);
        this.utf8Index = new HashMap<>();
        this.classIndex = new HashMap<>();
        this.nameAndTypeIndex = new HashMap<>();
        this.methodrefIndex = new HashMap<>();
        this.fieldrefIndex = new HashMap<>();
        this.stringIndex = new HashMap<>();
    }

    public ConstantPool(CpInfo[] initialConstants) {
        this();
        for (int i = 1; i < initialConstants.length; i++) {
            CpInfo cp = initialConstants[i];
            addConstant(cp);
            if (cp instanceof LongInfo || cp instanceof DoubleInfo) {
                i++;
                if (i < initialConstants.length) {
                    this.constants.add(null);
                }
            }
        }
        rebuildIndex();
    }

    public int size() {
        return constants.size();
    }

    public CpInfo get(int index) {
        return constants.get(index);
    }

    public List<CpInfo> getAll() {
        return constants;
    }

    public String getUtf8(int index) {
        CpInfo cp = constants.get(index);
        if (cp instanceof Utf8Info) {
            return ((Utf8Info) cp).value;
        }
        return null;
    }

    public String getClassName(int index) {
        CpInfo cp = constants.get(index);
        if (cp instanceof ClassInfo) {
            return getUtf8(((ClassInfo) cp).nameIndex);
        }
        return null;
    }

    public int addUtf8(String value) {
        Integer idx = utf8Index.get(value);
        if (idx != null) return idx;
        int index = addConstant(new Utf8Info(value));
        utf8Index.put(value, index);
        return index;
    }

    public int addClass(String className) {
        Integer idx = classIndex.get(className);
        if (idx != null) return idx;
        int nameIdx = addUtf8(className.replace('.', '/'));
        int index = addConstant(new ClassInfo(nameIdx));
        classIndex.put(className, index);
        return index;
    }

    public int addNameAndType(String name, String descriptor) {
        String key = name + ":" + descriptor;
        Integer idx = nameAndTypeIndex.get(key);
        if (idx != null) return idx;
        int nameIdx = addUtf8(name);
        int descIdx = addUtf8(descriptor);
        int index = addConstant(new NameAndTypeInfo(nameIdx, descIdx));
        nameAndTypeIndex.put(key, index);
        return index;
    }

    public int addMethodref(String className, String name, String descriptor) {
        String key = className + "." + name + descriptor;
        Integer idx = methodrefIndex.get(key);
        if (idx != null) return idx;
        int classIdx = addClass(className);
        int natIdx = addNameAndType(name, descriptor);
        int index = addConstant(new MethodrefInfo(classIdx, natIdx));
        methodrefIndex.put(key, index);
        return index;
    }

    public int addFieldref(String className, String name, String descriptor) {
        String key = className + "." + name + descriptor;
        Integer idx = fieldrefIndex.get(key);
        if (idx != null) return idx;
        int classIdx = addClass(className);
        int natIdx = addNameAndType(name, descriptor);
        int index = addConstant(new FieldrefInfo(classIdx, natIdx));
        fieldrefIndex.put(key, index);
        return index;
    }

    public int addString(String value) {
        Integer idx = stringIndex.get(value);
        if (idx != null) return idx;
        int utf8Idx = addUtf8(value);
        int index = addConstant(new StringInfo(utf8Idx));
        stringIndex.put(value, index);
        return index;
    }

    public int addInteger(int value) {
        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp instanceof IntegerInfo && ((IntegerInfo) cp).value == value) {
                return i;
            }
        }
        return addConstant(new IntegerInfo(value));
    }

    public int addLong(long value) {
        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp instanceof LongInfo && ((LongInfo) cp).value == value) {
                return i;
            }
        }
        int index = addConstant(new LongInfo(value));
        constants.add(null);
        return index;
    }

    public int addFloat(float value) {
        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp instanceof FloatInfo && ((FloatInfo) cp).value == value) {
                return i;
            }
        }
        return addConstant(new FloatInfo(value));
    }

    public int addDouble(double value) {
        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp instanceof DoubleInfo && ((DoubleInfo) cp).value == value) {
                return i;
            }
        }
        int index = addConstant(new DoubleInfo(value));
        constants.add(null);
        return index;
    }

    private int addConstant(CpInfo cp) {
        constants.add(cp);
        return constants.size() - 1;
    }

    public void rebuildIndex() {
        utf8Index.clear();
        classIndex.clear();
        nameAndTypeIndex.clear();
        methodrefIndex.clear();
        fieldrefIndex.clear();
        stringIndex.clear();

        for (int i = 1; i < constants.size(); i++) {
            CpInfo cp = constants.get(i);
            if (cp == null) continue;
            switch (cp.tag) {
                case CpInfo.CONSTANT_Utf8:
                    utf8Index.put(((Utf8Info) cp).value, i);
                    break;
                case CpInfo.CONSTANT_String:
                    String s = getUtf8(((StringInfo) cp).stringIndex);
                    if (s != null) stringIndex.put(s, i);
                    break;
                case CpInfo.CONSTANT_Class:
                    String cn = getUtf8(((ClassInfo) cp).nameIndex);
                    if (cn != null) classIndex.put(cn.replace('/', '.'), i);
                    break;
                case CpInfo.CONSTANT_NameAndType:
                    NameAndTypeInfo nat = (NameAndTypeInfo) cp;
                    String n = getUtf8(nat.nameIndex);
                    String d = getUtf8(nat.descriptorIndex);
                    if (n != null && d != null) nameAndTypeIndex.put(n + ":" + d, i);
                    break;
                case CpInfo.CONSTANT_Methodref:
                    MethodrefInfo mr = (MethodrefInfo) cp;
                    String mcn = getClassName(mr.classIndex);
                    NameAndTypeInfo mnat = (NameAndTypeInfo) get(mr.nameAndTypeIndex);
                    if (mcn != null && mnat != null) {
                        String mn = getUtf8(mnat.nameIndex);
                        String md = getUtf8(mnat.descriptorIndex);
                        if (mn != null && md != null) {
                            methodrefIndex.put(mcn.replace('/', '.') + "." + mn + md, i);
                        }
                    }
                    break;
                case CpInfo.CONSTANT_Fieldref:
                    FieldrefInfo fr = (FieldrefInfo) cp;
                    String fcn = getClassName(fr.classIndex);
                    NameAndTypeInfo fnat = (NameAndTypeInfo) get(fr.nameAndTypeIndex);
                    if (fcn != null && fnat != null) {
                        String fn = getUtf8(fnat.nameIndex);
                        String fd = getUtf8(fnat.descriptorIndex);
                        if (fn != null && fd != null) {
                            fieldrefIndex.put(fcn.replace('/', '.') + "." + fn + fd, i);
                        }
                    }
                    break;
            }
        }
    }

    public CpInfo[] toArray() {
        return constants.toArray(new CpInfo[0]);
    }
}
