package cd20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A lexer for CD20
 */
public class Scanner {
  private PeekableReader reader;
  private int line = 0;
  private int column = 0;
  private Character character;

  public Scanner(Reader reader) {
    this.reader = new PeekableReader(new BufferedReader(reader));
  }

  /**
   * Determines whether end of file has been reached
   */
  public boolean eof() throws IOException {
    return reader.peek() == null;
  }

  /**
   * Return the next token
   */
  public Token nextToken() throws IOException {
    // Start State
    consumeWhitespace();

    // Consume the next character, ready to determine which way to parse
    consumeChar();

    // Handle EOF
    if (character == null) {
      return new Token(TokenType.EOF, line, column);
    }

    // Handle string literal
    if (character == '"') {
      return parseString();
    }

    // Handle int/float literal
    if (Character.isDigit(character)) {
      return parseNumber();
    }

    // Handle possible identifier
    if (Character.isLetter(character)) {
      return parseIdentifer();
    }

    // Handle operator/delimiter
    TokenType operatorType = parseOperator();
    if (operatorType != null) {
      return new Token(operatorType, line, column);
    }

    // Undefined
    return new Token(TokenType.UNDEFINED, consumeWord(), line, column);
  }

  /**
   * Consume an entire string literal
   */
  private Token parseString() throws IOException {
    // Track line & column before consuming more
    int line = this.line;
    int column = this.column;
    String string = "" + character;

    while (true) {
      Character ch = consumeChar();

      // Check for EOF
      if (ch == null) {
        return new Token(TokenType.UNDEFINED, string, line, column);
      }

      string += ch;

      // Check for newline mid string
      if (ch == '\n') {
        return new Token(TokenType.UNDEFINED, string, line, column);
      }

      // End of string is reached
      if (ch == '"') {
        break;
      }
    }

    return new Token(TokenType.STRING_LITERAL, string, line, column);
  }

  /**
   * Consume and parse an integer or float literal.
   */
  private Token parseNumber() throws IOException {
    // Track line & column before consuming more
    int line = this.line;
    int column = this.column;

    // Record state
    boolean isInvalid = false;
    boolean isReal = false;

    String string = "" + character;

    while (reader.peek() != null && reader.peek() != ';' && !Character.isWhitespace(reader.peek())) {
      Character ch = consumeChar();
      string += ch;

      // Handle non-digit
      if (ch != '.' && !Character.isDigit(ch)) {
        isInvalid = true;
      }

      if (ch == '.') {
        isReal = true;
      }
    }

    if (isInvalid) {
      return new Token(TokenType.UNDEFINED, string, line, column);
    }

    if (isReal) {
      return new Token(TokenType.REAL, string, line, column);
    }

    return new Token(TokenType.INTEGER_LITERAL, string, line, column);
  }

  /**
   * Parses an identifier/keyword.
   */
  private Token parseIdentifer() throws IOException {
    // Track line & column before consuming more
    int line = this.line;
    int column = this.column;

    // Record state
    boolean isInvalid = false;

    String identifier = "" + character;
    Character nextChar = reader.peek();

    // Consume entire identifier first
    while (nextChar != null && nextChar != ';' && !Character.isWhitespace(nextChar)) {
      if (!Character.isLetterOrDigit(nextChar)) {
        isInvalid = true;
      }

      identifier += consumeChar();
      nextChar = reader.peek();
    }

    // Handle invalid
    if (isInvalid) {
      return new Token(TokenType.UNDEFINED, identifier, line, column);
    }

    // Identify keywords from identifiers
    TokenType keyword = identifyKeyword(identifier);
    if (keyword != null) {
      return new Token(keyword, line, column);
    }

    return new Token(TokenType.IDENTIFIER, identifier, line, column);
  }

  /**
   * Consume a single character.
   * @return Consumed character.
   */
  private Character consumeChar() throws IOException {
    // Previous char was a newline, drop to the next line
    if (line == 0 || character == '\n') {
      line++;
      column = 0;
    }
    
    character = reader.read();
    if (character != null) {
      column++;
    }

    return character;
  }

  /**
   * Consumes characters in the reader until a non-whitespace character is
   * found.
   *
   * Note: this function will also automatically consume any comments.
   */
  private void consumeWhitespace() throws IOException {
    while (true) {
      Character nextChar = reader.peek();

      // Handle EOF
      if (nextChar == null) {
        return;
      }

      // Handle whitespace
      if (Character.isWhitespace(nextChar)) {
        consumeChar();
        continue;
      }

      // Handle single line comment
      if (nextChar == '/' && reader.peek(2) == '-' && reader.peek(3) == '-') {
        consumeLine();
        continue;
      }

      // Handle multi-line comment
      if (nextChar == '/' && reader.peek(2) == '*' && reader.peek(3) == '*') {
        consumeUntilCommentEnd();
        continue;
      }

      return;
    }
  }

