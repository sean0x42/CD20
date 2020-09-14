package cd20.scanner;

public enum TokenType {
  EOF              ("T_EOF"),

  // 30 Keywords
  CD20             ("TCD20"),
  CONSTANTS        ("TCONS"),
  TYPES            ("TTYPS"),
  IS               ("TTTIS"),
  ARRAYS           ("TARRS"),
  MAIN             ("TMAIN"),
  BEGIN            ("TBEGN", "'begin'"),
  END              ("TTEND", "'end'"),
  ARRAY            ("TARAY"),
  OF               ("TTTOF"),
  FUNC             ("TFUNC"),
  VOID             ("TVOID"),
  CONST            ("TCNST"),
  INT              ("TINTG"),
  REAL             ("TREAL"),
  BOOL             ("TBOOL"),
  FOR              ("TTFOR"),
  REPEAT           ("TREPT"),
  UNTIL            ("TUNTL"),
  IF               ("TIFTH"),
  ELSE             ("TELSE"),
  INPUT            ("TINPT"),
  PRINT            ("TPRIN"),
  PRINTLN          ("TPRLN"),
  RETURN           ("TRETN"),
  NOT              ("TNOTT"),
  AND              ("TTAND"),
  OR               ("TTTOR"),
  XOR              ("TTXOR"),
  TRUE             ("TTRUE"),
  FALSE            ("TFALS"),

  // Operators and delimiters
  COMMA            ("TCOMA"),
  LEFT_BRACKET     ("TLBRK", "'['"),
  RIGHT_BRACKET    ("TRBRK", "']'"),
  LEFT_PAREN       ("TLPAR"),
  RIGHT_PAREN      ("TRPAR"),
  EQUALS           ("TEQUL"),
  PLUS             ("TPLUS"),
  MINUS            ("TMINS"),
  STAR             ("TSTAR"),
  DIVIDE           ("TDIVD"),
  PERCENT          ("TPERC"),
  CARAT            ("TCART"),
  LESS             ("TLESS"),
  GREATER          ("TGRTR"),
  COLON            ("TCOLN"),
  LESS_OR_EQUAL    ("TLEQL"),
  GREATER_OR_EQUAL ("TGEQL"),
  NOT_EQUAL        ("TNEQL"),
  EQUALS_EQUALS    ("TEQEQ"),
  INCREMENT        ("TPLEQ"),
  DECREMENT        ("TMNEQ"),
  STAR_EQUALS      ("TSTEQ"),
  DIVIDE_EQUALS    ("TDVEQ"),
  BANG             ("TBANG"),
  SEMI_COLON       ("TSEMI"),
  DOT              ("TDOTT"),

  // Tokens that need tuple values
  IDENTIFIER       ("TIDEN", "identifier"),
  INTEGER_LITERAL  ("TILIT", "integer"),
  FLOAT_LITERAL    ("TFLIT", "float"),
  STRING_LITERAL   ("TSTRG", "string"),
  UNDEFINED        ("TUNDF");

  private final String token;
  private final String humanReadable;

  TokenType(String token) {
    this(token, null);
  }

  TokenType(String token, String humanReadable) {
    this.token = token;
    this.humanReadable = humanReadable;
  }

  @Override
  public String toString() {
    return this.token;
  }

  public String getHumanReadable() {
    if (humanReadable == null) {
      return toString();
    }

    return humanReadable;
  }
}
