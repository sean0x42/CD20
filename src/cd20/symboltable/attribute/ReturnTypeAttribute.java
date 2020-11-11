package cd20.symboltable.attribute;

import cd20.parser.SType;

public class ReturnTypeAttribute implements Attribute {
  private final SType type;

  public ReturnTypeAttribute(SType type) {
    this.type = type;
  }

  public SType getType() {
    return type;
  }
}
