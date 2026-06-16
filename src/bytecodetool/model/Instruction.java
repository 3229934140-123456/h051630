package bytecodetool.model;

public class Instruction {
    public int opcode;
    public int offset;
    public int length;
    public Object[] operands;

    public Instruction(int opcode, int offset, int length) {
        this.opcode = opcode;
        this.offset = offset;
        this.length = length;
        this.operands = new Object[0];
    }

    public Instruction(int opcode, int offset, int length, Object[] operands) {
        this.opcode = opcode;
        this.offset = offset;
        this.length = length;
        this.operands = operands;
    }

    public int getIntOperand(int index) {
        return ((Number) operands[index]).intValue();
    }

    public short getShortOperand(int index) {
        return ((Number) operands[index]).shortValue();
    }

    public byte getByteOperand(int index) {
        return ((Number) operands[index]).byteValue();
    }
}
