package bytecodetool.opcode;

public class Opcodes {
    public static final int NOP = 0x00;
    public static final int ACONST_NULL = 0x01;
    public static final int ICONST_M1 = 0x02;
    public static final int ICONST_0 = 0x03;
    public static final int ICONST_1 = 0x04;
    public static final int ICONST_2 = 0x05;
    public static final int ICONST_3 = 0x06;
    public static final int ICONST_4 = 0x07;
    public static final int ICONST_5 = 0x08;
    public static final int LCONST_0 = 0x09;
    public static final int LCONST_1 = 0x0A;
    public static final int FCONST_0 = 0x0B;
    public static final int FCONST_1 = 0x0C;
    public static final int FCONST_2 = 0x0D;
    public static final int DCONST_0 = 0x0E;
    public static final int DCONST_1 = 0x0F;
    public static final int BIPUSH = 0x10;
    public static final int SIPUSH = 0x11;
    public static final int LDC = 0x12;
    public static final int LDC_W = 0x13;
    public static final int LDC2_W = 0x14;
    public static final int ILOAD = 0x15;
    public static final int LLOAD = 0x16;
    public static final int FLOAD = 0x17;
    public static final int DLOAD = 0x18;
    public static final int ALOAD = 0x19;
    public static final int ILOAD_0 = 0x1A;
    public static final int ILOAD_1 = 0x1B;
    public static final int ILOAD_2 = 0x1C;
    public static final int ILOAD_3 = 0x1D;
    public static final int LLOAD_0 = 0x1E;
    public static final int LLOAD_1 = 0x1F;
    public static final int LLOAD_2 = 0x20;
    public static final int LLOAD_3 = 0x21;
    public static final int FLOAD_0 = 0x22;
    public static final int FLOAD_1 = 0x23;
    public static final int FLOAD_2 = 0x24;
    public static final int FLOAD_3 = 0x25;
    public static final int DLOAD_0 = 0x26;
    public static final int DLOAD_1 = 0x27;
    public static final int DLOAD_2 = 0x28;
    public static final int DLOAD_3 = 0x29;
    public static final int ALOAD_0 = 0x2A;
    public static final int ALOAD_1 = 0x2B;
    public static final int ALOAD_2 = 0x2C;
    public static final int ALOAD_3 = 0x2D;
    public static final int IALOAD = 0x2E;
    public static final int LALOAD = 0x2F;
    public static final int FALOAD = 0x30;
    public static final int DALOAD = 0x31;
    public static final int AALOAD = 0x32;
    public static final int BALOAD = 0x33;
    public static final int CALOAD = 0x34;
    public static final int SALOAD = 0x35;
    public static final int ISTORE = 0x36;
    public static final int LSTORE = 0x37;
    public static final int FSTORE = 0x38;
    public static final int DSTORE = 0x39;
    public static final int ASTORE = 0x3A;
    public static final int ISTORE_0 = 0x3B;
    public static final int ISTORE_1 = 0x3C;
    public static final int ISTORE_2 = 0x3D;
    public static final int ISTORE_3 = 0x3E;
    public static final int LSTORE_0 = 0x3F;
    public static final int LSTORE_1 = 0x40;
    public static final int LSTORE_2 = 0x41;
    public static final int LSTORE_3 = 0x42;
    public static final int FSTORE_0 = 0x43;
    public static final int FSTORE_1 = 0x44;
    public static final int FSTORE_2 = 0x45;
    public static final int FSTORE_3 = 0x46;
    public static final int DSTORE_0 = 0x47;
    public static final int DSTORE_1 = 0x48;
    public static final int DSTORE_2 = 0x49;
    public static final int DSTORE_3 = 0x4A;
    public static final int ASTORE_0 = 0x4B;
    public static final int ASTORE_1 = 0x4C;
    public static final int ASTORE_2 = 0x4D;
    public static final int ASTORE_3 = 0x4E;
    public static final int IASTORE = 0x4F;
    public static final int LASTORE = 0x50;
    public static final int FASTORE = 0x51;
    public static final int DASTORE = 0x52;
    public static final int AASTORE = 0x53;
    public static final int BASTORE = 0x54;
    public static final int CASTORE = 0x55;
    public static final int SASTORE = 0x56;
    public static final int POP = 0x57;
    public static final int POP2 = 0x58;
    public static final int DUP = 0x59;
    public static final int DUP_X1 = 0x5A;
    public static final int DUP_X2 = 0x5B;
    public static final int DUP2 = 0x5C;
    public static final int DUP2_X1 = 0x5D;
    public static final int DUP2_X2 = 0x5E;
    public static final int SWAP = 0x5F;
    public static final int IADD = 0x60;
    public static final int LADD = 0x61;
    public static final int FADD = 0x62;
    public static final int DADD = 0x63;
    public static final int ISUB = 0x64;
    public static final int LSUB = 0x65;
    public static final int FSUB = 0x66;
    public static final int DSUB = 0x67;
    public static final int IMUL = 0x68;
    public static final int LMUL = 0x69;
    public static final int FMUL = 0x6A;
    public static final int DMUL = 0x6B;
    public static final int IDIV = 0x6C;
    public static final int LDIV = 0x6D;
    public static final int FDIV = 0x6E;
    public static final int DDIV = 0x6F;
    public static final int IREM = 0x70;
    public static final int LREM = 0x71;
    public static final int FREM = 0x72;
    public static final int DREM = 0x73;
    public static final int INEG = 0x74;
    public static final int LNEG = 0x75;
    public static final int FNEG = 0x76;
    public static final int DNEG = 0x77;
    public static final int ISHL = 0x78;
    public static final int LSHL = 0x79;
    public static final int ISHR = 0x7A;
    public static final int LSHR = 0x7B;
    public static final int IUSHR = 0x7C;
    public static final int LUSHR = 0x7D;
    public static final int IAND = 0x7E;
    public static final int LAND = 0x7F;
    public static final int IOR = 0x80;
    public static final int LOR = 0x81;
    public static final int IXOR = 0x82;
    public static final int LXOR = 0x83;
    public static final int IINC = 0x84;
    public static final int I2L = 0x85;
    public static final int I2F = 0x86;
    public static final int I2D = 0x87;
    public static final int L2I = 0x88;
    public static final int L2F = 0x89;
    public static final int L2D = 0x8A;
    public static final int F2I = 0x8B;
    public static final int F2L = 0x8C;
    public static final int F2D = 0x8D;
    public static final int D2I = 0x8E;
    public static final int D2L = 0x8F;
    public static final int D2F = 0x90;
    public static final int I2B = 0x91;
    public static final int I2C = 0x92;
    public static final int I2S = 0x93;
    public static final int LCMP = 0x94;
    public static final int FCMPL = 0x95;
    public static final int FCMPG = 0x96;
    public static final int DCMPL = 0x97;
    public static final int DCMPG = 0x98;
    public static final int IFEQ = 0x99;
    public static final int IFNE = 0x9A;
    public static final int IFLT = 0x9B;
    public static final int IFGE = 0x9C;
    public static final int IFGT = 0x9D;
    public static final int IFLE = 0x9E;
    public static final int IF_ICMPEQ = 0x9F;
    public static final int IF_ICMPNE = 0xA0;
    public static final int IF_ICMPLT = 0xA1;
    public static final int IF_ICMPGE = 0xA2;
    public static final int IF_ICMPGT = 0xA3;
    public static final int IF_ICMPLE = 0xA4;
    public static final int IF_ACMPEQ = 0xA5;
    public static final int IF_ACMPNE = 0xA6;
    public static final int GOTO = 0xA7;
    public static final int JSR = 0xA8;
    public static final int RET = 0xA9;
    public static final int TABLESWITCH = 0xAA;
    public static final int LOOKUPSWITCH = 0xAB;
    public static final int IRETURN = 0xAC;
    public static final int LRETURN = 0xAD;
    public static final int FRETURN = 0xAE;
    public static final int DRETURN = 0xAF;
    public static final int ARETURN = 0xB0;
    public static final int RETURN = 0xB1;
    public static final int GETSTATIC = 0xB2;
    public static final int PUTSTATIC = 0xB3;
    public static final int GETFIELD = 0xB4;
    public static final int PUTFIELD = 0xB5;
    public static final int INVOKEVIRTUAL = 0xB6;
    public static final int INVOKESPECIAL = 0xB7;
    public static final int INVOKESTATIC = 0xB8;
    public static final int INVOKEINTERFACE = 0xB9;
    public static final int INVOKEDYNAMIC = 0xBA;
    public static final int NEW = 0xBB;
    public static final int NEWARRAY = 0xBC;
    public static final int ANEWARRAY = 0xBD;
    public static final int ARRAYLENGTH = 0xBE;
    public static final int ATHROW = 0xBF;
    public static final int CHECKCAST = 0xC0;
    public static final int INSTANCEOF = 0xC1;
    public static final int MONITORENTER = 0xC2;
    public static final int MONITOREXIT = 0xC3;
    public static final int WIDE = 0xC4;
    public static final int MULTIANEWARRAY = 0xC5;
    public static final int IFNULL = 0xC6;
    public static final int IFNONNULL = 0xC7;
    public static final int GOTO_W = 0xC8;
    public static final int JSR_W = 0xC9;

