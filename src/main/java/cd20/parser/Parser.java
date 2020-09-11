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
  private Node currentNode;

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
  private void parseProgram() throws IOException, UnexpectedTokenException {
    symbolTable.pushScope("global");

    expectToken(TokenType.CD20);
    consumeToken();
  
    expectToken(TokenType.IDENTIFIER);
    rootNode = new Node(NodeType.PROGRAM, nextToken.getLexeme());
    currentNode = rootNode;
    consumeToken();

    parseGlobals();
    parseMain();

    symbolTable.popScope();
  }

  /**
   * Parses the GLOBALS node
   */
  private void parseGlobals() throws IOException, UnexpectedTokenException {
    // Create a new node for globals
    Node globals = new Node(NodeType.GLOBALS);
    currentNode.setNextChild(globals);
    currentNode = globals;

    // Parse constants, types, and arrays
    parseConstants();
    parseTypes();
    parseArrays();

    // Move back up tree
    currentNode = rootNode;
  }
  
  private void parseMain() throws UnexpectedTokenException, IOException {
    expectToken(TokenType.MAIN);
    consumeToken();
  }

  /**
   * Parse a possible collection of constants.
   */
  private void parseConstants() throws IOException, UnexpectedTokenException {
    if (isNextToken(TokenType.CONSTANTS)) {
      consumeToken();
      parseInitList();
    }
  }

  /**
   * Parse a list of initialisers.
   */
  private void parseInitList() throws UnexpectedTokenException, IOException {
    // Create new init list node
    Node parent = currentNode;
    Node node = new Node(NodeType.INIT_LIST);
    parent.setNextChild(node);
    currentNode = node;

    parseInit();
    parseOptInit();

    // Move back up tree
    currentNode = parent;
  }

  /**
   * Parse an initialiser.
   */
  private void parseInit() throws UnexpectedTokenException, IOException {
    // Handle identifier
    expectToken(TokenType.IDENTIFIER);

    // Create our nodes
    Node parent = currentNode;
    Node node = new Node(NodeType.INIT, nextToken.getLexeme());
    parent.setLeftChild(node);
    currentNode = node;

    symbolTable.insertSymbol(new Symbol(nextToken));

    // Move on
    consumeToken();
    expectToken(TokenType.EQUALS);
    consumeToken();

    // Handle expression
    parseExpression();

    currentNode = parent;
  }

  /**
   * Parse optionally more initialisers.
   * <optinit> := , <initlist>
   *            | ε
   */
  private void parseOptInit() throws IOException, UnexpectedTokenException {
    if (isNextToken(TokenType.COMMA)) {
      consumeToken();
      parseInitList();
    }
  }

  private void parseTypes() throws IOException {
    if (isNextToken(TokenType.TYPES)) {
      consumeToken();
      parseTypeList();
    }
  }

  private void parseTypeList() {}

  private void parseArrays() throws IOException {
    if (isNextToken(TokenType.ARRAYS)) {
      consumeToken();
      parseArrayDecls();
    }
  }

  private void parseArrayDecls() {}

  /**
   * Parse an expression.
   *
   *  <expr> := <term><expr'>
   * <expr'> := + <term><expr'>
   *          | - <term><expr'>
   *          | ε
   */
  private void parseExpression() throws IOException, UnexpectedTokenException {
    parseTerm();
  }

  /**
   * Parse a term.
   *  <term> := <fact><term'>
   * <term'> := * <fact><term'>
   *          | / <fact><term'>
   *          | % <fact><term'>
   *          | ε
   */
  private void parseTerm() throws IOException, UnexpectedTokenException {
    parseFact();
  }

  /**
   * Parse a fact.
   *  <fact> := <exponent><fact'>
   * <fact'> := ^ <fact>
   *          | ε
   */
  private void parseFact() throws IOException, UnexpectedTokenException {
    parseExponent();
    parseFactPrime();
  }

  private void parseFactPrime() throws IOException, UnexpectedTokenException {
    if (isNextToken(TokenType.CARAT)) {
      parseFact();
    }
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
  private void parseExponent() throws IOException, UnexpectedTokenException {
    // Handle int
    if (isNextToken(TokenType.INTEGER_LITERAL)) {
      Node node = new Node(NodeType.INTEGER_LITERAL, nextToken.getLexeme());
      currentNode.setNextChild(node);
      consumeToken();
      return;
    }

    // Handle real
    if (isNextToken(TokenType.REAL)) {
      Node node = new Node(NodeType.REAL_LITERAL, nextToken.getLexeme());
      currentNode.setNextChild(node);
      consumeToken();
      return;
    }

    // Handle true
    if (isNextToken(TokenType.TRUE)) {
      Node node = new Node(NodeType.TRUE);
      currentNode.setNextChild(node);
      consumeToken();
      return;
    }

    // Handle false
    if (isNextToken(TokenType.FALSE)) {
      Node node = new Node(NodeType.FALSE);
      currentNode.setNextChild(node);
      consumeToken();
      return;
    }

    // Handle bool
    if (isNextToken(TokenType.LEFT_PAREN)) {
      consumeToken();
      parseBool();
      expectToken(TokenType.RIGHT_PAREN);
      return;
    }

    // TODO handle function call

    parseVar();
  }

  private void parseBool() {}

  /**
   * Parse a variable
   */
  private void parseVar() throws UnexpectedTokenException, IOException {
    expectToken(TokenType.IDENTIFIER);
    Token token = nextToken;
    consumeToken();
    
    // Handle array variable
    if (isNextToken(TokenType.LEFT_BRACKET)) {
      Node parent = currentNode;
      Node node = new Node(NodeType.ARRAY_VARIABLE, token.getLexeme());
      parent.setNextChild(node);
      currentNode = node;

      parseOptVar();

      currentNode = parent;
      return;
    }

    // Handle simple variable
    Node node = new Node(NodeType.SIMPLE_VARIABLE, token.getLexeme());
    currentNode.setNextChild(node);
  }

  private void parseOptVar() throws UnexpectedTokenException, IOException {
    expectAndConsumeToken(TokenType.LEFT_BRACKET);
    parseExpression();
  }
}
