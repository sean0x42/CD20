package cd20.codegen;

import cd20.ByteUtils;
import cd20.symboltable.Symbol;

/**
 * An instruction that requires backfilling to get a Symbol's address.
 */
public class BackfillInstruction extends Instruction {
  private final Symbol symbol;

  /**
   * Construct a new backfill instruction.
   * @param symbol Symbol address to fill in later.
   * @param operation Operation to perform with symbol address.
   */
  public BackfillInstruction(Symbol symbol, Operation operation) {
    super(operation);
    this.symbol = symbol;
  }

  /**
   * Backfill this instruction to include the address of the given symbol.
   * Note: this function must be called after all symbol addresses are known.
   */
  public void backfill() {
    // Determine whether this should be a load address or load value
    boolean isLoadAddress = this.getOperation() == Operation.PLACEHOLDER_LA;
    Operation operation;

    if (symbol.getRegister() == null || symbol.getOffset() == Integer.MIN_VALUE) {
      throw new RuntimeException(String.format(
        "Encountered a symbol that was not ready to be backfilled. (%s) [%s]",
        this.toString(),
        symbol.toString()
      ));
    }

    // Determine which base register
    switch (symbol.getRegister()) {
      case CONSTANTS:
        operation = isLoadAddress ? Operation.LA0 : Operation.LV0;
        break;
      case GLOBALS:
        operation = isLoadAddress ? Operation.LA1 : Operation.LV1;
        break;
      case DECLARATIONS:
        operation = isLoadAddress ? Operation.LA2 : Operation.LV2;
        break;
      default:
        throw new UnsupportedOperationException();
    }

    this.setOperation(operation);
    this.setOperands(ByteUtils.toByteArray(symbol.getOffset()));
  }

  /**
   * Get size of instruction.
   * Note that a backfilled instruction is always 5 bytes long.
   */
  @Override
  public int getSize() {
    return 5;
  }
}