    public static final int T_BOOLEAN = 4;
    public static final int T_CHAR = 5;
    public static final int T_FLOAT = 6;
    public static final int T_DOUBLE = 7;
    public static final int T_BYTE = 8;
    public static final int T_SHORT = 9;
    public static final int T_INT = 10;
    public static final int T_LONG = 11;

    private static final String[] OPCODE_NAMES = new String[256];

    static {
        OPCODE_NAMES[NOP] = "nop";
        OPCODE_NAMES[ACONST_NULL] = "aconst_null";
        OPCODE_NAMES[ICONST_M1] = "iconst_m1";
        OPCODE_NAMES[ICONST_0] = "iconst_0";
        OPCODE_NAMES[ICONST_1] = "iconst_1";
        OPCODE_NAMES[ICONST_2] = "iconst_2";
        OPCODE_NAMES[ICONST_3] = "iconst_3";
        OPCODE_NAMES[ICONST_4] = "iconst_4";
        OPCODE_NAMES[ICONST_5] = "iconst_5";
        OPCODE_NAMES[LCONST_0] = "lconst_0";
        OPCODE_NAMES[LCONST_1] = "lconst_1";
        OPCODE_NAMES[FCONST_0] = "fconst_0";
        OPCODE_NAMES[FCONST_1] = "fconst_1";
        OPCODE_NAMES[FCONST_2] = "fconst_2";
        OPCODE_NAMES[DCONST_0] = "dconst_0";
        OPCODE_NAMES[DCONST_1] = "dconst_1";
        OPCODE_NAMES[BIPUSH] = "bipush";
        OPCODE_NAMES[SIPUSH] = "sipush";
        OPCODE_NAMES[LDC] = "ldc";
        OPCODE_NAMES[LDC_W] = "ldc_w";
        OPCODE_NAMES[LDC2_W] = "ldc2_w";
        OPCODE_NAMES[ILOAD] = "iload";
        OPCODE_NAMES[LLOAD] = "lload";
        OPCODE_NAMES[FLOAD] = "fload";
        OPCODE_NAMES[DLOAD] = "dload";
        OPCODE_NAMES[ALOAD] = "aload";
        OPCODE_NAMES[ILOAD_0] = "iload_0";
        OPCODE_NAMES[ILOAD_1] = "iload_1";
        OPCODE_NAMES[ILOAD_2] = "iload_2";
        OPCODE_NAMES[ILOAD_3] = "iload_3";
        OPCODE_NAMES[LLOAD_0] = "lload_0";
        OPCODE_NAMES[LLOAD_1] = "lload_1";
        OPCODE_NAMES[LLOAD_2] = "lload_2";
        OPCODE_NAMES[LLOAD_3] = "lload_3";
        OPCODE_NAMES[FLOAD_0] = "fload_0";
        OPCODE_NAMES[FLOAD_1] = "fload_1";
        OPCODE_NAMES[FLOAD_2] = "fload_2";
        OPCODE_NAMES[FLOAD_3] = "fload_3";
        OPCODE_NAMES[DLOAD_0] = "dload_0";
        OPCODE_NAMES[DLOAD_1] = "dload_1";
        OPCODE_NAMES[DLOAD_2] = "dload_2";
        OPCODE_NAMES[DLOAD_3] = "dload_3";
        OPCODE_NAMES[ALOAD_0] = "aload_0";
        OPCODE_NAMES[ALOAD_1] = "aload_1";
        OPCODE_NAMES[ALOAD_2] = "aload_2";
        OPCODE_NAMES[ALOAD_3] = "aload_3";
        OPCODE_NAMES[IALOAD] = "iaload";
        OPCODE_NAMES[LALOAD] = "laload";
        OPCODE_NAMES[FALOAD] = "faload";
        OPCODE_NAMES[DALOAD] = "daload";
        OPCODE_NAMES[AALOAD] = "aaload";
        OPCODE_NAMES[BALOAD] = "baload";
        OPCODE_NAMES[CALOAD] = "caload";
        OPCODE_NAMES[SALOAD] = "saload";
        OPCODE_NAMES[ISTORE] = "istore";
        OPCODE_NAMES[LSTORE] = "lstore";
        OPCODE_NAMES[FSTORE] = "fstore";
        OPCODE_NAMES[DSTORE] = "dstore";
        OPCODE_NAMES[ASTORE] = "astore";
        OPCODE_NAMES[ISTORE_0] = "istore_0";
        OPCODE_NAMES[ISTORE_1] = "istore_1";
        OPCODE_NAMES[ISTORE_2] = "istore_2";
        OPCODE_NAMES[ISTORE_3] = "istore_3";
        OPCODE_NAMES[LSTORE_0] = "lstore_0";
        OPCODE_NAMES[LSTORE_1] = "lstore_1";
        OPCODE_NAMES[LSTORE_2] = "lstore_2";
        OPCODE_NAMES[LSTORE_3] = "lstore_3";
        OPCODE_NAMES[FSTORE_0] = "fstore_0";
        OPCODE_NAMES[FSTORE_1] = "fstore_1";
        OPCODE_NAMES[FSTORE_2] = "fstore_2";
        OPCODE_NAMES[FSTORE_3] = "fstore_3";
        OPCODE_NAMES[DSTORE_0] = "dstore_0";
        OPCODE_NAMES[DSTORE_1] = "dstore_1";
        OPCODE_NAMES[DSTORE_2] = "dstore_2";
        OPCODE_NAMES[DSTORE_3] = "dstore_3";
        OPCODE_NAMES[ASTORE_0] = "astore_0";
        OPCODE_NAMES[ASTORE_1] = "astore_1";
        OPCODE_NAMES[ASTORE_2] = "astore_2";
        OPCODE_NAMES[ASTORE_3] = "astore_3";
        OPCODE_NAMES[IASTORE] = "iastore";
        OPCODE_NAMES[LASTORE] = "lastore";
        OPCODE_NAMES[FASTORE] = "fastore";
        OPCODE_NAMES[DASTORE] = "dastore";
        OPCODE_NAMES[AASTORE] = "aastore";
        OPCODE_NAMES[BASTORE] = "bastore";
        OPCODE_NAMES[CASTORE] = "castore";
        OPCODE_NAMES[SASTORE] = "sastore";
        OPCODE_NAMES[POP] = "pop";
        OPCODE_NAMES[POP2] = "pop2";
        OPCODE_NAMES[DUP] = "dup";
        OPCODE_NAMES[DUP_X1] = "dup_x1";
        OPCODE_NAMES[DUP_X2] = "dup_x2";
        OPCODE_NAMES[DUP2] = "dup2";
        OPCODE_NAMES[DUP2_X1] = "dup2_x1";
        OPCODE_NAMES[DUP2_X2] = "dup2_x2";
        OPCODE_NAMES[SWAP] = "swap";
        OPCODE_NAMES[IADD] = "iadd";
        OPCODE_NAMES[LADD] = "ladd";
        OPCODE_NAMES[FADD] = "fadd";
        OPCODE_NAMES[DADD] = "dadd";
        OPCODE_NAMES[ISUB] = "isub";
        OPCODE_NAMES[LSUB] = "lsub";
        OPCODE_NAMES[FSUB] = "fsub";
        OPCODE_NAMES[DSUB] = "dsub";
        OPCODE_NAMES[IMUL] = "imul";
        OPCODE_NAMES[LMUL] = "lmul";
        OPCODE_NAMES[FMUL] = "fmul";
        OPCODE_NAMES[DMUL] = "dmul";
        OPCODE_NAMES[IDIV] = "idiv";
        OPCODE_NAMES[LDIV] = "ldiv";
        OPCODE_NAMES[FDIV] = "fdiv";
        OPCODE_NAMES[DDIV] = "ddiv";
        OPCODE_NAMES[IREM] = "irem";
        OPCODE_NAMES[LREM] = "lrem";
        OPCODE_NAMES[FREM] = "frem";
        OPCODE_NAMES[DREM] = "drem";
        OPCODE_NAMES[INEG] = "ineg";
        OPCODE_NAMES[LNEG] = "lneg";
        OPCODE_NAMES[FNEG] = "fneg";
        OPCODE_NAMES[DNEG] = "dneg";
        OPCODE_NAMES[ISHL] = "ishl";
        OPCODE_NAMES[LSHL] = "lshl";
        OPCODE_NAMES[ISHR] = "ishr";
        OPCODE_NAMES[LSHR] = "lshr";
        OPCODE_NAMES[IUSHR] = "iushr";
        OPCODE_NAMES[LUSHR] = "lushr";
        OPCODE_NAMES[IAND] = "iand";
        OPCODE_NAMES[LAND] = "land";
        OPCODE_NAMES[IOR] = "ior";
        OPCODE_NAMES[LOR] = "lor";
        OPCODE_NAMES[IXOR] = "ixor";
        OPCODE_NAMES[LXOR] = "lxor";
        OPCODE_NAMES[IINC] = "iinc";
        OPCODE_NAMES[I2L] = "i2l";
        OPCODE_NAMES[I2F] = "i2f";
        OPCODE_NAMES[I2D] = "i2d";
        OPCODE_NAMES[L2I] = "l2i";
        OPCODE_NAMES[L2F] = "l2f";
        OPCODE_NAMES[L2D] = "l2d";
        OPCODE_NAMES[F2I] = "f2i";
        OPCODE_NAMES[F2L] = "f2l";
        OPCODE_NAMES[F2D] = "f2d";
        OPCODE_NAMES[D2I] = "d2i";
        OPCODE_NAMES[D2L] = "d2l";
        OPCODE_NAMES[D2F] = "d2f";
        OPCODE_NAMES[I2B] = "i2b";
        OPCODE_NAMES[I2C] = "i2c";
        OPCODE_NAMES[I2S] = "i2s";
        OPCODE_NAMES[LCMP] = "lcmp";
        OPCODE_NAMES[FCMPL] = "fcmpl";
        OPCODE_NAMES[FCMPG] = "fcmpg";
        OPCODE_NAMES[DCMPL] = "dcmpl";
        OPCODE_NAMES[DCMPG] = "dcmpg";
        OPCODE_NAMES[IFEQ] = "ifeq";
        OPCODE_NAMES[IFNE] = "ifne";
        OPCODE_NAMES[IFLT] = "iflt";
        OPCODE_NAMES[IFGE] = "ifge";
        OPCODE_NAMES[IFGT] = "ifgt";
        OPCODE_NAMES[IFLE] = "ifle";
        OPCODE_NAMES[IF_ICMPEQ] = "if_icmpeq";
        OPCODE_NAMES[IF_ICMPNE] = "if_icmpne";
        OPCODE_NAMES[IF_ICMPLT] = "if_icmplt";
        OPCODE_NAMES[IF_ICMPGE] = "if_icmpge";
        OPCODE_NAMES[IF_ICMPGT] = "if_icmpgt";
        OPCODE_NAMES[IF_ICMPLE] = "if_icmple";
        OPCODE_NAMES[IF_ACMPEQ] = "if_acmpeq";
        OPCODE_NAMES[IF_ACMPNE] = "if_acmpne";
        OPCODE_NAMES[GOTO] = "goto";
        OPCODE_NAMES[JSR] = "jsr";
        OPCODE_NAMES[RET] = "ret";
        OPCODE_NAMES[TABLESWITCH] = "tableswitch";
        OPCODE_NAMES[LOOKUPSWITCH] = "lookupswitch";
        OPCODE_NAMES[IRETURN] = "ireturn";
        OPCODE_NAMES[LRETURN] = "lreturn";
        OPCODE_NAMES[FRETURN] = "freturn";
        OPCODE_NAMES[DRETURN] = "dreturn";
        OPCODE_NAMES[ARETURN] = "areturn";
        OPCODE_NAMES[RETURN] = "return";
        OPCODE_NAMES[GETSTATIC] = "getstatic";
        OPCODE_NAMES[PUTSTATIC] = "putstatic";
        OPCODE_NAMES[GETFIELD] = "getfield";
        OPCODE_NAMES[PUTFIELD] = "putfield";
        OPCODE_NAMES[INVOKEVIRTUAL] = "invokevirtual";
        OPCODE_NAMES[INVOKESPECIAL] = "invokespecial";
        OPCODE_NAMES[INVOKESTATIC] = "invokestatic";
        OPCODE_NAMES[INVOKEINTERFACE] = "invokeinterface";
        OPCODE_NAMES[INVOKEDYNAMIC] = "invokedynamic";
        OPCODE_NAMES[NEW] = "new";
        OPCODE_NAMES[NEWARRAY] = "newarray";
        OPCODE_NAMES[ANEWARRAY] = "anewarray";
        OPCODE_NAMES[ARRAYLENGTH] = "arraylength";
        OPCODE_NAMES[ATHROW] = "athrow";
        OPCODE_NAMES[CHECKCAST] = "checkcast";
        OPCODE_NAMES[INSTANCEOF] = "instanceof";
        OPCODE_NAMES[MONITORENTER] = "monitorenter";
        OPCODE_NAMES[MONITOREXIT] = "monitorexit";
        OPCODE_NAMES[WIDE] = "wide";
        OPCODE_NAMES[MULTIANEWARRAY] = "multianewarray";
        OPCODE_NAMES[IFNULL] = "ifnull";
        OPCODE_NAMES[IFNONNULL] = "ifnonnull";
        OPCODE_NAMES[GOTO_W] = "goto_w";
        OPCODE_NAMES[JSR_W] = "jsr_w";
    }

