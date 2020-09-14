package cd20.parser;

import java.io.IOException;

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
   * Begin parsing
   */
  public Node parse() throws IOException {
    nextToken = scanner.nextToken();

    try {
      parseProgram();
    } catch (UnexpectedTokenException exception) {
      output.addAnnotation(new Annotation(exception.getFoundToken(), exception.getMessage()));
    }

    return rootNode;
  }

  /**
   * Expect a {@link Token} of a given {@link TokenType}.
   * If a {@link Token} of that {@link TokenType} is not provided next, throws
   * an exception that cannot be recovered from.
   */
  private void expectToken(TokenType type) throws UnexpectedTokenException {
    // Handle unexpected token
    if (nextToken.getType() != type) {
      throw new UnexpectedTokenException(type, nextToken);
    }
  }

  private void expectAndConsumeToken(TokenType type) throws IOException, UnexpectedTokenException {
    expectToken(type);
    consumeToken();
  }

  private boolean isNextToken(TokenType type) {
    return nextToken.getType() == type;
  }

  /**
   * Move to the next available {@link Token}
   */
  private void consumeToken() throws IOException {
    nextToken = scanner.nextToken();
  }

  /**
   * Parses the PROGRAM node
   */
  private Node parseProgram() throws IOException, UnexpectedTokenException {
    symbolTable.pushScope("global");

    // Handle CD20
    expectAndConsumeToken(TokenType.CD20);
  
    // Parse identifier
    expectToken(TokenType.IDENTIFIER);
    rootNode = new Node(NodeType.PROGRAM, nextToken.getLexeme());
    consumeToken();

    rootNode.setNextChild(parseGlobals());
    rootNode.setNextChild(parseMain());

    symbolTable.popScope();

    return rootNode;
  }

  /**
   * Parses the GLOBALS node
   */
  private Node parseGlobals() throws IOException, UnexpectedTokenException {
    // Create a new node for globals
    Node globals = new Node(NodeType.GLOBALS);

    // Parse constants, types, and arrays
    globals.setNextChild(parseConstants());
    globals.setNextChild(parseTypes());
    globals.setNextChild(parseArrays());

    return globals;
  }
  
  private Node parseMain() throws UnexpectedTokenException, IOException {
    expectToken(TokenType.MAIN);
    consumeToken();
    return null;
  }

  /**
   * Parse a possible collection of constants.
   */
  private Node parseConstants() throws IOException, UnexpectedTokenException {
    if (!isNextToken(TokenType.CONSTANTS)) {
      return null;
    }

    consumeToken();
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
    expectToken(TokenType.IDENTIFIER);

    Node node = new Node(NodeType.INIT, nextToken.getLexeme());
    symbolTable.insertSymbol(new Symbol(nextToken));

    // Move on
    consumeToken();
    expectAndConsumeToken(TokenType.EQUALS);

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
    // Only continue if given a comma
    if (!isNextToken(TokenType.COMMA)) {
      return null;
    }

    consumeToken();
    return parseInitList();
  }

  /**
   * Parse types.
   */
  private Node parseTypes() throws IOException, UnexpectedTokenException {
    // Only continue if given types
    if (!isNextToken(TokenType.TYPES)) {
      return null;
    }

    consumeToken();
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
    expectToken(TokenType.IDENTIFIER);
    String lexeme = nextToken.getLexeme();
    consumeToken();

    expectAndConsumeToken(TokenType.IS);

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
    if (!isNextToken(TokenType.ARRAY)) {
      return null;
    }

    Node node = new Node(NodeType.ARRAY_DEF, lexeme);
    consumeToken();

    // Handle [<expr>]
    expectAndConsumeToken(TokenType.LEFT_BRACKET);
    node.setNextChild(parseExpression());
    expectAndConsumeToken(TokenType.RIGHT_BRACKET);

    // Handle of <structid>
    expectAndConsumeToken(TokenType.OF);
    expectToken(TokenType.IDENTIFIER);
    node.setNextChild(new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme()));
    consumeToken();

    return node;
  }

  /**
   * Parse a struct definition.
   */
  private Node parseStructDef(String lexeme) throws IOException, UnexpectedTokenException {
    Node node = new Node(NodeType.STRUCT_DEF, lexeme);
    node.setNextChild(parseFields());

    expectAndConsumeToken(TokenType.END);

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
    // Only continue if given comma
    if (!isNextToken(TokenType.COMMA)) {
      return null;
    }

    consumeToken();
    return parseFields();
  }

  /**
   * Parse a struct declaration.
   */
  private Node parseDeclaration() throws UnexpectedTokenException, IOException {
    // Handle ident
    expectToken(TokenType.IDENTIFIER);
    Node node = new Node(NodeType.STRUCT_FIELD, nextToken.getLexeme());
    consumeToken();

    // Handle :
    expectAndConsumeToken(TokenType.COLON);

    // Handle <stype>
    node.setNextChild(parseSType());

    return node;
  }

  /**
   * Parse stype
   * TODO could use a lot of work
   */
  private Node parseSType() throws UnexpectedTokenException, IOException {
    if (isNextToken(TokenType.INT) || isNextToken(TokenType.REAL) || isNextToken(TokenType.BOOL)) {
      consumeToken();
      return null;
    }

    throw new UnexpectedTokenException("'int', 'real', or 'bool'", nextToken);
  }

  /**
   * Parse optionally another type.
   */
  private Node parseOptType() throws IOException, UnexpectedTokenException {
    // Determine whether another type is defined
    if (isNextToken(TokenType.IDENTIFIER)) {
      return parseTypeList();
    }

    return null;
  }

  private Node parseArrays() throws IOException {
    // Only continue if given arrays
    if (!isNextToken(TokenType.ARRAYS)) {
      return null;
    }

    consumeToken();
    return parseArrayDecls();
  }

  private Node parseArrayDecls() {
    return null;
  }

  /**
   * Parse an expression.
   *
   *  <expr> := <term><expr'>
   * <expr'> := + <term><expr'>
   *          | - <term><expr'>
   *          | ε
   */
  private Node parseExpression() throws IOException, UnexpectedTokenException {
    Node term = parseTerm();
    Node parent = parseExpressionPrime();

    // If there is more to this expression, parent will not be null
    if (parent != null) {
      parent.setLeftChild(term);
      return parent;
    }

    return term;
  }

  /**
   * Parses optional extensions to an expression.
   */
  private Node parseExpressionPrime() throws IOException, UnexpectedTokenException {
    Node node;

    // Determine whether the next Token is a + or -
    if (isNextToken(TokenType.PLUS)) {
      node = new Node(NodeType.ADD);
    } else if (isNextToken(TokenType.MINUS)) {
      node = new Node(NodeType.SUBTRACT);
    } else {
      return null;
    }

    consumeToken(); // Consume the +/- symbol

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
   *  <term> := <fact><term'>
   * <term'> := * <fact><term'>
   *          | / <fact><term'>
   *          | % <fact><term'>
   *          | ε
   */
  private Node parseTerm() throws IOException, UnexpectedTokenException {
    Node fact = parseFact();
    Node parent = parseTermPrime();

    if (parent != null) {
      parent.setLeftChild(fact);
      return parent;
    }

    return fact;
  }

  /**
   * Parses optional extensions to a term.
   */
  private Node parseTermPrime() throws IOException, UnexpectedTokenException {
    Node node;

    // Determine whether the next Token indicates another round
    if (isNextToken(TokenType.STAR)) {
      node = new Node(NodeType.MULTIPLY);
    } else if (isNextToken(TokenType.DIVIDE)) {
      node = new Node(NodeType.DIVIDE);
    } else if (isNextToken(TokenType.PERCENT)) {
      node = new Node(NodeType.MODULO);
    } else {
      return null;
    }

    consumeToken(); // Consume the * / % token

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
   *  <fact> := <exponent><fact'>
   * <fact'> := ^ <fact>
   *          | ε
   */
  private Node parseFact() throws IOException, UnexpectedTokenException {
    Node exponent = parseExponent();
    Node parent = parseFactPrime();

    if (parent != null) {
      parent.setLeftChild(exponent);
      return parent;
    }

    return exponent;
  }

  /**
   * Parses optional extensions to a fact.
   */
  private Node parseFactPrime() throws IOException, UnexpectedTokenException {
    // Only continue if a carat is provided
    if (!isNextToken(TokenType.CARAT)) {
      return null;
    }

    consumeToken();

    // Create new node for exponent
    Node power = new Node(NodeType.POWER);
    Node fact = parseFact();
    power.setRightChild(fact);
    return power;
  }

  /**
   * Parse an exponent.
   * <exponent> := <var>
   *             | <intlit>
   *             | <reallit>
   *             | <fncall>
   *             | true
   *             | false
   *             | (<bool>)
   */
  private Node parseExponent() throws IOException, UnexpectedTokenException {
    // Handle int
    if (isNextToken(TokenType.INTEGER_LITERAL)) {
      Node node = new Node(NodeType.INTEGER_LITERAL, nextToken.getLexeme());
      consumeToken();
      return node;
    }

    // Handle real
    if (isNextToken(TokenType.REAL)) {
      Node node = new Node(NodeType.REAL_LITERAL, nextToken.getLexeme());
      consumeToken();
      return node;
    }

    // Handle true
    if (isNextToken(TokenType.TRUE)) {
      Node node = new Node(NodeType.TRUE);
      consumeToken();
      return node;
    }

    // Handle false
    if (isNextToken(TokenType.FALSE)) {
      Node node = new Node(NodeType.FALSE);
      consumeToken();
      return node;
    }

    // Handle bool
    if (isNextToken(TokenType.LEFT_PAREN)) {
      consumeToken();
      Node node = parseBool();
      expectToken(TokenType.RIGHT_PAREN);
      return node;
    }

    // TODO handle function call

    return parseVar();
  }

  private Node parseBool() {
    return null;
  }

  /**
   * Parse a variable
   */
  private Node parseVar() throws UnexpectedTokenException, IOException {
    // Expect and consume an identifier
    expectToken(TokenType.IDENTIFIER);
    Token token = nextToken;
    consumeToken();

    // Handle array variable
    Node arrVar = parseArrayVar(nextToken.getLexeme());
    if (arrVar != null) {
      return arrVar;
    }

    // Handle simple variable
    Node node = new Node(NodeType.SIMPLE_VARIABLE, token.getLexeme());
    return node;
  }

  /**
   * Parse a more complex array variable.
   */
  private Node parseArrayVar(String identifier) throws IOException, UnexpectedTokenException {
    // Only continue if we have a left bracket
    if (!isNextToken(TokenType.LEFT_BRACKET)) {
      return null;
    }

    consumeToken();
    
    // Create node and parse expression
    Node node = new Node(NodeType.ARRAY_VARIABLE, identifier);
    node.setLeftChild(parseExpression());

    // Handle syntax tokens
    expectAndConsumeToken(TokenType.RIGHT_BRACKET);
    expectAndConsumeToken(TokenType.DOT);

    // Handle remaining identifier
    expectToken(TokenType.IDENTIFIER);
    Node ident = new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme());
    node.setRightChild(ident);
    consumeToken();

    return node;
  }
}
