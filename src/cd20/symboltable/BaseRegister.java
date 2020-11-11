package cd20.symboltable;

/**
 * An enum containing all available base register locations.
 */
public enum BaseRegister {
  CONSTANTS(0),
  GLOBALS(1),
  DECLARATIONS(2);

  private final int baseId;

  BaseRegister(int baseId) {
    this.baseId = baseId;
  }

  public int getId() {
    return baseId;
  }
}