    public static String getOpcodeName(int opcode) {
        if (opcode < 0 || opcode > 255 || OPCODE_NAMES[opcode] == null) {
            return "invalid(" + opcode + ")";
        }
        return OPCODE_NAMES[opcode];
    }

    public static int getStackEffect(int opcode) {
        switch (opcode) {
            case NOP: return 0;
            case ACONST_NULL:
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
            case BIPUSH:
            case SIPUSH:
            case LDC:
            case LDC_W:
                return 1;
            case LCONST_0:
            case LCONST_1:
            case DCONST_0:
            case DCONST_1:
            case LDC2_W:
                return 2;
            case ILOAD:
            case FLOAD:
            case ALOAD:
            case ILOAD_0: case ILOAD_1: case ILOAD_2: case ILOAD_3:
            case FLOAD_0: case FLOAD_1: case FLOAD_2: case FLOAD_3:
            case ALOAD_0: case ALOAD_1: case ALOAD_2: case ALOAD_3:
                return 1;
            case LLOAD: case DLOAD:
            case LLOAD_0: case LLOAD_1: case LLOAD_2: case LLOAD_3:
            case DLOAD_0: case DLOAD_1: case DLOAD_2: case DLOAD_3:
                return 2;
            case IALOAD: case FALOAD: case AALOAD: case BALOAD: case CALOAD: case SALOAD:
                return -1;
            case LALOAD: case DALOAD:
                return 0;
            case ISTORE: case FSTORE: case ASTORE:
            case ISTORE_0: case ISTORE_1: case ISTORE_2: case ISTORE_3:
            case FSTORE_0: case FSTORE_1: case FSTORE_2: case FSTORE_3:
            case ASTORE_0: case ASTORE_1: case ASTORE_2: case ASTORE_3:
                return -1;
            case LSTORE: case DSTORE:
            case LSTORE_0: case LSTORE_1: case LSTORE_2: case LSTORE_3:
            case DSTORE_0: case DSTORE_1: case DSTORE_2: case DSTORE_3:
                return -2;
            case IASTORE: case FASTORE: case AASTORE: case BASTORE: case CASTORE: case SASTORE:
                return -3;
            case LASTORE: case DASTORE:
                return -4;
            case POP: return -1;
            case POP2: return -2;
            case DUP: return 1;
            case DUP_X1: return 1;
            case DUP_X2: return 1;
            case DUP2: return 2;
            case DUP2_X1: return 2;
            case DUP2_X2: return 2;
            case SWAP: return 0;
            case IADD: case ISUB: case IMUL: case IDIV: case IREM:
            case IAND: case IOR: case IXOR:
            case ISHL: case ISHR: case IUSHR:
            case FADD: case FSUB: case FMUL: case FDIV: case FREM:
                return -1;
            case LADD: case LSUB: case LMUL: case LDIV: case LREM:
            case LAND: case LOR: case LXOR:
            case LSHL: case LSHR: case LUSHR:
            case DADD: case DSUB: case DMUL: case DDIV: case DREM:
                return -2;
            case INEG: case FNEG: return 0;
            case LNEG: case DNEG: return 0;
            case IINC: return 0;
            case I2F: case L2I: case L2F: case F2I: case D2I: case D2L: case D2F:
                return 0;
            case I2L: case I2D: case F2L: case F2D:
                return 1;
            case L2D:
                return 0;
            case I2B: case I2C: case I2S: return 0;
            case LCMP: case FCMPL: case FCMPG: case DCMPL: case DCMPG:
                return -3;
            case IFEQ: case IFNE: case IFLT: case IFGE: case IFGT: case IFLE:
            case IFNULL: case IFNONNULL:
                return -1;
            case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT: case IF_ICMPGE: case IF_ICMPGT: case IF_ICMPLE:
            case IF_ACMPEQ: case IF_ACMPNE:
                return -2;
            case GOTO: case GOTO_W: case JSR: case JSR_W: case RET: return 0;
            case TABLESWITCH: case LOOKUPSWITCH: return -1;
            case IRETURN: case FRETURN: case ARETURN: return -1;
            case LRETURN: case DRETURN: return -2;
            case RETURN: return 0;
            case GETSTATIC: return 1;
            case PUTSTATIC: return -1;
            case GETFIELD: return 0;
            case PUTFIELD: return -2;
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
                return -1;
            case INVOKEINTERFACE:
                return -1;
            case INVOKEDYNAMIC:
                return -1;
            case NEW: return 1;
            case NEWARRAY: case ANEWARRAY: return 0;
            case ARRAYLENGTH: return 0;
            case ATHROW: return -1;
            case CHECKCAST: case INSTANCEOF: return 0;
            case MONITORENTER: case MONITOREXIT: return -1;
            case WIDE: return 0;
            case MULTIANEWARRAY:
                return -1;
            default: return 0;
        }
    }

    public static boolean isReturn(int opcode) {
        return opcode >= IRETURN && opcode <= RETURN;
    }
}
