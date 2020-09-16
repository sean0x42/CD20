package cd20.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cd20.output.Annotation;
import cd20.output.OutputController;
import cd20.scanner.Scanner;
import cd20.scanner.Token;
import cd20.scanner.TokenType;

/**
 * A top down recursive parser for CD20
 *
 * Turns a stream of tokens into an AST.
 */
public class Parser {
  private final OutputController output;
  private final Scanner scanner;
  private final SymbolManager symbolTable;

  private Token nextToken;
  private Node rootNode;

  public Parser(Scanner scanner, OutputController output) {
    this.scanner = scanner;
    this.output = output;
    this.symbolTable = new SymbolManager();
  }

  /**
   * Begin parsing a CD20 program.
   * @return An abstract syntax tree.
   */
  public Node parse() throws IOException {
    nextToken = scanner.nextToken();

    try {
      return parseProgram();
    } catch (UnexpectedTokenException exception) {
      output.addAnnotation(
        new Annotation(exception.getFoundToken(), exception.getMessage())
      );
    }

    return null;
  }

  /**
   * Expect a particular {@link TokenType}.
   *
   * If a {@link Token} of that {@link TokenType} is not provided next, throws
   * an exception that cannot be recovered from.
   */
  private void expect(TokenType type) throws UnexpectedTokenException {
    // Handle unexpected token
    if (nextToken.getType() != type) {
      throw new UnexpectedTokenException(type, nextToken);
    }
  }

  /**
   * Expects and consumes an identifier, and returns consumed lexeme.
   */
  private String expectIdentifier() throws IOException, UnexpectedTokenException {
    String lexeme = nextToken.getLexeme();
    expectAndConsume(TokenType.IDENTIFIER);
    return lexeme;
  }

  /**
   * Expects and consumes the next {@link Token}.
   */
  private void expectAndConsume(TokenType type) throws IOException, UnexpectedTokenException {
    expect(type);
    consume();
  }

  /**
   * Determine whether the next token is of the given {@link TokenType}.
   */
  private boolean isNext(TokenType type) {
    return nextToken.getType() == type;
  }

  /**
   * Move to the next available {@link Token}
   */
  private void consume() throws IOException {
    nextToken = scanner.nextToken();
  }

  /**
   * Parses the a CD20 program node
   * @return A {@link Node} of type PROGRAM
   */
  private Node parseProgram() throws IOException, UnexpectedTokenException {
    symbolTable.pushScope("global");

    // Handle CD20 <id>
    expectAndConsume(TokenType.CD20);
    rootNode = new Node(NodeType.PROGRAM, expectIdentifier());

    // Handle <globals><funcs><main>
    rootNode.setNextChild(parseGlobals());
    rootNode.setNextChild(parseFunctions());
    rootNode.setNextChild(parseMain());

    symbolTable.popScope();
    return rootNode;
  }

  /**
   * Parses a globals node
   * @return A {@link Node} of type GLOBALS
   */
  private Node parseGlobals() throws IOException, UnexpectedTokenException {
    Node globals = new Node(NodeType.GLOBALS);

    // Handle <consts><types><arrays>
    globals.setNextChild(parseConstants());
    globals.setNextChild(parseTypes());
    globals.setNextChild(parseArrays());

    return globals;
  }

  /**
   * Parse a functions node.
   * @return A {@link Node} of type FUNCTIONS or null
   */
  private Node parseFunctions() throws IOException, UnexpectedTokenException {
    // Only continue if a function is given
    if (!isNext(TokenType.FUNC)) return null;

    Node functions = new Node(NodeType.FUNCTIONS);
    functions.setNextChild(parseFunction());
    functions.setNextChild(parseFunctions());

    return functions;
  }

