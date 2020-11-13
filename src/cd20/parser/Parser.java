package cd20.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import cd20.output.Annotation;
import cd20.output.ListingGenerator;
import cd20.output.WarningAnnotation;
import cd20.scanner.Scanner;
import cd20.scanner.Token;
import cd20.scanner.TokenType;
import cd20.symboltable.BaseRegister;
import cd20.symboltable.Symbol;
import cd20.symboltable.SymbolBuilder;
import cd20.symboltable.SymbolTableManager;
import cd20.symboltable.SymbolType;
import cd20.symboltable.attribute.*;

/**
 * A top down recursive parser for CD20
 *
 * Turns a stream of tokens into an AST.
 */
public class Parser {
  private final ListingGenerator output;
  private final Scanner scanner;
  private final SymbolTableManager symbolManager;

  private Token nextToken;
  private Node rootNode;

  public Parser(Reader reader, SymbolTableManager symbolManager, ListingGenerator output) {
    this.scanner = new Scanner(reader, output);
    this.output = output;
    this.symbolManager = symbolManager;
  }

  /**
   * Begin parsing a CD20 program.
   * @return An abstract syntax tree.
   */
  public Node parse() throws IOException {
    nextToken = scanner.nextToken();

    try {
      return parseProgram();
    } catch (ParserException exception) {
      output.addAnnotation(
        new Annotation(exception.getMessage(), exception.getToken())
      );
    }

    return null;
  }

