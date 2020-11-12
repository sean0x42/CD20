package cd20.parser;

public class DataType {
  private final String type;

  public DataType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public boolean isInteger() {
    return type.equals("int");
  }

  public boolean isReal() {
    return type.equals("real");
  }

  public boolean isBoolean() {
    return type.equals("bool");
  }

  public boolean isString() {
    return type.equals("string");
  }

  public boolean isVoid() {
    return type.equals("void");
  }

  public boolean isStruct() {
    return type.startsWith("__struct__"); 
  }

  public boolean isArray() {
    return type.startsWith("__array__");
  }

  public boolean isNumeric() {
    return isReal() || isInteger();
  }

  public boolean equals(DataType type) {
    return type.getType().equals(getType());
  }

  @Override
  public String toString() {
    return getType();
  }
}
