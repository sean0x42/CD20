package cd20.codegen.generators;

import java.util.Collection;

import cd20.codegen.Constant;

public class IntegerConstantGenerator extends LineGenerator<Constant<Integer>> {
  /**
   * Generate code for integer constants.
   * @param constants Integer constants to generate code for.
   */
  public void generate(Collection<Constant<Integer>> constants) {
    for (Constant<Integer> constant : constants) {
      super.insertLine(constant.getValue().toString());
    }
  }
}