  /**
   * Consumes all characters until the end of a comment is reached.
   */
  private void consumeUntilCommentEnd() throws IOException {
    do {
      consumeChar();
    } while (
      reader.peek() != null &&
      !(reader.peek(1) == '*' && reader.peek(2) == '*' && reader.peek(3) == '/')
    );

    consumeChar();
    consumeChar();
    consumeChar();
  }

  /**
   * Consume one word (until whitespace is found)
   * @return Consumed word.
   */
  private String consumeWord() throws IOException {
    String word = "" + character;

    while (reader.peek() != null && reader.peek() != ';' && !Character.isWhitespace(reader.peek())) {
      word += consumeChar();
    }

    return word;
  }

  /**
   * Consumes the remainder of the current line.
   */
  private void consumeLine() throws IOException {
    do {
      consumeChar();
    } while (character != null && character != '\n');
  }

  /**
   * Parse the current operator or delimiter (if found).
   *
   * If an operator or delimiter is found, its {@link TokenType} would be
   * returned. Otherwise null.
   *
   * @throws IOException
   */
  private TokenType parseOperator() throws IOException {
    switch (character) {
      case ',':
        return TokenType.COMMA;
      case '[':
        return TokenType.LEFT_BRACKET;
      case ']':
        return TokenType.RIGHT_BRACKET;
      case '(':
        return TokenType.LEFT_PAREN;
      case ')':
        return TokenType.RIGHT_PAREN;
      case '=':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.EQUALS_EQUALS;
        }

        return TokenType.EQUALS;
      case '+':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.INCREMENT;
        }

        return TokenType.PLUS;
      case '-':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.DECREMENT;
        }

        return TokenType.MINUS;
      case '*':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.STAR_EQUALS;
        }

        return TokenType.STAR;
      case '/':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.DIVIDE_EQUALS;
        }

        return TokenType.DIVIDE;
      case '%':
        return TokenType.PERCENT;
      case '^':
        return TokenType.CARAT;
      case '<':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.LESS_OR_EQUAL;
        }

        return TokenType.LESS;
      case '>':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.GREATER_OR_EQUAL;
        }

        return TokenType.GREATER;
      case ':':
        return TokenType.COLON;
      case '!':
        if (reader.peek() == '=') {
          consumeChar();
          return TokenType.NOT_EQUAL;
        }

        // TODO This may not be correct
        return TokenType.BANG;
      case ';':
        return TokenType.SEMI_COLON;
      case '.':
        return TokenType.DOT;
      default:
        return null;
    }
  }

  private TokenType identifyKeyword(String identifier) {
    switch (identifier.toLowerCase()) {
      case "cd20":
        return TokenType.CD20;
      case "constants":
        return TokenType.CONSTANTS;
      case "types":
        return TokenType.TYPES;
      case "is":
        return TokenType.IS;
      case "arrays":
        return TokenType.ARRAYS;
      case "main":
        return TokenType.MAIN;
      case "begin":
        return TokenType.BEGIN;
      case "end":
        return TokenType.END;
      case "array":
        return TokenType.ARRAY;
      case "of":
        return TokenType.OF;
      case "func":
        return TokenType.FUNC;
      case "void":
        return TokenType.VOID;
      case "const":
        return TokenType.CONST;
      case "int":
        return TokenType.INT;
      case "real":
        return TokenType.REAL;
      case "bool":
        return TokenType.BOOL;
      case "for":
        return TokenType.FOR;
      case "repeat":
        return TokenType.REPEAT;
      case "until":
        return TokenType.UNTIL;
      case "if":
        return TokenType.IF;
      case "else":
        return TokenType.ELSE;
      case "input":
        return TokenType.INPUT;
      case "print":
        return TokenType.PRINT;
      case "println":
        return TokenType.PRINTLN;
      case "return":
        return TokenType.RETURN;
      case "not":
        return TokenType.NOT;
      case "and":
        return TokenType.AND;
      case "or":
        return TokenType.OR;
      case "xor":
        return TokenType.XOR;
      case "true":
        return TokenType.TRUE;
      case "false":
        return TokenType.FALSE;
      default:
        return null;
    }
  }
}
