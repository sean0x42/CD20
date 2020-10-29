package cd20.scanner;

public enum TokenType {
  EOF              ("T_EOF", "end of file"),

  // 30 Keywords
  CD20             ("TCD20", "CD20"),
  CONSTANTS        ("TCONS", "'constants'"),
  TYPES            ("TTYPS", "'types'"),
  IS               ("TTTIS", "'is'"),
  ARRAYS           ("TARRS", "'arrays'"),
  MAIN             ("TMAIN", "'main'"),
  BEGIN            ("TBEGN", "'begin'"),
  END              ("TTEND", "'end'"),
  ARRAY            ("TARAY", "'array'"),
  OF               ("TTTOF"),
  FUNC             ("TFUNC"),
  VOID             ("TVOID"),
  CONST            ("TCNST"),
  INT              ("TINTG"),
  REAL             ("TREAL", "'real'"),
  BOOL             ("TBOOL"),
  FOR              ("TTFOR", "'for'"),
  REPEAT           ("TREPT", "'repeat'"),
  UNTIL            ("TUNTL", "'until'"),
  IF               ("TIFTH", "'if'"),
  ELSE             ("TELSE", "'else'"),
  INPUT            ("TINPT", "'input'"),
  PRINT            ("TPRIN", "'print'"),
  PRINTLN          ("TPRLN", "'println'"),
  RETURN           ("TRETN", "'return'"),
  NOT              ("TNOTT"),
  AND              ("TTAND"),
  OR               ("TTTOR"),
  XOR              ("TTXOR"),
  TRUE             ("TTRUE", "'true'"),
  FALSE            ("TFALS", "'false'"),

  // Operators and delimiters
  COMMA            ("TCOMA"),
  LEFT_BRACKET     ("TLBRK", "'['"),
  RIGHT_BRACKET    ("TRBRK", "']'"),
  LEFT_PAREN       ("TLPAR", "'('"),
  RIGHT_PAREN      ("TRPAR", "')'"),
  ASSIGN           ("TEQUL", "'='"),
  PLUS             ("TPLUS", "'+'"),
  MINUS            ("TMINS", "'-'"),
  STAR             ("TSTAR", "'*'"),
  DIVIDE           ("TDIVD", "'/'"),
  PERCENT          ("TPERC", "'%'"),
  CARAT            ("TCART", "'^'"),
  LESS             ("TLESS", "'<'"),
  GREATER          ("TGRTR", "'>'"),
  COLON            ("TCOLN", "':'"),
  LESS_OR_EQUAL    ("TLEQL", "'<='"),
  GREATER_OR_EQUAL ("TGEQL", "'>='"),
  NOT_EQUAL        ("TNEQL", "'!='"),
  EQUALS_EQUALS    ("TEQEQ", "'=='"),
  INCREMENT        ("TPLEQ", "'+='"),
  DECREMENT        ("TMNEQ", "'-='"),
  STAR_EQUALS      ("TSTEQ", "'*='"),
  DIVIDE_EQUALS    ("TDVEQ", "'/='"),
  BANG             ("TBANG", "'!'"),
  SEMI_COLON       ("TSEMI", "';'"),
  DOT              ("TDOTT", "'.'"),

  // Tokens that need tuple values
  IDENTIFIER       ("TIDEN", "identifier"),
  INTEGER_LITERAL  ("TILIT", "integer"),
  FLOAT_LITERAL    ("TFLIT", "float"),
  STRING_LITERAL   ("TSTRG", "string"),
  UNDEFINED        ("TUNDF", "unknown token");

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
