package cd20.parser;

import cd20.scanner.Token;
import cd20.scanner.TokenType;

public class UnexpectedTokenException extends SyntaxException {
  public UnexpectedTokenException(TokenType expected, Token found) {
    super(
        String.format(
          "Expected %s but found %s",
          expected.getHumanReadable(),
          found.getType().getHumanReadable()
        ),
        found
    );
  }

  public UnexpectedTokenException(String expected, Token found) {
    super(
        String.format(
          "Error: Expected %s but found %s",
          expected,
          found.getHumanReadable()
        ),
        found
    );
  }
}
