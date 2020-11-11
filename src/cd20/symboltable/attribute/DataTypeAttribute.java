package cd20.symboltable.attribute;

import cd20.parser.DataType;

public class DataTypeAttribute implements Attribute {
  private DataType type;

  public DataTypeAttribute(DataType type) {
    this.type = type;
  }

  public DataType getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format("Data type = %s", type.toString());
  }
}
