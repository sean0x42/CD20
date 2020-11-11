package cd20.codegen;

import cd20.StringUtils;

/**
 * An enum containing all possible SM20 instruction types.
 *
 * Each type has been annotated with a docstring, which provides helpful
 * instructions for using that operating.
 *
 * Use your editor to see these docstrings in context.
 */
public enum Operation {
  /**
   * Stops the program currently running.
   */
  HALT(0),

  /**
   * No op.
   */
  NOOP(1),

  /**
   * Abort the program.
   */
  TRAP(2),

  /**
   * Integer Zero to TOS.
   */
  ZERO(3),

  /**
   * Boolean False to TOS.
   */
  FALSE(4),

  /**
   * Boolean True to TOS.
   */
  TRUE(5),

  /**
   * Toggle arithmetic type.
   * i.e. INTG to FLOT, or FLOT to INTG
   */
  TYPE(7),

  /**
   * Set TOS type to INTG.
   */
  ITYPE(8),

  /**
   * Set TOS type to FLOT.
   */
  FTYPE(9),
  
  /**
   * Add.
   * 1. Pop 2 entries.
   * 2. Add
   * 3. Push result
   */
  ADD(11),

  /**
   * Subtract.
   * 1. Pop 2 entries.
   * 2. Subtract TOS from second (e.g. b - a)
   * 3. Push result
   */
  SUB(12),
  
  /**
   * Multiply.
   * 1. Pop 2 entries.
   * 2. Multiply
   * 3. Push
   */
  MUL(13),

  /**
   * Divide.
   * 1. Pop 2 entries.
   * 2. Divide second by TOS (b / a)
   * 3. Push
   */
  DIV(14),

  /**
   * Get the remainder from the last DIV op???
   */
  REM(15),

  POWER                 (16),

  /**
   * Change sign? Then push back to the stack.
   */
  CHS(17),

  /**
   * Absolute value.
   * 1. Pop TOS
   * 2. Take absolute value
   * 3. Push result
   */
  ABS(18),

  /**
   * Greater than 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  GT(21),

  /**
   * Greater than or equal to 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  GE(22),

  /**
   * Less than 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  LT(23),

  /**
   * Less than or equal to 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  LE(24),

  /**
   * Equal to 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  EQ(25),

  /**
   * Not equal to 0.
   * 1. Pop TOS
   * 2. Compare to 0
   * 3. Push true/false
   */
  NE(26),

  /**
   * Pops 2 entries, performs AND, pushes the result.
   */
  AND(31),

  /**
   * Pops 2 entries, performs OR, pushes the result.
   */
  OR(32),

  /**
   * Pops 2 entries, performs XOR, pushes the result.
   */
  XOR(33),

  /**
   * Pop 1 entry, negate, and push.
   */
  NOT(34),

  /**
   * Branch true.
   * 1. Pop TOS.
   * 2. If top is true, then pop second entry and load into the PC.
   */
  BT(35),

  /**
   * Branch false.
   * 1. Pop TOS.
   * 2. If top is false, then pop second entry and load into the PC.
   */
  BF(36),

  /**
   * Branch.
   * 1. Pop TOS.
   * 2. Load address into PC
   */
  BR(37),

  /**
   * Load.
   * 1. Pops the top entry
   * 2. Get memory contents at address
   * 3. Push contents
   */
  L(40),

  /**
   * Load a byte.
   * 1. Fetch next instruction byte
   * 2. Sign and extend to 64 bits
   * 3. Push onto stack as type INTG
   */
  LB(41),

  /**
   * Load half word.
   * 1. Fetch next 2 instruction bytes
   * 2. Sign and extend to 64 bits
   * 3. Push onto stack as type INTG
   */
  LH(42),

  /**
   * Store.
   * 1. Pop 2 entries
   * 2. Store the value at TOS into address given by second
   */
  ST(43),

  /**
   * A special no operand form of ALLOC for a single word.
   */
  STEP(51),

  /**
   * Allocates a variable.
   * 1. Pop TOS
   * 2. Multiply by 8
   * 3. Add to Stack Ptr
   */
  ALLOC(52),

  /**
   * Construct an array descriptor and step stack pointer by size.
   */
  ARRAY(53),

  /**
   * Get address of an array element at an index.
   * 1. Pop 2 entries
   * 2. Use first as array index
   * 3. Use second entry as array descriptor
   */
  INDEX(54),

  /**
   * Get size of an array from a descriptor.
   */
  SIZE(55),

  /**
   * Duplicate.
   * 1. Pop top entry
   * 2. Push
   * 3. Push
   */
  DUP(56),

  /**
   * Read a float from standard input onto stack.
   */
  READF(60),

  /**
   * Read an integer from standard input onto stack.
   */
  READI(61),

  /**
   * Print value to stdout.
   */
  VALPR(62),

  /**
   * Print string constant to stdout.
   * String start address must be on stack.
   */
  STRPR(63),

  /**
   * Print single character to stdout.
   * Char address must be on stack.
   */
  CHRPR(64),

  /**
   * Prints a newline character to stdout.
   */
  NEWLN(65),

  /**
   * Prints a space character to stdout.
   */
  SPACE(66),

  /**
   * Setup return value for function.
   */
  RVAL(70),

  /**
   * Return from subprogram.
   */
  RETN(71),

  /**
   * Jump subroutine 2.
   */
  JS2(72),

  /**
   * Load a value from the instruction area (read only space).
   * Program and constants live within this area.
   *
   * Takes one operand. A 32 bit offset
   */
  LV0(80),

  /**
   * Load a value from the stack area.
   * Variables are to be allocated in this area.
   *
   * Takes one operand. A 32 bit offset
   */
  LV1(81),

  /**
   * Load a value from area 2.
   * Procedures and functions live in this area.
   *
   * Takes one operand. A 32 bit offset
   */
  LV2(82),

  /**
   * Load a value.
   * The exact base and offset will be unknown at this point.
   */
  PLACEHOLDER_LV(100),

  /**
   * Load address from area 0.
   * 1. Push address onto stack.
   */
  LA0(90),

  /**
   * Load address from area 1.
   * 1. Push address onto stack.
   */
  LA1(91),

  /**
   * Load address from area 2.
   * 1. Push address onto stack.
   */
  LA2(92),

  /**
   * Load address.
   * The exact base and offset will be unknown at this point.
   */
  PLACEHOLDER_LA(110);

  private final int code;

  Operation(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  @Override
  public String toString() {
    return StringUtils.leftPad(2, code + "");
  }
}
