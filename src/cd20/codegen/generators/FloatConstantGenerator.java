package cd20.codegen.generators;

import java.util.Collection;

import cd20.codegen.Constant;

public class FloatConstantGenerator extends LineGenerator<Constant<Float>> {
  /**
   * Generate code for a collection of floating point constants.
   * @param constants Floating point constants to generate code for.
   */
  public void generate(Collection<Constant<Float>> constants) {
    for (Constant<Float> constant : constants) {
      super.insertLine(constant.getValue().toString());
    }
  }
}
