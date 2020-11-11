package cd20.parser;

import cd20.scanner.Token;

public class SyntaxException extends ParserException {
  public SyntaxException(String message, Token token) {
    super("Syntax error: " + message, token);
  }
}