  /**
   * Parse a function.
   * @return A {@link Node} of type FUNC
   */
  private Node parseFunction() throws IOException, UnexpectedTokenException {
    // Handle func <id>
    expectAndConsume(TokenType.FUNC);
    String lexeme = expectIdentifier();
    Node func = new Node(NodeType.FUNCTION_DEF, lexeme);
    symbolTable.pushScope(String.format("%s_function", lexeme));

    // Handle (<plist>):
    expectAndConsume(TokenType.LEFT_PAREN);
    func.setNextChild(parseParamList());
    expectAndConsume(TokenType.RIGHT_PAREN);
    expectAndConsume(TokenType.COLON);

    // Handle <rtype><funcbody>
    func.setNextChild(parseReturnType());
    for (Node node : parseFunctionBody()) {
      func.setNextChild(node);
    }

    symbolTable.popScope();
    return func;
  }

  /**
   * Parse a function return type.
   * @return A {@link Node} of type SType or null (for void functions).
   */
  private Node parseReturnType() throws IOException, UnexpectedTokenException {
    // Handle void
    if (isNext(TokenType.VOID)) {
      consume();
      return null;
    }

    return parseSType();
  }

  /**
   * Parse a function body.
   * May include a node of locals and a node of statements.
   * @return A list of {@link Node}s that comprise a function's body.
   */
  private List<Node> parseFunctionBody() throws IOException, UnexpectedTokenException {
    List<Node> nodes = new ArrayList<>();

    // Handle <locals>
    Node locals = parseLocals();
    if (locals != null) {
      nodes.add(locals);
    }
    
    // Handle begin <stats> end
    expectAndConsume(TokenType.BEGIN);
    nodes.add(parseStatements());
    expectAndConsume(TokenType.END);

    return nodes;
  }

  /**
   * Parse locals.
   */
  private Node parseLocals() throws UnexpectedTokenException, IOException {
    if (isNext(TokenType.IDENTIFIER)) {
      return parseDeclarationList();
    }

    return null;
  }

  /**
   * Parse declaration list.
   */
  private Node parseDeclarationList() throws UnexpectedTokenException, IOException {
    Node decl = parseDeclaration();
    Node chain = parseOptDeclarationList();

    if (chain != null) {
      Node node = new Node(NodeType.DECL_LIST);
      node.setLeftChild(decl);
      node.setRightChild(decl);
      return node;
    }

    return decl;
  }

