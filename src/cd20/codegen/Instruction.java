package cd20.codegen;

import java.util.List;
import java.util.StringJoiner;

import cd20.StringUtils;

import java.util.Arrays;

/**
 * An SM20 instruction.
 */
public class Instruction {
  private Operation operation;
  private List<Byte> operands;

  /**
   * Construct a new instruction.
   * @param operation Operation to perform.
   */
  public Instruction(Operation operation, Byte... operands) {
    this.operation = operation;
    this.operands = Arrays.asList(operands);
  }

  /**
   * Get the total size of this instruction in bytes.
   */
  public int getSize() {
    return operands.size() + 1;
  }

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public List<Byte> getOperands() {
    return operands;
  }

  public void setOperands(Byte[] bytes) {
    this.operands = Arrays.asList(bytes);
  }

  @Override
  public String toString() {
    StringJoiner joiner = new StringJoiner(" ");
    joiner.add(String.format(
        "%s (%s):",
        StringUtils.rightPad(5, operation.name()),
        StringUtils.leftPad(2, "" + operation.getCode(), '0')
    ));

    for (byte operand : operands) {
      joiner.add(StringUtils.leftPad(2, "" + operand, '0'));
    }

    return joiner.toString();
  }
}
