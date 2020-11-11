package cd20.parser;

import cd20.scanner.Token;

public class SemanticException extends ParserException {
  public SemanticException(String message, Token token) {
    super("Semantic Error: " + message, token);
  }
}
