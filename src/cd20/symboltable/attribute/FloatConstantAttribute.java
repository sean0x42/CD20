package cd20.symboltable.attribute;

public class FloatConstantAttribute implements Attribute {
  private final float constant;

  public FloatConstantAttribute(String constant) {
    this.constant = Float.parseFloat(constant);
  }

  public float getConstant() {
    return constant;
  }

  @Override
  public String toString() {
    return String.format("Float constant = %f", constant);
  }
}