  /**
   * Parse optionally more declarations.
   */
  private Node parseOptDeclarationList() throws UnexpectedTokenException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseDeclarationList();
  }

  /**
   * Parse a collection of parameters.
   */
  private Node parseParamList() throws UnexpectedTokenException, IOException {
    // Look for end of parameter list
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    return parseParams();
  }

  /**
   * Parse parameters.
   */
  private Node parseParams() throws UnexpectedTokenException, IOException {
    // Handle <param><optparam>
    Node param = parseParam();
    Node chain = parseOptParams();

    // Determine whether there are more parameters
    if (chain != null) {
      Node node = new Node(NodeType.PARAM_LIST);
      node.setLeftChild(param);
      node.setRightChild(chain);
      return node;
    }

    return param;
  }

  /**
   * Parse optionally more parameters.
   */
  private Node parseOptParams() throws UnexpectedTokenException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseParams();
  }

  /**
   * Parse a function parameter.
   */
  private Node parseParam() throws UnexpectedTokenException, IOException {
    if (isNext(TokenType.CONST)) {
      consume();
      return parseArrayDecl();
    }

    return null;
  }
  
  /**
   * Parse main program
   */
  private Node parseMain() throws UnexpectedTokenException, IOException {
    // Handle main
    symbolTable.pushScope("main");
    expectAndConsume(TokenType.MAIN);
    Node node = new Node(NodeType.MAIN);

    // Handle <slist>
    node.setNextChild(parseSList());
    
    // Handle begin <stats>
    expectAndConsume(TokenType.BEGIN);
    node.setNextChild(parseStatements());

    // Handle end CD20 <id>
    expectAndConsume(TokenType.END);
    expectAndConsume(TokenType.CD20);
    expectAndConsume(TokenType.IDENTIFIER);

    symbolTable.popScope();
    return node;
  }

  /**
   * Handles a list of sdecl?
   */
  private Node parseSList() throws IOException, UnexpectedTokenException {
    if (isNext(TokenType.BEGIN)) return null;

    Node decl = parseDeclaration();
    Node chain = parseOptSDecl();

    if (chain != null) {
      Node node = new Node(NodeType.SDECL_LIST);
      node.setLeftChild(decl);
      node.setRightChild(chain);
      return node;
    }

    return decl;
  }

  /**
   * Parse optionally more sdecls.
   */
  private Node parseOptSDecl() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseSList();
  }

  /**
   * Parse a collection of statements.
   */
  private Node parseStatements() throws IOException, UnexpectedTokenException {
    // First, handle simpler <strstat>
    Node statement = parseBlockStatement();

    // Handle <stat> if not <strstat>
    if (statement == null) {
      statement = parseStatement();
      expectAndConsume(TokenType.SEMI_COLON);
    }

    // Handle <optstats>
    Node chain = parseOptStatements();

    if (chain != null) {
      Node node = new Node(NodeType.STATEMENTS);
      node.setLeftChild(statement);
      node.setRightChild(chain);
      return node;
    }

    return statement;
  }

  /**
   * Parses optionally more statements.
   * @return A {@link Node} containing statements or null.
   */
  private Node parseOptStatements() throws IOException, UnexpectedTokenException {
    // TODO Find a better method for determining when statements end
    if (isNext(TokenType.END)) return null;
    return parseStatements();
  }

  /**
   * Parse a block statement.
   * @return A {@link Node} containing a block statement.
   */
  private Node parseBlockStatement() throws IOException, UnexpectedTokenException {
    switch (nextToken.getType()) {
      case FOR:
        return parseForStatement();
      case IF:
        return parseIfStatement();
      default:
        return null;
    }
  }

  /**
   * Parse a for statement.
   * @return A {@link Node} containing a for statement.
   */
  private Node parseForStatement() throws IOException, UnexpectedTokenException {
    // Handle for (
    Node node = new Node(NodeType.FOR);
    expectAndConsume(TokenType.FOR);
    expectAndConsume(TokenType.LEFT_PAREN);
    
    // Handle <asgnlist>;
    node.setNextChild(parseAssignmentList());
    expectAndConsume(TokenType.SEMI_COLON);

    // Handle <bool>)
    node.setNextChild(parseBool());
    expectAndConsume(TokenType.RIGHT_PAREN);

    // Handle <stats> end
    node.setNextChild(parseStatements());
    expectAndConsume(TokenType.END);

    return node;
  }

  /**
   * Parse a list of assignments.
   */
  private Node parseAssignmentList() throws IOException, UnexpectedTokenException {
    Node assignment = parseAssignment();
    Node chain = parseOptAssignmentList();

    if (chain != null) {
      Node node = new Node(NodeType.ASSIGN_LIST);
      node.setLeftChild(assignment);
      node.setRightChild(chain);
      return node;
    }

    return assignment;
  }

  /**
   * Parse optionally more assignments.
   */
  private Node parseOptAssignmentList() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseAssignmentList();
  }

  /**
   * Parse an if statement.
   */
  private Node parseIfStatement() throws IOException, UnexpectedTokenException {
    // Handle if
    expectAndConsume(TokenType.IF);

    // Handle <bool>)
    expectAndConsume(TokenType.LEFT_PAREN);
    Node bool = parseBool();
    expectAndConsume(TokenType.RIGHT_PAREN);

    // Handle <stats>
    Node statements = parseStatements();

    // Handle else <stats> end
    Node elseStatements = parseOptElseStatement();
    expectAndConsume(TokenType.END);

    // Create node
    Node node;
    if (elseStatements == null) {
      node = new Node(NodeType.IF);
    } else {
      node = new Node(NodeType.IF_ELSE);
    }

    node.setNextChild(bool);
    node.setNextChild(statements);
    node.setNextChild(elseStatements);

    return node;
  }

  /**
   * Parse an optional else statement.
   * @return A {@link Node} containing statements from an else block or null.
   */
  private Node parseOptElseStatement() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.END)) return null;
    expectAndConsume(TokenType.ELSE);
    return parseStatements();
  }

  /**
   * Parse a statement.
   */
  private Node parseStatement() throws IOException, UnexpectedTokenException {
    switch (nextToken.getType()) {
      case REPEAT:
        return parseRepeatStatement();
      case INPUT:
      case PRINT:
      case PRINTLN:
        return parseIoStatement();
      case RETURN:
        return parseReturnStatement();
      case IDENTIFIER:
        return parseCallStatement();
      default:
        return parseAssignment();
    }
  }

  /**
   * Parse a return statement.
   */
  private Node parseReturnStatement() throws IOException, UnexpectedTokenException {
    // Handle return
    Node node = new Node(NodeType.RETURN);
    expectAndConsume(TokenType.RETURN);

    node.setNextChild(parseOptReturn());

    return node;
  }

  /**
   * Parse an optional expression after a return statement.
   */
  private Node parseOptReturn() throws IOException, UnexpectedTokenException {
    if (isNext(TokenType.SEMI_COLON)) return null;
    return parseExpression();
  }

  /**
   * Parse a function call.
   */
  private Node parseCallStatement() throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.FUNCTION_CALL, expectIdentifier());

    // Handle (<optparams>)
    expectAndConsume(TokenType.LEFT_PAREN);
    node.setNextChild(parseOptCallParams());
    expectAndConsume(TokenType.RIGHT_PAREN);

    return node;
  }

  /**
   * Parse an optional list of parameters.
   */
  private Node parseOptCallParams() throws IOException, UnexpectedTokenException {
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    return parseExpressionList();
  }

  /**
   * Parse a list of expressions.
   */
  private Node parseExpressionList() throws IOException, UnexpectedTokenException {
    Node bool = parseBool();
    Node chain = parseOptExpressionList();

    if (chain != null) {
      Node node = new Node(NodeType.EXPRESSION_LIST);
      node.setLeftChild(bool);
      node.setRightChild(chain);
      return node;
    }

    return bool;
  }

  /**
   * Parse optionally more bools.
   */
  private Node parseOptExpressionList() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseExpressionList();
  }

  /**
   * Parse an assignment statement.
   */
  private Node parseAssignment() throws UnexpectedTokenException, IOException {
    // Handle <var><asgnop>
    Node varNode = parseVar();
    Node asignOp = parseAssignmentOp();
    asignOp.setLeftChild(varNode);

    // Handle <bool>
    asignOp.setRightChild(parseBool());

    return asignOp;
  }

  /**
   * Parse an assignment operator
   */
  private Node parseAssignmentOp() throws IOException, UnexpectedTokenException {
    switch (nextToken.getType()) {
      case ASSIGN:
        consume();
        return new Node(NodeType.ASSIGN);
      case INCREMENT:
        consume();
        return new Node(NodeType.INCREMENT);
      case DECREMENT:
        consume();
        return new Node(NodeType.DECREMENT);
      case STAR_EQUALS:
        consume();
        return new Node(NodeType.STAR_EQUALS);
      case DIVIDE_EQUALS:
        consume();
        return new Node(NodeType.DIVIDE_EQUALS);
      default:
        throw new UnexpectedTokenException("an assignment operator", nextToken);
    }
  }

  /**
   * Parse a repeat statement.
   */
  private Node parseRepeatStatement() throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.REPEAT);

    // Handle repeat (
    expectAndConsume(TokenType.REPEAT);
    expectAndConsume(TokenType.LEFT_PAREN);

    // Handle <asgnlist>
    node.setNextChild(parseAssignmentList());

    // Handle ) <stats>
    expectAndConsume(TokenType.RIGHT_PAREN);
    node.setNextChild(parseStatements());

    // Handle until <bool>
    expectAndConsume(TokenType.UNTIL);
    node.setNextChild(parseBool());

    return node;
  }

  /**
   * Parse an I/O statment.
   */
  private Node parseIoStatement() throws IOException, UnexpectedTokenException {
    switch (nextToken.getType()) {
      case INPUT:
        return parseInputStatement();
      case PRINT:
        return parsePrintStatement();
      case PRINTLN:
        return parsePrintLineStatement();
      default:
        throw new UnexpectedTokenException("'input', 'print', or 'println'", nextToken);
    }
  }

  /**
   * Parse an input statement.
   */
  private Node parseInputStatement() throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.INPUT);

    // Handle input <vlist>
    expectAndConsume(TokenType.INPUT);
    node.setNextChild(parseVarList());

    return node;
  }

  /**
   * Parse a print statement.
   */
  private Node parsePrintStatement() throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.PRINT);
    expectAndConsume(TokenType.PRINT);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse println statement.
   */
  private Node parsePrintLineStatement() throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.PRINTLN);
    expectAndConsume(TokenType.PRINTLN);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse a print list.
   */
  private Node parsePrintList() throws IOException, UnexpectedTokenException {
    Node print = parsePrint();
    Node chain = parseOptPrintList();

    if (chain != null) {
      Node node = new Node(NodeType.PRINT_LIST);
      node.setLeftChild(print);
      node.setRightChild(chain);
      return node;
    }

    return print;
  }

  /**
   * Parse optionally more print statements.
   */
  private Node parseOptPrintList() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parsePrintList();
  }

  /**
   * Parse a print entry.
   */
  private Node parsePrint() throws IOException, UnexpectedTokenException {
    // Handle <string>
    if (isNext(TokenType.STRING_LITERAL)) {
      Node node = new Node(NodeType.STRING, nextToken.getLexeme());
      consume();
      return node;
    }

    return parseExpression();
  }

  /**
   * Parse a list of variables.
   */
  private Node parseVarList() throws UnexpectedTokenException, IOException {
    Node variable = parseVar();
    Node chain = parseOptVar();

    if (chain != null) {
      Node node = new Node(NodeType.VARIABLE_LIST);
      node.setLeftChild(variable);
      node.setRightChild(chain);
      return node;
    }

    return variable;
  }

  /**
   * Parse optionall more variables.
   */
  private Node parseOptVar() throws UnexpectedTokenException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseVarList();
  }

  /**
   * Parse a possible collection of constants.
   */
  private Node parseConstants() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.CONSTANTS)) return null;
    consume();
    return parseInitList();
  }

  /**
   * Parse a list of initialisers.
   */
  private Node parseInitList() throws UnexpectedTokenException, IOException {
    Node node = new Node(NodeType.INIT_LIST);

    node.setNextChild(parseInit());
    node.setNextChild(parseOptInit());

    return node;
  }

  /**
   * Parse an initialiser.
   */
  private Node parseInit() throws UnexpectedTokenException, IOException {
    // Handle identifier
    expect(TokenType.IDENTIFIER);

    Node node = new Node(NodeType.INIT, nextToken.getLexeme());
    symbolTable.insertSymbol(new Symbol(nextToken));

    // Move on
    consume();
    expectAndConsume(TokenType.ASSIGN);

    // Handle expression
    node.setNextChild(parseExpression());

    return node;
  }

  /**
   * Parse optionally more initialisers.
   * <optinit> := , <initlist>
   *            | ε
   */
  private Node parseOptInit() throws IOException, UnexpectedTokenException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseInitList();
  }

  /**
   * Parse types.
   */
  private Node parseTypes() throws IOException, UnexpectedTokenException {
    // Only continue if given types
    if (!isNext(TokenType.TYPES)) {
      return null;
    }

    consume();
    return parseTypeList();
  }

  /**
   * Parse a list of types.
   */
  private Node parseTypeList() throws IOException, UnexpectedTokenException {
    // Create our type list and parse
    Node node = new Node(NodeType.TYPE_LIST);
    node.setNextChild(parseType());
    node.setNextChild(parseOptType());

    return node;
  }

  /**
   * Parse a type.
   */
  private Node parseType() throws IOException, UnexpectedTokenException {
    // Parse identifier
    expect(TokenType.IDENTIFIER);
    String lexeme = nextToken.getLexeme();
    consume();

    expectAndConsume(TokenType.IS);

    // Handle array
    Node array = parseArrayDef(lexeme);
    if (array != null) {
      return array;
    }

    return parseStructDef(lexeme);
  }

  /**
   * Parse an array definition.
   */
  private Node parseArrayDef(String lexeme) throws IOException, UnexpectedTokenException {
    // Only continue if next token is array
    if (!isNext(TokenType.ARRAY)) {
      return null;
    }

    Node node = new Node(NodeType.ARRAY_DEF, lexeme);
    consume();

    // Handle [<expr>]
    expectAndConsume(TokenType.LEFT_BRACKET);
    node.setNextChild(parseExpression());
    expectAndConsume(TokenType.RIGHT_BRACKET);

    // Handle of <structid>
    expectAndConsume(TokenType.OF);
    expect(TokenType.IDENTIFIER);
    node.setNextChild(new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme()));
    consume();

    return node;
  }

  /**
   * Parse a struct definition.
   */
  private Node parseStructDef(String lexeme) throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.STRUCT_DEF, lexeme);
    node.setNextChild(parseFields());

    expectAndConsume(TokenType.END);

    return node;
  }

  /**
   * Parse a list of struct fields
   */
  private Node parseFields() throws UnexpectedTokenException, IOException {
    Node declaration = parseDeclaration();
    Node sibling = parseOptFields();

    // Handle multiple fields
    if (sibling != null) {
      Node node = new Node(NodeType.STRUCT_FIELDS);
      node.setLeftChild(declaration);
      node.setRightChild(sibling);
      return node;
    }

    return declaration;
  }

  /**
   * Parse optionally more declarations.
   */
  private Node parseOptFields() throws UnexpectedTokenException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseFields();
  }

  /**
   * Parse a struct declaration.
   */
  private Node parseDeclaration() throws UnexpectedTokenException, IOException {
    // Handle ident :
    Node node = new Node(NodeType.SDECL, expectIdentifier());
    expectAndConsume(TokenType.COLON);

    // Handle <stype>
    node.setNextChild(parseSType());

    return node;
  }

  /**
   * Parse stype
   * TODO could use a lot of work
   */
  private Node parseSType() throws UnexpectedTokenException, IOException {
    if (isNext(TokenType.INT) || isNext(TokenType.REAL) || isNext(TokenType.BOOL)) {
      consume();
      return null;
    }

    throw new UnexpectedTokenException("'int', 'real', or 'bool'", nextToken);
  }

  /**
   * Parse optionally another type.
   */
  private Node parseOptType() throws IOException, UnexpectedTokenException {
    // Determine whether another type is defined
    if (isNext(TokenType.IDENTIFIER)) {
      return parseTypeList();
    }

    return null;
  }

  private Node parseArrays() throws IOException, UnexpectedTokenException {
    // Only continue if given arrays
    if (!isNext(TokenType.ARRAYS)) {
      return null;
    }

    consume();
    return parseArrayDecls();
  }

  private Node parseArrayDecls() throws UnexpectedTokenException, IOException {
    Node decl = parseArrayDecl();
    Node chain = parseOptArrDecls();

    if (chain != null) {
      Node node = new Node(NodeType.ARRAY_DECLS);
      node.setLeftChild(decl);
      node.setRightChild(chain);
      return node;
    }

    return decl;
  }

  private Node parseOptArrDecls() throws UnexpectedTokenException, IOException {
    // Only continue if given comma
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseArrayDecls();
  }

  private Node parseArrayDecl() throws UnexpectedTokenException, IOException {
    // Handle <id>
    expect(TokenType.IDENTIFIER);
    Node node = new Node(NodeType.ARRAY_DECL, nextToken.getLexeme());
    consume();

    // Handle :
    expectAndConsume(TokenType.COLON);

    // Handle <typeid>
    expect(TokenType.IDENTIFIER);
    node.setNextChild(new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme()));
    consume();

    return node;
  }

  /**
   * Parse an expression.
   */
  private Node parseExpression() throws IOException, UnexpectedTokenException {
    Node term = parseTerm();
    Node chain = parseExpressionPrime();

    if (chain != null) {
      chain.setLeftChild(term);
      return chain;
    }

    return term;
  }

  /**
   * Parses optional extensions to an expression.
   */
  private Node parseExpressionPrime() throws IOException, UnexpectedTokenException {
    Node node;

    // Determine whether the next Token is a + or -
    if (isNext(TokenType.PLUS)) {
      node = new Node(NodeType.ADD);
    } else if (isNext(TokenType.MINUS)) {
      node = new Node(NodeType.SUBTRACT);
    } else {
      return null;
    }

    consume(); // Consume the +/- symbol

    // Parse the term and recursively parse the remainder of the chain
    Node term = parseTerm();
    Node chain = parseExpressionPrime();
      
    // There are more nodes to come
    if (chain != null) {
      node.setLeftChild(term);
      node.setRightChild(chain);
    } else {
      node.setRightChild(term);
    }

    return node;
  }

  /**
   * Parse a term.
   */
  private Node parseTerm() throws IOException, UnexpectedTokenException {
    Node fact = parseFact();
    Node chain = parseTermPrime();

    if (chain != null) {
      chain.setLeftChild(fact);
      return chain;
    }

    return fact;
  }

  /**
   * Parses optional extensions to a term.
   */
  private Node parseTermPrime() throws IOException, UnexpectedTokenException {
    Node node;

    // Determine whether the next Token indicates another round
    if (isNext(TokenType.STAR)) {
      node = new Node(NodeType.MULTIPLY);
    } else if (isNext(TokenType.DIVIDE)) {
      node = new Node(NodeType.DIVIDE);
    } else if (isNext(TokenType.PERCENT)) {
      node = new Node(NodeType.MODULO);
    } else {
      return null;
    }

    consume(); // Consume the * / % token

    // Parse the term and recursively parse the remainder of the chain
    Node term = parseFact();
    Node chain = parseFactPrime();
      
    // There are more nodes to come
    if (chain != null) {
      node.setLeftChild(term);
      node.setRightChild(chain);
    } else {
      node.setRightChild(term);
    }

    return node;
  }

  /**
   * Parse a fact.
   */
  private Node parseFact() throws IOException, UnexpectedTokenException {
    Node exponent = parseExponent();
    Node chain = parseFactPrime();

    if (chain != null) {
      chain.setLeftChild(exponent);
      return chain;
    }

    return exponent;
  }

  /**
   * Parses optional extensions to a fact.
   */
  private Node parseFactPrime() throws IOException, UnexpectedTokenException {
    // Handle ^
    if (!isNext(TokenType.CARAT)) return null;
    consume();

    // Create new node for exponent
    // <fact>
    Node power = new Node(NodeType.POWER);
    power.setRightChild(parseFact());
    
    return power;
  }

  /**
   * Parse an exponent.
   */
  private Node parseExponent() throws IOException, UnexpectedTokenException {
    // Handle <int>
    if (isNext(TokenType.INTEGER_LITERAL)) {
      Node node = new Node(NodeType.INTEGER_LITERAL, nextToken.getLexeme());
      consume();
      return node;
    }

    // Handle <real>
    if (isNext(TokenType.REAL)) {
      Node node = new Node(NodeType.REAL_LITERAL, nextToken.getLexeme());
      consume();
      return node;
    }

    // Handle true
    if (isNext(TokenType.TRUE)) {
      Node node = new Node(NodeType.TRUE);
      consume();
      return node;
    }

    // Handle false
    if (isNext(TokenType.FALSE)) {
      Node node = new Node(NodeType.FALSE);
      consume();
      return node;
    }

    // Handle <bool>
    if (isNext(TokenType.LEFT_PAREN)) {
      consume();
      Node node = parseBool();
      expectAndConsume(TokenType.RIGHT_PAREN);
      return node;
    }

    // TODO handle function call

    return parseVar();
  }

  /**
   * Parse a boolean.
   */
  private Node parseBool() throws IOException, UnexpectedTokenException {
    Node rel = parseRel();
    Node[] chain = parseOptBool();

    if (chain != null) {
      Node node = new Node(NodeType.BOOLEAN);
      node.setLeftChild(rel);
      node.setCentreChild(chain[0]);
      node.setRightChild(chain[1]);
      return node;
    }

    return rel;
  }

  /**
   * Parse more boolean
   */
  private Node[] parseOptBool() throws IOException, UnexpectedTokenException {
    // Attempt to parse logical operator
    Node logicalOp = parseLogicalOp();
    if (logicalOp == null) return null;

    Node rel = parseRel();
    Node chain = parseBool();
    Node[] nodes = { rel, chain };
    return nodes;
  }

  /**
   * Parse a logicl operator.
   * @return A logical op {@link Node} or null.
   */
  private Node parseLogicalOp() throws IOException {
    switch (nextToken.getType()) {
      case AND:
        consume();
        return new Node(NodeType.AND);
      case OR:
        consume();
        return new Node(NodeType.OR);
      case XOR:
        consume();
        return new Node(NodeType.XOR);
      default:
        return null;
    }
  }

  /**
   * Parse relational statement.
   */
  private Node parseRel() throws IOException, UnexpectedTokenException {
    Node not = parseOptNot();
    Node expression = parseExpression();

    // Handle <optrelop>
    Node relOp = parseOptRelOp();
    if (relOp != null) {
      relOp.setLeftChild(expression);

      // Handle possible not
      if (not != null) {
        not.setNextChild(relOp);
        return not;
      }

      return relOp;
    }

    // Handle possible not
    if (not != null) {
      not.setNextChild(expression);
      return not;
    }

    return expression;
  }

  /**
   * Parse an optional not node.
   */
  private Node parseOptNot() throws IOException {
    if (!isNext(TokenType.NOT)) return null;
    consume();
    return new Node(NodeType.NOT);
  }

  /**
   * Parse an optional relational operator.
   */
  private Node parseOptRelOp() throws IOException, UnexpectedTokenException {
    // See if a <relop> is provided
    Node relop = parseRelOp();
    if (relop == null) return null;

    // Handle <expr>
    relop.setRightChild(parseExpression());
    return relop;
  }

  /**
   * Parse a relative operator.
   * @return A relative operator {@link Node} or null.
   */
  private Node parseRelOp() throws IOException {
    switch (nextToken.getType()) {
      case EQUALS_EQUALS:
        consume();
        return new Node(NodeType.EQUAL);
      case NOT_EQUAL:
        consume();
        return new Node(NodeType.NOT_EQUAL);
      case GREATER:
        consume();
        return new Node(NodeType.GREATER);
      case GREATER_OR_EQUAL:
        consume();
        return new Node(NodeType.GREATER_OR_EQUAL);
      case LESS:
        consume();
        return new Node(NodeType.LESS);
      case LESS_OR_EQUAL:
        consume();
        return new Node(NodeType.LESS_OR_EQUAL);
      default:
        return null;
    }
  }

  /**
   * Parse a variable
   */
  private Node parseVar() throws UnexpectedTokenException, IOException {
    // Handle <ident>
    String lexeme = expectIdentifier();

    // Handle array variable
    Node arrVar = parseArrayVar(nextToken.getLexeme());
    if (arrVar != null) {
      return arrVar;
    }

    // Handle simple variable
    Node node = new Node(NodeType.SIMPLE_VARIABLE, lexeme);
    return node;
  }

  /**
   * Parse a more complex array variable.
   */
  private Node parseArrayVar(String identifier) throws IOException, UnexpectedTokenException {
    // Handle [
    if (!isNext(TokenType.LEFT_BRACKET)) return null;
    consume();
    
    // Create node and parse expression
    // <expr>
    Node node = new Node(NodeType.ARRAY_VARIABLE, identifier);
    node.setLeftChild(parseExpression());

    // Handle ].
    expectAndConsume(TokenType.RIGHT_BRACKET);
    expectAndConsume(TokenType.DOT);

    // Handle <ident>
    expect(TokenType.IDENTIFIER);
    Node ident = new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme());
    node.setRightChild(ident);
    consume();

    return node;
  }
}
