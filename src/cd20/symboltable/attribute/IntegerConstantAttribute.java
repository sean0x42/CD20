package cd20.symboltable.attribute;

public class IntegerConstantAttribute implements Attribute {
  private final int constant;

  public IntegerConstantAttribute(String constant) {
    this.constant = Integer.parseInt(constant);
  }

  public int getConstant() {
    return constant;
  }
}
