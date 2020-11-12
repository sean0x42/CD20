package cd20.symboltable.attribute;

public class StringConstantAttribute implements Attribute {
  private final String constant;

  public StringConstantAttribute(String constant) {
    this.constant = constant;
  }

  public String getConstant() {
    return constant;
  }

  @Override
  public String toString() {
    return String.format("String constant \"%s\"", constant);
  }
}
