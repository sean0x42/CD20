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
import cd20.symboltable.SymbolTableManager;
import cd20.symboltable.SymbolType;
import cd20.symboltable.attribute.DataTypeAttribute;
import cd20.symboltable.attribute.FloatConstantAttribute;
import cd20.symboltable.attribute.IntegerConstantAttribute;
import cd20.symboltable.attribute.ParameterAttribute;
import cd20.symboltable.attribute.ReturnTypeAttribute;

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
      return parseDeclarationList();
    }

    return null;
  }

  /**
   * Parse declaration list.
   */
  private Node parseDeclarationList() throws ParserException, IOException {
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
  private Node parseOptDeclarationList() throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseDeclarationList();
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
    return parseDeclaration();
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
  private Node parseOptSDecl() throws IOException, ParserException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseMainDeclarationList();
  }

  /**
   * Parse a collection of statements.
   */
  private Node parseStatements() throws IOException, SyntaxException {
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
  private Node parseOptionalStatements() throws IOException, SyntaxException {
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
  private Node parseBlockStatement() throws IOException, SyntaxException {
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
  private Node parseForStatement() throws IOException, SyntaxException {
    // Handle for (
    Node node = new Node(NodeType.FOR);
    int line = nextToken.getLine();
    expectAndConsume(TokenType.FOR);
    expectAndConsume(TokenType.LEFT_PAREN);

    // Handle scope
    symbolManager.createScope(String.format("%s_for%d", symbolManager.getScope(), line));
    
    // Handle <asgnlist>;
    node.setNextChild(parseAssignmentList());
    expectAndConsume(TokenType.SEMI_COLON);

    // Handle <bool>)
    node.setNextChild(parseBool());
    expectAndConsume(TokenType.RIGHT_PAREN);

    // Handle <stats> end
    node.setNextChild(parseStatements());
    expectAndConsume(TokenType.END);

    symbolManager.leaveScope();
    return node;
  }

  /**
   * Parse a list of assignments.
   */
  private Node parseAssignmentList() throws IOException, SyntaxException {
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
  private Node parseOptAssignmentList() throws IOException, SyntaxException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseAssignmentList();
  }

  /**
   * Parse an if statement.
   */
  private Node parseIfStatement() throws IOException, SyntaxException {
    // Handle if
    int line = nextToken.getLine();
    expectAndConsume(TokenType.IF);
    symbolManager.createScope(String.format("%s_if%d", symbolManager.getScope(), line));

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
  private Node parseOptionalElse() throws IOException, SyntaxException {
    if (!isNext(TokenType.ELSE)) return null;
    consume();
    return parseStatements();
  }

  /**
   * Parse a simple inline statement
   */
  private Node parseInlineStatement() throws IOException, SyntaxException {
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
  private Node parseStatementPrime() throws SyntaxException, IOException {
    String lexeme = expectIdentifier();
    consume();

    switch (nextToken.getType()) {
      case LEFT_PAREN:
        return parseFunctionCallStatement(lexeme);
      default:
        return parseAssignment(lexeme);
    }
  }

  /**
   * Parse a return statement.
   */
  private Node parseReturnStatement() throws IOException, SyntaxException {
    // Handle return
    Node node = new Node(NodeType.RETURN);
    expectAndConsume(TokenType.RETURN);

    node.setNextChild(parseOptionalReturn());

    return node;
  }

  /**
   * Parse an optional expression after a return statement.
   */
  private Node parseOptionalReturn() throws IOException, SyntaxException {
    if (isNext(TokenType.SEMI_COLON)) return null;
    return parseExpression();
  }

  /**
   * Parse a function call statement
   */
  private Node parseFunctionCallStatement(String lexeme) throws IOException, SyntaxException {
    Node node = new Node(NodeType.FUNCTION_CALL, lexeme);

    // (<optparams>)
    expectAndConsume(TokenType.LEFT_PAREN);
    node.setNextChild(parseOptCallParams());
    expectAndConsume(TokenType.RIGHT_PAREN);

    return node;
  }

  /**
   * Parse an optional list of parameters.
   */
  private Node parseOptCallParams() throws IOException, SyntaxException {
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    return parseExpressionList();
  }

  /**
   * Parse a list of expressions.
   */
  private Node parseExpressionList() throws IOException, SyntaxException {
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
  private Node parseOptExpressionList() throws IOException, SyntaxException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseExpressionList();
  }

  /**
   * Parse an assignment statement.
   */
  private Node parseAssignment() throws SyntaxException, IOException {
    String identifier = expectIdentifier();
    consume();
    return parseAssignment(identifier);
  }

  /**
   * Parse an assignment statement.
   * @param lexeme Lexeme of the variable being assigned to.
   */
  private Node parseAssignment(String lexeme) throws SyntaxException, IOException {
    // Handle <var><asgnop>
    Node varNode = parseVar(lexeme);
    Node asignOp = parseAssignmentOp();
    asignOp.setLeftChild(varNode);

    // Find relevant symbol
    Symbol symbol = symbolManager.resolve(lexeme);
    asignOp.setSymbol(symbol);
    varNode.setSymbol(symbol);

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
  private Node parseRepeatStatement() throws IOException, SyntaxException {
    Node node = new Node(NodeType.REPEAT);

    // Handle repeat (
    int line = nextToken.getLine();
    expectAndConsume(TokenType.REPEAT);
    expectAndConsume(TokenType.LEFT_PAREN);
    symbolManager.createScope(String.format("%s_repeat%d", symbolManager.getScope(), line));

    // Handle <asgnlist>
    node.setNextChild(parseAssignmentList());

    // Handle ) <stats>
    expectAndConsume(TokenType.RIGHT_PAREN);
    node.setNextChild(parseStatements());

    // Handle until <bool>
    expectAndConsume(TokenType.UNTIL);
    node.setNextChild(parseBool());

    symbolManager.leaveScope();
    return node;
  }

  /**
   * Parse an I/O statment.
   */
  private Node parseIoStatement() throws IOException, SyntaxException {
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
  private Node parseInputStatement() throws IOException, SyntaxException {
    Node node = new Node(NodeType.INPUT);

    // Handle input <vlist>
    expectAndConsume(TokenType.INPUT);
    node.setNextChild(parseVarList());

    return node;
  }

  /**
   * Parse a print statement.
   */
  private Node parsePrintStatement() throws IOException, SyntaxException {
    Node node = new Node(NodeType.PRINT);
    expectAndConsume(TokenType.PRINT);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse println statement.
   */
  private Node parsePrintLineStatement() throws IOException, SyntaxException {
    // Handle println
    Node node = new Node(NodeType.PRINTLN);
    expectAndConsume(TokenType.PRINTLN);

    node.setNextChild(parsePrintList());

    return node;
  }

  /**
   * Parse a print list.
   */
  private Node parsePrintList() throws IOException, SyntaxException {
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
  private Node parseOptPrintList() throws IOException, SyntaxException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parsePrintList();
  }

  /**
   * Parse a print entry.
   */
  private Node parsePrint() throws IOException, SyntaxException {
    // Handle <string>
    if (isNext(TokenType.STRING_LITERAL)) {
      Node node = new Node(NodeType.STRING, nextToken.getLexeme());

      // Create symbol
      Symbol symbol = new Symbol(
        SymbolType.STRING_CONSTANT,
        "__string__" + nextToken.getLexeme().replace(" ", "_"),
        nextToken
      );
      node.setSymbol(symbol);
      symbolManager.insertSymbol(symbol, BaseRegister.CONSTANTS);

      consume();
      return node;
    }

    return parseExpression();
  }

  /**
   * Parse a list of variables.
   */
  private Node parseVarList() throws SyntaxException, IOException {
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
  private Node parseOptVar() throws SyntaxException, IOException {
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
    // TODO propograte expression data type
    Node expression = parseExpression();
    DataType type = expression
      .getSymbol()
      .getFirstAttribute(DataTypeAttribute.class)
      .getType();
    Symbol symbol = new Symbol(SymbolType.fromDataType(type), initToken);
    node.setSymbol(symbol);
    symbolManager.insertSymbol(symbol);
    
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
  private Node parseArrayDef(String lexeme) throws IOException, SyntaxException {
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
  private Node parseOptFields() throws ParserException, IOException {
    if (!isNext(TokenType.COMMA)) return null;
    consume();
    return parseFields();
  }

  /**
   * Parse a struct declaration.
   */
  private Node parseDeclaration() throws ParserException, IOException {
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
    DataType type = dataType
      .getSymbol()
      .getFirstAttribute(DataTypeAttribute.class)
      .getType();

    // Create symbol
    Symbol symbol = new Symbol(SymbolType.fromDataType(type), token);
    node.setSymbol(symbol);
    symbolManager.insertSymbol(symbol);

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

    Symbol symbol = new Symbol(SymbolType.EXPRESSION, nextToken);
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
  private Node parseExpression() throws IOException, SyntaxException {
    Node term = parseTerm();
    Node chain = parseExpressionPrime();

    // TODO
    // 1. Determine term type
    // 2. Determine chain type
    // 3. Ensure that types are both numbers
    // 4. Make sure returned node has the correct type set
    Symbol symbol = new Symbol(type, token);

    if (chain != null) {
      chain.setLeftChild(term);
      return chain;
    }

    return term;
  }

  /**
   * Parses optional extensions to an expression.
   */
  private Node parseExpressionPrime() throws IOException, SyntaxException {
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
  private Node parseTerm() throws IOException, SyntaxException {
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
  private Node parseTermPrime() throws IOException, SyntaxException {
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
  private Node parseFact() throws IOException, SyntaxException {
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
  private Node parseFactPrime() throws IOException, SyntaxException {
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
  private Node parseExponent() throws IOException, SyntaxException {
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

      // Create symbol
      Symbol symbol = new Symbol(
        SymbolType.INTEGER_CONSTANT,
        lexeme,
        nextToken.getLine(),
        nextToken.getColumn()
      );
      symbol.addAttribute(new IntegerConstantAttribute(lexeme));
      node.setSymbol(symbol);
      symbolManager.insertSymbol(symbol);

      consume();
      return node;
    }

    // Handle <real>
    if (isNext(TokenType.FLOAT_LITERAL)) {
      // Generate lexeme
      String lexeme = nextToken.getLexeme();
      if (isNegative) lexeme = "-" + lexeme;
      Node node = new Node(NodeType.REAL_LITERAL, lexeme);

      // Create symbol
      Symbol symbol = new Symbol(
        SymbolType.FLOAT_CONSTANT,
        lexeme,
        nextToken.getLine(),
        nextToken.getColumn()
      );
      symbol.addAttribute(new FloatConstantAttribute(lexeme));
      node.setSymbol(symbol);
      symbolManager.insertSymbol(symbol);

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

    // Handle possible function call
    String lexeme = expectIdentifier();
    consume();
    if (isNext(TokenType.LEFT_PAREN)) {
      return parseFunctionCall(lexeme);
    }

    return parseVar(lexeme);
  }

  /**
   * Parse a function call within an exponent.
   * @param lexeme Function called.
   */
  private Node parseFunctionCall(String lexeme) throws IOException, SyntaxException {
    Node node = new Node(NodeType.FUNC_CALL, lexeme);

    // Parse (<optelist>)
    expectAndConsume(TokenType.LEFT_PAREN);
    node.setNextChild(parseOptFuncExpressionList());
    expectAndConsume(TokenType.RIGHT_PAREN);

    return node;
  }

  /**
   * Optionally parse function expressions.
   */
  private Node parseOptFuncExpressionList() throws IOException, SyntaxException {
    if (isNext(TokenType.RIGHT_PAREN)) return null;
    Node node = parseExpressionList();
    return node;
  }

  /**
   * Parse a boolean.
   */
  private Node parseBool() throws IOException, SyntaxException {
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
  private Node[] parseOptBool() throws IOException, SyntaxException {
    // Attempt to parse logical operator
    Node logicalOp = parseLogicalOp();
    if (logicalOp == null) return null;

    Node chain = parseBool();
    Node[] nodes = { logicalOp, chain };
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
  private Node parseRel() throws IOException, SyntaxException {
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
  private Node parseOptRelOp() throws IOException, SyntaxException {
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
  private Node parseVar() throws SyntaxException, IOException {
    String lexeme = expectIdentifier();
    consume();
    return parseVar(lexeme);
  }

  /**
   * Parse a variable
   * @param lexeme Lexeme of the variable.
   */
  private Node parseVar(String lexeme) throws SyntaxException, IOException {
    // Handle array variable
    Node arrVar = parseArrayVar(nextToken.getLexeme());
    if (arrVar != null) {
      return arrVar;
    }

    Symbol symbol = symbolManager.resolve(lexeme);
    Node node = new Node(NodeType.SIMPLE_VARIABLE, lexeme);
    node.setSymbol(symbol);
    return node;
  }

  /**
   * Parse a more complex array variable.
   */
  private Node parseArrayVar(String identifier) throws IOException, SyntaxException {
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
