package cd20.codegen.generators;

import java.util.Collection;

import cd20.codegen.Constant;
import cd20.codegen.Operation;

public class StringConstantGenerator extends WordFilledGenerator<Constant<String>> {
  /**
   * Generate code for string constants.
   * @param symbols Symbols to generate code for.
   */
  public void generate(Collection<Constant<String>> symbols) {
    // Insert each string
    for (Constant<String> symbol : symbols) {
      String string = symbol.getValue();

      // Insert each character
      for (byte charByte : string.getBytes()) {
        insertByte(charByte);
      }

      insertByte(Operation.HALT.getCode());
    }

    super.fill();
  }
}
