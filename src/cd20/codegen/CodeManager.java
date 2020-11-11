package cd20.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import cd20.codegen.generators.Generator;
import cd20.ByteUtils;
import cd20.codegen.generators.FloatConstantGenerator;
import cd20.codegen.generators.InstructionGenerator;
import cd20.codegen.generators.IntegerConstantGenerator;
import cd20.codegen.generators.StringConstantGenerator;
import cd20.symboltable.Symbol;

public class CodeManager {
  private Map<Integer, Instruction> instructions = new LinkedHashMap<>();
  private List<Constant<Integer>> integerConstants = new ArrayList<>();
  private List<Constant<Float>> floatConstants = new ArrayList<>();
  private List<Constant<String>> stringConstants = new ArrayList<>();
  private int codeGenerationPosition;

  public CodeManager() {
    codeGenerationPosition = 0;
  }

  /**
   * Insert a new instruction.
   * @param instruction Instruction to insert.
   * @return Code generation position of instruction.
   */
  public int insert(Instruction instruction) {
    int position = codeGenerationPosition;
    instructions.put(position, instruction);
    codeGenerationPosition += instruction.getSize();
    return position;
  }

  /**
   * Insert a new operation.
   * @param operation Operation to insert.
   */
  public void insert(Operation operation) {
    insert(new Instruction(operation));
  }

  /**
   * Add a new string constant.
   * @param constant String constant.
   * @param symbol Corresponding symbol.
   */
  public void addStringConstant(String constant, Symbol symbol) {
    stringConstants.add(new Constant<String>(constant, symbol));
  }

  /**
   * Add a new integer constant.
   * @param constant Integer constant.
   * @param symbol Corresponding symbol.
   */
  public void addIntegerConstant(Integer constant, Symbol symbol) {
    integerConstants.add(new Constant<Integer>(constant, symbol));
  }

  /**
   * Add a new float constant.
   * @param constant Float constant.
   * @param symbol Corresponding symbol.
   */
  public void addFloatConstant(Float constant, Symbol symbol) {
    floatConstants.add(new Constant<Float>(constant, symbol));
  }

  /**
   * Set offsets on all constants in list.
   * @param constants A list of constants to set offsets for.
   * @param offsetAccumulator An accumulator containing the current starting
   * offset.
   * @return New offset value.
   */
  private <T> int setConstantOffset(List<Constant<T>> constants, int offsetAccumulator) {
    int offset = offsetAccumulator;

    for (Constant<T> constant : constants) {
      constant.getSymbol().setOffset(offset);

      if (constant.getValue() instanceof String) {
        offset += ((String) constant.getValue()).length() + 1;
      } else {
        offset += 8;
      }
    }

    return offset;
  }

  /**
   * Backfill all instructions to include symbol offsets.
   * Note: This function must be run after all symbols have been assigned
   * a register and an offset.
   */
  private void backfill() {
    // Find all back fill instructions
    for (Instruction instruction : instructions.values()) {
      if (instruction instanceof BackfillInstruction) {
        ((BackfillInstruction) instruction).backfill();
      }
    }
  }

  /**
   * Generate module and backfill instructions.
   * Format:
   * 1. Instruction section
   *    a. Number of lines to expect
   *    b. Instructions, word-filled with HALT instructions.
   * 2. Integer constants section
   *    a. Number of values to expect
   *    b. One line for each integer constant. May be negative.
   * 3. Floating point constants section.
   *    a. Number of values to expect
   *    b. One line for reach floating point constant.
   * 4. String constants section.
   *    a. Number of strings to expect
   *    b. Each string constant, delimited by null characters (00). Each new
   *    string does not need to start on a word boundary. Word filled with null
   *    characters (00).
   */
  public String generateModule() {
    // Update constant offsets now that we know where they are 
    int offset = ByteUtils.getNextByteBoundary(codeGenerationPosition);
    offset = setConstantOffset(integerConstants, offset);
    offset = setConstantOffset(floatConstants, offset);
    offset = setConstantOffset(stringConstants, offset);

    // At this point, the position of all symbols will be known. Back fill.
    backfill();

    // Generate code
    StringJoiner joiner = new StringJoiner("\n");
    joiner.add(generate(new InstructionGenerator(), instructions.values()));
    joiner.add(generate(new IntegerConstantGenerator(), integerConstants));
    joiner.add(generate(new FloatConstantGenerator(), floatConstants));
    joiner.add(generate(new StringConstantGenerator(), stringConstants));

    return joiner.toString();
  }

  /**
   * Generate code.
   * @param generator Generator to use.
   * @param collection Collection to seed to generator.
   * @return A string containing generated code and preceding size.
   */
  private <T, G extends Generator<T>> String generate(G generator, Collection<T> collection) {
    StringJoiner joiner = new StringJoiner("\n");
    generator.generate(collection);

    // Generate size and body
    joiner.add(generator.getSize() + "");
    String body = generator.getBody();
    if (body.length() > 0) {
      joiner.add(generator.getBody());
    }

    return joiner.toString();
  }

  public void printDebug() {
    System.out.println("\n==================");
    System.out.println("CODE MANAGER DEBUG");
    System.out.println("Number of instructions: " + instructions.size());
    System.out.println("Instructions:");

    for (Instruction instruction : instructions.values()) {
      System.out.println(instruction.toString());
    }

    System.out.println("==================");
  }
}
