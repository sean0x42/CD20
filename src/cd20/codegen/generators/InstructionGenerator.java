package cd20.codegen.generators;

import java.util.Collection;

import cd20.codegen.Instruction;

public class InstructionGenerator extends WordFilledGenerator<Instruction> {
  /**
   * Generate code for a collection of instructions.
   * @param instructions Instructions to generate code for.
   */
  public void generate(Collection<Instruction> instructions) {
    // Insert each instruction
    for (Instruction instruction : instructions) {
      insertByte(instruction.getOperation().getCode());

      // Insert each operand
      for (Byte operand : instruction.getOperands()) {
        // We need to make sure we print the value as if it was an unsigned
        // byte.
        int unsignedByte = operand & 0xFF;
        insertByte(unsignedByte);
      }
    }

    super.fill();
  }
}
