package cd20.output;

import cd20.scanner.Token;

public class WarningAnnotation extends Annotation {
  public WarningAnnotation(String message, Token token) {
    super("Warning: " + message, token);
  }
}
