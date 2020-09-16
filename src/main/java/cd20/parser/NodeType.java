package cd20.parser;

/**
 * CD20 AST {@link Node} type.
 */
public enum NodeType {
  PROGRAM               ("NPROG"),
  GLOBALS               ("NGLOB"),
  INIT_LIST             ("NILIST"),
  INIT                  ("NINIT"),
  FUNCTIONS             ("NFUNCS"),
  MAIN                  ("NMAIN"),
  SDECL_LIST            ("NSDLST"),
  TYPE_LIST             ("NTYPEL"),
  STRUCT_DEF            ("NRTYPE"),
  ARRAY_DEF             ("NATYPE"),
  STRUCT_FIELDS         ("NFLIST"),
  SDECL                 ("NSDECL"),
  ARRAY_DECLS           ("NALIST"),
  ARRAY_DECL            ("NARRD"),
  FUNCTION_DEF          ("NFUND"),
  PARAM_LIST            ("NPLIST"),
  SIMPLE_PARAM          ("NSIMP"),
  ARRAY_PARAM           ("NARRP"),
  CONST_ARRAY_PARAM     ("NARRC"),
  DECL_LIST             ("NDLIST"),
  STATEMENTS            ("NSTATS"),
  FOR                   ("NFOR"),
  REPEAT                ("NREPT"),
  ASSIGN_LIST           ("NASGNS"),
  IF                    ("NIFTH"),
  IF_ELSE               ("NIFTE"),
  ASSIGN                ("NASGN"),
  INCREMENT             ("NPLEQ"),
  DECREMENT             ("NMNEQ"),
  STAR_EQUALS           ("NSTEQ"),
  DIVIDE_EQUALS          ("NDVEQ"),
  INPUT                 ("NINPUT"),
  PRINT                 ("NPRINT"),
  PRINTLN               ("NPRLN"),
  FUNCTION_CALL         ("NCALL"),
  RETURN                ("NRETN"),
  VARIABLE_LIST         ("NVLIST"),
  SIMPLE_VARIABLE       ("NSIMV"),
  ARRAY_VARIABLE        ("NARRV"),
  EXPRESSION_LIST       ("NEXPL"),
  BOOLEAN               ("NBOOL"),
  NOT                   ("NNOT"),
  AND                   ("NAND"),
  OR                    ("NOR"),
  XOR                   ("NXOR"),
  EQUAL                 ("NEQL"),
  NOT_EQUAL             ("NNEQ"),
  GREATER               ("NGRT"),
  GREATER_OR_EQUAL      ("NGEQ"),
  LESS                  ("NLSS"),
  LESS_OR_EQUAL         ("NLEQ"),
  ADD                   ("NADD"),
  SUBTRACT              ("NSUB"),
  MULTIPLY              ("NMUL"),
  DIVIDE                ("NDIV"),
  MODULO                ("NMOD"),
  POWER                 ("NPOW"),
  INTEGER_LITERAL       ("NILIT"),
  REAL_LITERAL          ("NFLIT"),
  TRUE                  ("NTRUE"),
  FALSE                 ("NFALSE"),
  FUNC_CALL             ("NFCALL"),
  PRINT_LIST            ("NPRLST"),
  STRING                ("NSTRG");

  private final String node;

  NodeType(String node) {
    this.node = node;
  }

  @Override
  public String toString() {
    return node;
  }
}