  /**
   * Adds a warning to the output controller.
   * @param message Warning message.
   * @param token Token that caused warning.
   */
  private void warn(String message, Token token) {
    output.addAnnotation(new WarningAnnotation(message, token));
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
    expect(TokenType.IDENTIFIER);
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
   * Similar to #expectAndConsume, but if the token is not provided it will attempt to continue parsing.
   * @param type Type to suggest.
   */
  private void expectAndConsumeOrInsert(TokenType type) throws IOException {
    // Handle correct type
    if (isNext(type)) {
      consume();
      return;
    }

    warn(
      String.format("Expected %s. Will try continuing anyway.", type.getHumanReadable()),
      nextToken
    );
  }

  /**
   * Throw a semantic exception if the node is not numeric.
   * @param node Node to check type of.
   * @param token Token to annotate exception to.
   */
  private void expectNumeric(Node node, Token token) throws SemanticException {
    DataType nodeType = AttributeUtils.getDataType(node);

    // Ensure exponent is numeric
    if (!nodeType.isNumeric()) {
      throw new SemanticException(
          String.format("Type must be numerical. Instead found type: %s", nodeType.toString()),
          token
      );
    }
  }

  /**
   * Throw a semantic exception if the node is not a boolean.
   * @param node Node to check type of.
   * @param token Token to annotate exception to.
   */
  private void expectBoolean(Node node, Token token) throws SemanticException {
    DataType type = AttributeUtils.getDataType(node);

    if (!type.isBoolean()) {
      throw new SemanticException(
          String.format("Type must be boolean. Instead found type: %s", type.toString()),
          token
      );
    }
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
  private Node parseProgram() throws IOException, ParserException {
    symbolManager.createScope("global");

    // Handle CD20 <id>
    expectAndConsume(TokenType.CD20);
    rootNode = new Node(NodeType.PROGRAM, expectIdentifier());
    consume();

    // Handle <globals><funcs><main>
    rootNode.setNextChild(parseGlobals());
    rootNode.setNextChild(parseFunctions());
    rootNode.setNextChild(parseMain());

    expectAndConsume(TokenType.CD20);
    String endLexeme = expectIdentifier();

    // Ensure that program name matches
    if (!endLexeme.equals(rootNode.getValue())) {
      throw new SemanticException(
        String.format(
          "Program name '%s' does not match '%s'.",
          endLexeme,
          rootNode.getValue()
        ),
        nextToken
      );
    }

    consume();
    symbolManager.leaveScope();
    return rootNode;
  }

  /**
   * Parses a globals node
   * @return A {@link Node} of type GLOBALS
   */
  private Node parseGlobals() throws IOException, ParserException {
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
  private Node parseFunctions() throws IOException, ParserException {
    // Only continue if a function is given
    if (!isNext(TokenType.FUNC)) return null;

    Node functions = new Node(NodeType.FUNCTIONS);
    functions.setNextChild(parseFunction());
    functions.setNextChild(parseFunctions());

    return functions;
  }

  /**
   * Add parameter attributes to the given symbol.
   * @param params A node containing function parameters.
   * @param symbol A symbol to add parameters to.
   */
  private void addParameterAttributes(Node params, Symbol symbol) {
    switch (params.getType()) {
      case SDECL:
        symbol.addAttribute(new ParameterAttribute(params.getSymbol()));
        return;
      case PARAM_LIST:
        for (Node child : params.getChildren()) {
          addParameterAttributes(child, symbol);
        }
        return;
      default:
        throw new RuntimeException(
          String.format("Unexpected parameter node type: %s", params.getType())
        );
    }
  }

  /**
   * Parse a function.
   * @return A {@link Node} of type FUNC
   */
  private Node parseFunction() throws IOException, ParserException {
    // Handle func <id>
    expectAndConsume(TokenType.FUNC);
    String lexeme = expectIdentifier();
    Node func = new Node(NodeType.FUNCTION_DEF, lexeme);

    // Create symbol
    if (symbolManager.containsSymbol(lexeme)) {
      throw new SemanticException(
        String.format("Duplicate identifier: '%s'. Function names must be unique.", lexeme),
        nextToken
      );
    }

    // Create symbol
    Symbol symbol = new Symbol(SymbolType.FUNCTION, nextToken);
    func.setSymbol(symbol);
    symbolManager.insertSymbol(symbol);
    symbolManager.createScope(String.format("__function__%s", lexeme));
    consume();

    // Handle (<plist>):
    expectAndConsume(TokenType.LEFT_PAREN);
    Node params = parseParamList();

    if (params != null) {
      addParameterAttributes(params, symbol);
      func.setNextChild(params);
    }

    expectAndConsume(TokenType.RIGHT_PAREN);
    expectAndConsumeOrInsert(TokenType.COLON);

    // Handle <rtype><funcbody>
    symbol.addAttribute(parseReturnType());

    for (Node node : parseFunctionBody()) {
      func.setNextChild(node);
    }

    symbolManager.leaveScope();
    return func;
  }

  /**
   * Parse a function return type.
   * @return A {@link ReturnTypeAttribute} for use within a Symbol.
   */
  private ReturnTypeAttribute parseReturnType() throws IOException, UnexpectedTokenException {
    // Handle void
    if (isNext(TokenType.VOID)) {
      consume();
      return new ReturnTypeAttribute(new DataType("void"));
    }

    Node stype = parseDataType();
    DataType type = stype
      .getSymbol()
      .getFirstAttribute(DataTypeAttribute.class)
      .getType();
    return new ReturnTypeAttribute(type);
  }

  /**
   * Parse a function body.
   * May include a node of locals and a node of statements.
   * @return A list of {@link Node}s that comprise a function's body.
   */
  private List<Node> parseFunctionBody() throws IOException, ParserException {
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
  private Node parseLocals() throws ParserException, IOException {
    if (isNext(TokenType.IDENTIFIER)) {
      return parseDeclarationList(BaseRegister.DECLARATIONS);
    }

    return null;
  }

  /**
   * Parse declaration list.
   */
  private Node parseDeclarationList(BaseRegister register) throws ParserException, IOException {
    Node decl = parseDeclaration(register);
    Node chain = parseOptDeclarationList(register);

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
  private Node parseOptDeclarationList(BaseRegister register) throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseDeclarationList(register);
  }

  /**
   * Parse a collection of parameters.
   */
  private Node parseParamList() throws ParserException, IOException {
    // Look for end of parameter list
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    return parseParams();
  }

  /**
   * Parse parameters.
   */
  private Node parseParams() throws ParserException, IOException {
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
  private Node parseOptParams() throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseParams();
  }

  /**
   * Parse a function parameter.
   */
  private Node parseParam() throws ParserException, IOException {
    // TODO handle array decl and const
    return parseDeclaration(BaseRegister.DECLARATIONS, true);
  }
  
  /**
   * Parse main program
   */
  private Node parseMain() throws IOException, ParserException {
    // Handle main
    symbolManager.createScope("main");
    expectAndConsume(TokenType.MAIN);
    Node node = new Node(NodeType.MAIN);

    // Handle <slist>
    node.setNextChild(parseMainDeclarationList());
    
    // Handle begin <stats>
    expectAndConsume(TokenType.BEGIN);
    node.setNextChild(parseStatements());

    // Handle end CD20 <id>
    expectAndConsume(TokenType.END);

    symbolManager.leaveScope();
    return node;
  }

  /**
   * Handles a list of declarations in the main function.
   */
  private Node parseMainDeclarationList() throws IOException, ParserException {
    if (!isNext(TokenType.IDENTIFIER)) return null;

    Node decl = parseDeclaration(BaseRegister.GLOBALS);
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
  private Node parseOptSDecl() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseMainDeclarationList();
  }

  /**
   * Parse a collection of statements.
   */
  private Node parseStatements() throws IOException, ParserException {
    // If we detect an end here, we can throw a more meaningful error message
    // about how at least one statement is required.
    if (isNext(TokenType.ELSE) || isNext(TokenType.END) || isNext(TokenType.UNTIL)) {
      warn(
        "At least one statement is required here.\nWill try continuing anyway.",
        nextToken
      );
      return null;
    }

    // First, handle simpler <strstat>
    Node statement = parseBlockStatement();

    // Handle <stat> if not <strstat>
    if (statement == null) {
      statement = parseInlineStatement();
      expectAndConsumeOrInsert(TokenType.SEMI_COLON);
    }

    // Handle <optstats>
    Node chain = parseOptionalStatements();

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
   */
  private Node parseOptionalStatements() throws IOException, ParserException {
    switch (nextToken.getType()) {
      case ELSE:
      case END:
      case UNTIL:
        return null;
      default:
        return parseStatements();
    }
  }

  /**
   * Parse a block statement.
   * @return A {@link Node} containing a block statement.
   */
  private Node parseBlockStatement() throws IOException, ParserException {
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
  private Node parseForStatement() throws IOException, ParserException {
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
  private Node parseAssignmentList() throws IOException, ParserException {
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
  private Node parseOptAssignmentList() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseAssignmentList();
  }

  /**
   * Parse an if statement.
   */
  private Node parseIfStatement() throws IOException, ParserException {
    // Handle if
    expectAndConsume(TokenType.IF);

    // Handle (<bool>)
    expectAndConsume(TokenType.LEFT_PAREN);
    Node bool = parseBool();
    expectAndConsume(TokenType.RIGHT_PAREN);

    // Handle <stats>
    Node statements = parseStatements();

    // Handle else <stats> end
    Node elseStatements = parseOptionalElse();
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
   * Parse an optional else statement within an if statement
   */
  private Node parseOptionalElse() throws IOException, ParserException {
    if (!isNext(TokenType.ELSE)) return null;
    consume();
    return parseStatements();
  }

  /**
   * Parse a simple inline statement
   */
  private Node parseInlineStatement() throws IOException, ParserException {
    switch (nextToken.getType()) {
      case REPEAT:
        return parseRepeatStatement();
      case INPUT:
      case PRINT:
      case PRINTLN:
        return parseIoStatement();
      case RETURN:
        return parseReturnStatement();
      default:
        return parseStatementPrime();
    }
  }

  /**
   * Parse call and assignment statements, which both begin with a common
   * <ident>
   */
  private Node parseStatementPrime() throws ParserException, IOException {
    String lexeme = expectIdentifier();
    Token token = nextToken;
    consume();

    switch (nextToken.getType()) {
      case LEFT_PAREN:
        return parseFunctionCallStatement(lexeme);
      default:
        return parseAssignment(lexeme, token);
    }
  }

  /**
   * Parse a return statement.
   */
  private Node parseReturnStatement() throws IOException, ParserException {
    // Handle return
    Node node = new Node(NodeType.RETURN);
    expectAndConsume(TokenType.RETURN);

    node.setNextChild(parseOptionalReturn());

    return node;
  }

  /**
   * Parse an optional expression after a return statement.
   */
  private Node parseOptionalReturn() throws IOException, ParserException {
    if (isNext(TokenType.SEMI_COLON)) return null;
    return parseExpression();
  }

  /**
   * Parse a function call statement
   */
  private Node parseFunctionCallStatement(String lexeme) throws IOException, ParserException {
    Token callToken = nextToken;
    Node node = new Node(NodeType.FUNCTION_CALL, lexeme);

    Symbol symbol = symbolManager.resolve(lexeme);
    if (symbol == null) {
      throw new SemanticException(
          String.format("Unknown function: '%s'", lexeme),
          callToken
      );
    }
    node.setSymbol(symbol);

    // (<optparams>)
    expectAndConsume(TokenType.LEFT_PAREN);
    node.setNextChild(parseOptCallParams());
    expectAndConsume(TokenType.RIGHT_PAREN);

    return node;
  }

  /**
   * Parse an optional list of parameters.
   */
  private Node parseOptCallParams() throws IOException, ParserException {
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    return parseExpressionList();
  }

  /**
   * Parse a list of expressions.
   */
  private Node parseExpressionList() throws IOException, ParserException {
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
  private Node parseOptExpressionList() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseExpressionList();
  }

  /**
   * Parse an assignment statement.
   */
  private Node parseAssignment() throws ParserException, IOException {
    String identifier = expectIdentifier();
    Token token = nextToken;
    consume();
    return parseAssignment(identifier, token);
  }

  /**
   * Parse an assignment statement.
   * @param lexeme Lexeme of the variable being assigned to.
   * @param token Token.
   */
  private Node parseAssignment(String lexeme, Token token) throws ParserException, IOException {
    // Handle <var><asgnop>
    Token varToken = nextToken;
    Node varNode = parseVar(lexeme, token);
    Node asignOp = parseAssignmentOp();
    asignOp.setLeftChild(varNode);

    // Find relevant symbol
    Symbol symbol = symbolManager.resolve(lexeme);
    if (symbol.hasAttribute(ImmutableAttribute.class)) {
      throw new SemanticException("Attempted to assign to an immutable constant variable.", varToken);
    }

    asignOp.setSymbol(symbol);
    varNode.setSymbol(symbol);

    // Handle <bool>
    Node bool = parseBool();
    DataType varType = AttributeUtils.getDataType(varNode);
    DataType boolType = AttributeUtils.getDataType(bool);

    if (!varType.isAssignable(boolType)) {
      throw new SemanticException(
          String.format("Cannot assign type %s to %s.", boolType.toString(), varType.toString()),
          varToken
      );
    }

    asignOp.setRightChild(bool);

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
  private Node parseRepeatStatement() throws IOException, ParserException {
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
  private Node parseIoStatement() throws IOException, ParserException {
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
  private Node parseInputStatement() throws IOException, ParserException {
    Node node = new Node(NodeType.INPUT);

    // Handle input <vlist>
    expectAndConsume(TokenType.INPUT);
    node.setNextChild(parseVarList());

    return node;
  }

  /**
   * Parse a print statement.
   */
  private Node parsePrintStatement() throws IOException, ParserException {
    Node node = new Node(NodeType.PRINT);
    expectAndConsume(TokenType.PRINT);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse println statement.
   */
  private Node parsePrintLineStatement() throws IOException, ParserException {
    // Handle println
    Node node = new Node(NodeType.PRINTLN);
    expectAndConsume(TokenType.PRINTLN);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse a print list.
   */
  private Node parsePrintList() throws IOException, ParserException {
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
  private Node parseOptPrintList() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parsePrintList();
  }

  /**
   * Parse a print entry.
   */
  private Node parsePrint() throws IOException, ParserException {
    // Handle <string>
    if (isNext(TokenType.STRING_LITERAL)) {
      Node node = new Node(NodeType.STRING, nextToken.getLexeme());
      String symName = "__string__" + nextToken.getLexeme().replace(" ", "_");
      Symbol symbol = symbolManager.resolveConstant(symName);

      if (symbol == null) {
        symbol = SymbolBuilder.fromType(SymbolType.STRING_CONSTANT)
          .withValue(symName)
          .withTokenPosition(nextToken)
          .withAttribute(new StringConstantAttribute(nextToken.getLexeme()))
          .build();
        symbolManager.insertConstant(symbol);
      }

      node.setSymbol(symbol);
      consume();
      return node;
    }

    return parseExpression();
  }

  /**
   * Parse a list of variables.
   */
  private Node parseVarList() throws ParserException, IOException {
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
  private Node parseOptVar() throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseVarList();
  }

  /**
   * Parse a possible collection of constants.
   */
  private Node parseConstants() throws IOException, ParserException {
    if (!isNext(TokenType.CONSTANTS)) return null;
    consume();
    return parseInitList();
  }

  /**
   * Parse a list of initialisers.
   */
  private Node parseInitList() throws ParserException, IOException {
    Node node = new Node(NodeType.INIT_LIST);

    node.setNextChild(parseInit());
    node.setNextChild(parseOptInit());

    return node;
  }

  /**
   * Parse an initialiser.
   */
  private Node parseInit() throws ParserException, IOException {
    // Handle identifier
    expect(TokenType.IDENTIFIER);

    Node node = new Node(NodeType.INIT, nextToken.getLexeme());

    // Ensure that this symbol has not already been defined.
    if (symbolManager.containsSymbol(nextToken.getLexeme())) {
      throw new SemanticException(
        String.format("Duplicate identifier: '%s'", nextToken.getLexeme()),
        nextToken
      );
    }

    // Move on
    Token initToken = nextToken;
    consume();
    expectAndConsume(TokenType.ASSIGN);

    // Handle expression
    Node expression = parseExpression();
    DataType type = AttributeUtils.getDataType(expression);
    Symbol symbol = SymbolBuilder.fromType(SymbolType.fromDataType(type))
      .withValue(initToken.getLexeme())
      .withTokenPosition(initToken)
      .withAttribute(new DataTypeAttribute(type))
      .withAttribute(new ImmutableAttribute())
      .build();

    node.setSymbol(symbol);
    symbolManager.insertSymbol(symbol, BaseRegister.GLOBALS);

    node.setNextChild(expression);

    return node;
  }

  /**
   * Parse optionally more initialisers.
   */
  private Node parseOptInit() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseInitList();
  }

  /**
   * Parse types.
   */
  private Node parseTypes() throws IOException, ParserException {
    // Only continue if given types
    if (!isNext(TokenType.TYPES)) return null;
    consume();
    return parseTypeList();
  }

  /**
   * Parse a list of types.
   */
  private Node parseTypeList() throws IOException, ParserException {
    // Create our type list and parse
    Node node = new Node(NodeType.TYPE_LIST);
    node.setNextChild(parseType());
    node.setNextChild(parseOptType());

    return node;
  }

  /**
   * Parse a struct or array type.
   */
  private Node parseType() throws IOException, ParserException {
    // Parse <ident> is
    String lexeme = expectIdentifier();
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
  private Node parseArrayDef(String lexeme) throws IOException, ParserException {
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
  private Node parseStructDef(String lexeme) throws IOException, ParserException {
    Node node = new Node(NodeType.STRUCT_DEF, lexeme);
    node.setNextChild(parseFields());

    expectAndConsume(TokenType.END);

    return node;
  }

  /**
   * Parse a list of struct fields
   */
  private Node parseFields() throws ParserException, IOException {
    Node declaration = parseDeclaration(BaseRegister.GLOBALS); // TODO is this correct register?
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
  private Node parseOptFields() throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseFields();
  }

  private Node parseDeclaration(BaseRegister register) throws ParserException, IOException {
    return parseDeclaration(register, false);
  }

  /**
   * Parse a struct declaration.
   */
  private Node parseDeclaration(BaseRegister register, boolean isParameter) throws ParserException, IOException {
    // Handle <ident> :
    Token token = nextToken;
    Node node = new Node(NodeType.SDECL, expectIdentifier());
    consume();

    expectAndConsumeOrInsert(TokenType.COLON);

    // Handle <stype>
    Node dataType = parseDataType();
    node.setNextChild(dataType);

    // Ensure this isn't a duplicate declaration
    if (symbolManager.containsSymbol(token.getLexeme())) {
      throw new SemanticException(
        String.format("Duplicate identifier: '%s'", token.getLexeme()),
        token
      );
    }

    // Extract data type from node
    DataType type = AttributeUtils.getDataType(dataType);

    // Create symbol
    SymbolBuilder builder = SymbolBuilder.fromType(SymbolType.fromDataType(type))
      .withValue(token.getLexeme())
      .withTokenPosition(token)
      .withAttribute(new DataTypeAttribute(type));

    if (isParameter) {
      builder = builder.withAttribute(new IsParamAttribute());
    }

    Symbol symbol = builder.build();
    node.setSymbol(symbol);
    symbolManager.insertSymbol(symbol, register);

    return node;
  }

  /**
   * Parse data type.
   */
  private Node parseDataType() throws UnexpectedTokenException, IOException {
    DataType type;

    switch (nextToken.getType()) {
      case INT:
        type = new DataType("int");
        break;
      case REAL:
        type = new DataType("real");
        break;
      case BOOL:
        type = new DataType("bool");
        break;
      case IDENTIFIER:
        // TODO
        // lexeme = nextToken.getLexeme();
        throw new UnsupportedOperationException("Not implemented");
      default:
        throw new UnexpectedTokenException("'int', 'real', 'bool', or an identifier", nextToken);
    }

    Symbol symbol = new Symbol(SymbolType.TEMPORARY, nextToken);
    symbol.addAttribute(new DataTypeAttribute(type));

    consume();
    Node node = new Node(NodeType.DECLARATION_TYPE, type.toString());
    node.setSymbol(symbol);

    return node;
  }

  /**
   * Parse optionally another type.
   */
  private Node parseOptType() throws IOException, ParserException {
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
    expectAndConsumeOrInsert(TokenType.COLON);

    // Handle <typeid>
    expect(TokenType.IDENTIFIER);
    node.setNextChild(new Node(NodeType.SIMPLE_VARIABLE, nextToken.getLexeme()));
    consume();

    return node;
  }

  /**
   * Parse an expression.
   */
  private Node parseExpression() throws IOException, ParserException {
    Token termToken = nextToken;
    Node term = parseTerm();
    Token chainToken = nextToken;
    Node chain = parseExpressionPrime();

    if (chain != null) {
      expectNumeric(term, termToken);
      expectNumeric(chain, chainToken);
      
      chain.setLeftChild(term);
      return chain;
    }

    return term;
  }

  /**
   * Parses optional extensions to an expression.
   */
  private Node parseExpressionPrime() throws IOException, ParserException {
    Node node;

    // Determine whether the next Token is a + or -
    if (isNext(TokenType.PLUS)) {
      node = new Node(NodeType.ADD);
    } else if (isNext(TokenType.MINUS)) {
      node = new Node(NodeType.SUBTRACT);
    } else {
      return null;
    }

    Token nodeToken = nextToken;
    consume(); // Consume the +/- symbol

    // Parse the term and recursively parse the remainder of the chain
    Token termToken = nextToken;
    Node term = parseTerm();
    Token chainToken = nextToken;
    Node chain = parseExpressionPrime();
      
    // There are more nodes to come
    if (chain != null) {
      expectNumeric(term, termToken);
      expectNumeric(chain, chainToken);

      AttributeUtils.propogateDataType(term, node, nodeToken);

      node.setLeftChild(term);
      node.setRightChild(chain);
    } else {
      AttributeUtils.propogateDataType(term, node, nodeToken);
      node.setRightChild(term);
    }

    return node;
  }

  /**
   * Parse a term.
   */
  private Node parseTerm() throws IOException, ParserException {
    Token factToken = nextToken;
    Node fact = parseFact();
    Token chainToken = nextToken;
    Node chain = parseTermPrime();

    if (chain != null) {
      expectNumeric(fact, factToken);
      expectNumeric(chain, chainToken);

      chain.setLeftChild(fact);
      return chain;
    }

    return fact;
  }

  /**
   * Parses optional extensions to a term.
   */
  private Node parseTermPrime() throws IOException, ParserException {
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

    Token nodeToken = nextToken;
    consume(); // Consume the * / % token

    // Parse the term and recursively parse the remainder of the chain
    Token termToken = nextToken;
    Node term = parseFact();
    Token chainToken = nextToken;
    Node chain = parseTermPrime();

    // There are more nodes to come
    if (chain != null) {
      expectNumeric(term, termToken);
      expectNumeric(chain, chainToken);

      AttributeUtils.propogateDataType(term, node, nodeToken);

      node.setLeftChild(term);
      node.setRightChild(chain);
    } else {
      AttributeUtils.propogateDataType(term, node, nodeToken);
      node.setRightChild(term);
    }

    return node;
  }

  /**
   * Parse a fact.
   */
  private Node parseFact() throws IOException, ParserException {
    Token exponentToken = nextToken;
    Node exponent = parseExponent();
    Token chainToken = nextToken;
    Node chain = parseFactPrime();

    if (chain != null) {
      expectNumeric(exponent, exponentToken);
      expectNumeric(chain, chainToken);

      chain.setLeftChild(exponent);
      return chain;
    }

    return exponent;
  }

  /**
   * Parses optional extensions to a fact.
   */
  private Node parseFactPrime() throws IOException, ParserException {
    // Handle ^
    if (!isNext(TokenType.CARAT)) return null;
    consume();

    // Create new node for exponent
    // <fact>
    Node power = new Node(NodeType.POWER);
    Token powerToken = nextToken;
    power.setRightChild(parseFact());

    // Propogate type
    AttributeUtils.propogateDataType(power.getRightChild(), power, powerToken);
    
    return power;
  }

  /**
   * Parse an exponent.
   */
  private Node parseExponent() throws IOException, ParserException {
    // Handle possible negative
    boolean isNegative = false;
    if (isNext(TokenType.MINUS)) {
      consume();
      isNegative = true;
    }

    // Handle <int>
    if (isNext(TokenType.INTEGER_LITERAL)) {
      // Generate lexeme
      String lexeme = nextToken.getLexeme();
      if (isNegative) lexeme = "-" + lexeme;
      Node node = new Node(NodeType.INTEGER_LITERAL, lexeme);
      Symbol symbol = symbolManager.resolveConstant(lexeme);

      // Create symbol if it doesn't exist
      if (symbol == null) {
        symbol = SymbolBuilder.fromType(SymbolType.INTEGER_CONSTANT)
          .withValue(lexeme)
          .withTokenPosition(nextToken)
          .withAttribute(new IntegerConstantAttribute(lexeme))
          .withAttribute(new DataTypeAttribute("int"))
          .build();
        symbolManager.insertConstant(symbol);
      }

      node.setSymbol(symbol);
      consume();
      return node;
    }

    // Handle <real>
    if (isNext(TokenType.FLOAT_LITERAL)) {
      // Generate lexeme
      String lexeme = nextToken.getLexeme();
      if (isNegative) lexeme = "-" + lexeme;
      Node node = new Node(NodeType.REAL_LITERAL, lexeme);
      Symbol symbol = symbolManager.resolveConstant(lexeme);

      // Create symbol if it doesn't exist
      if (symbol == null) {
        symbol = SymbolBuilder.fromType(SymbolType.FLOAT_CONSTANT)
          .withValue(lexeme)
          .withTokenPosition(nextToken)
          .withAttribute(new FloatConstantAttribute(lexeme))
          .withAttribute(new DataTypeAttribute("real"))
          .build();
        symbolManager.insertConstant(symbol);
      }

      node.setSymbol(symbol);
      consume();
      return node;
    }

    // Nothing should be negative from here on
    if (isNegative) {
      warn(
        String.format(
          "Cannot have a negative %s.\nWill try continuing without the minus sign.",
          nextToken.getType().getHumanReadable()
        ),
        nextToken
      );
    }

    // Handle true
    if (isNext(TokenType.TRUE)) {
      Node node = new Node(NodeType.TRUE);

      // Create symbol for type checking
      Symbol symbol = new Symbol(SymbolType.TEMPORARY, nextToken);
      symbol.addAttribute(new DataTypeAttribute(new DataType("bool")));
      node.setSymbol(symbol);

      consume();
      return node;
    }

    // Handle false
    if (isNext(TokenType.FALSE)) {
      Node node = new Node(NodeType.FALSE);

      // Create symbol for type checking
      Symbol symbol = new Symbol(SymbolType.TEMPORARY, nextToken);
      symbol.addAttribute(new DataTypeAttribute(new DataType("bool")));
      node.setSymbol(symbol);

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

    // Handle possible function call
    String lexeme = expectIdentifier();
    Token token = nextToken;
    consume();
    if (isNext(TokenType.LEFT_PAREN)) {
      return parseFunctionCall(lexeme);
    }

    return parseVar(lexeme, token);
  }

  /**
   * Parse a function call within an exponent.
   * @param lexeme Function called.
   */
  private Node parseFunctionCall(String lexeme) throws IOException, ParserException {
    Token callToken = nextToken;
    Node node = new Node(NodeType.FUNC_CALL, lexeme);
    
    // Attempt to resolve symbol
    Symbol symbol = symbolManager.resolve(lexeme);
    if (symbol == null) {
      throw new SemanticException(
          String.format("Unknown function: '%s'.", lexeme),
          callToken
      );
    }
    node.setSymbol(symbol);

    // Parse (<optelist>)
    expectAndConsume(TokenType.LEFT_PAREN);
    node.setNextChild(parseOptFuncExpressionList());
    expectAndConsume(TokenType.RIGHT_PAREN);

    return node;
  }

  /**
   * Optionally parse function expressions.
   */
  private Node parseOptFuncExpressionList() throws IOException, ParserException {
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    Node node = parseExpressionList();
    return node;
  }

  /**
   * Parse a boolean.
   */
  private Node parseBool() throws IOException, ParserException {
    Token relToken = nextToken;
    Node rel = parseRel();
    Token chainToken = nextToken;
    Node chain = parseOptBool();

    if (chain != null) {
      expectBoolean(rel, relToken);
      expectBoolean(chain, chainToken);

      chain.setLeftChild(rel);
      return chain;
    }

    return rel;
  }

  /**
   * Parse more boolean
   */
  private Node parseOptBool() throws IOException, ParserException {
    // Attempt to parse logical operator
    Token logicalOpToken = nextToken;
    Node logicalOp = parseLogicalOp();
    if (logicalOp == null) return null;

    Node bool = parseBool();
    AttributeUtils.propogateDataType(bool, logicalOp, logicalOpToken);
    logicalOp.setRightChild(bool);
    return logicalOp;
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
  private Node parseRel() throws IOException, ParserException {
    Token notToken = nextToken;
    Node not = parseOptNot();
    Node expression = parseExpression();

    // Handle <optrelop>
    Token relOpToken = notToken;
    Node relOp = parseOptRelOp();
    if (relOp != null) {
      relOp.setLeftChild(expression);
      AttributeUtils.propogateDataType(expression, relOp, relOpToken);

      // Handle possible not
      if (not != null) {
        not.setNextChild(relOp);
        AttributeUtils.propogateDataType(relOp, not, notToken);
        return not;
      }

      return relOp;
    }

    // Handle possible not
    if (not != null) {
      not.setNextChild(expression);
      AttributeUtils.propogateDataType(expression, not, notToken);
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
  private Node parseOptRelOp() throws IOException, ParserException {
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
  private Node parseVar() throws ParserException, IOException {
    String lexeme = expectIdentifier();
    Token token = nextToken;
    consume();
    return parseVar(lexeme, token);
  }

  /**
   * Parse a variable
   * @param lexeme Lexeme of the variable.
   */
  private Node parseVar(String lexeme, Token token) throws ParserException, IOException {
    // Handle array variable
    Node arrVar = parseArrayVar(nextToken.getLexeme());
    if (arrVar != null) {
      return arrVar;
    }

    // Resolve symbol and ensure it has been defined
    Symbol symbol = symbolManager.resolve(lexeme);
    if (symbol == null) {
      throw new SemanticException(
        String.format("Variable '%s' has not been defined.", lexeme),
        token
      );
    }

    Node node = new Node(NodeType.SIMPLE_VARIABLE, lexeme);
    node.setSymbol(symbol);
    return node;
  }

  /**
   * Parse a more complex array variable.
   */
  private Node parseArrayVar(String identifier) throws IOException, ParserException {
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
