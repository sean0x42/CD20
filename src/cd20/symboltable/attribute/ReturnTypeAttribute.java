package cd20.symboltable.attribute;

import cd20.parser.DataType;

public class ReturnTypeAttribute implements Attribute {
  private final DataType type;

  public ReturnTypeAttribute(DataType type) {
    this.type = type;
  }

  public DataType getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("return %s", type.toString());
  }
}
