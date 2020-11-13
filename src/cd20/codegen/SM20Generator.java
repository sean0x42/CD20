package cd20.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import cd20.ByteUtils;
import cd20.parser.DataType;
import cd20.parser.Node;
import cd20.parser.NodeType;
import cd20.symboltable.BaseRegister;
import cd20.symboltable.Symbol;
import cd20.symboltable.SymbolTable;
import cd20.symboltable.SymbolTableManager;
import cd20.symboltable.attribute.*;

public class SM20Generator {
  private final SymbolTableManager symbolManager;
  private final Node root;
  private final CodeManager codeManager;

  private int totalVariables = 0;

  /**
   * Construct a new SM20 code generator.
   * @param symbolManager Symbol table manager.
   * @param root AST root node.
   */
  public SM20Generator(SymbolTableManager symbolManager, Node root) {
    this.symbolManager = symbolManager;
    this.root = root;
    this.codeManager = new CodeManager();
  }

  /**
   * Write generated SM20 code to a file at the given path.
   * @param path Path to create file.
   */
  public void writeToFile(String path) throws IOException {
    generateProgram(root);
    
    BufferedWriter writer = new BufferedWriter(new FileWriter(path));
    String module = codeManager.generateModule();
    writer.append(module);
    writer.close();

    symbolManager.printDebug();
    codeManager.printDebug();
    System.out.println(module);
  }

  /**
   * Extract the main node from a program node.
   * @param node Program node.
   */
  private Node extractMain(Node node) {
    for (Node child : node.getChildren()) {
      if (child.getType() == NodeType.MAIN) return child;
    }

    return null;
  }

  /**
   * Generate code for the program.
   */
  private void generateProgram(Node node) {
    SymbolTable table = symbolManager.enterScope("global");

    allocateConstants();
    allocateGlobals(table);
    allocateMain();

    // Now that all global variables have been assigned an offset, we can
    // allocate space on the stack
    if (totalVariables > 0) {
      codeManager.insert(new Instruction(Operation.LB, (byte) totalVariables));
      codeManager.insert(Operation.ALLOC);
    }

    // Generate main ahead of functions
    Node main = extractMain(node);
    generateMain(main);

    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case GLOBALS:
          generateGlobals(child);
          continue;
        case FUNCTIONS:
          generateFunctions(child);
          continue;
        case MAIN:
          continue;
        default:
          throw new UnsupportedOperationException("Unknown token type");
      }
    }

    symbolManager.leaveScope();
  }

  /**
   * Generate code for globals section.
   */
  private void generateGlobals(Node node) {
    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case INIT_LIST:
          generateInitList(child);
          continue;
        default:
          throw new UnsupportedOperationException("Not sure how to handle node of type: " + child.getType().toString());
      }
    }
  }

  /**
   * Generate code for an initialiser list.
   */
  private void generateInitList(Node node) {
    for (Node child : node.getChildren()) {
      // Handle possible init list nesting
      if (child.getType() == NodeType.INIT_LIST) {
        generateInitList(child);
        continue;
      }

      generateInit(child);
    }
  }

  /**
   * Generate code for an initialiser.
   */
  private void generateInit(Node node) {
    // Load address for value
    codeManager.insert(new BackfillInstruction(node.getSymbol(), Operation.PLACEHOLDER_LA));

    // Load and store
    generateExpression(node.getLeftChild());
    codeManager.insert(Operation.ST);
  }

  /**
   * Generate code for functions.
   */
  private void generateFunctions(Node node) {
    for (Node func : node.getChildren()) {
      // Handle possible nested functions
      if (func.getType() == NodeType.FUNCTIONS) {
        generateFunctions(func);
        return;
      }

      generateFunction(func);
    }
  }

  // TODO repeat statements!

  /**
   * Generate code for a function.
   * @param node Function node.
   */
  private void generateFunction(Node node) {
    SymbolTable table = symbolManager.enterScope(String.format("__function__%s", node.getValue()));

    // Set symbol register/offset
    Symbol symbol = node.getSymbol();
    symbol.setRegister(BaseRegister.CONSTANTS);
    symbol.setOffset(codeManager.getCodeGenerationPosition());

    // Alocate locals
    int variableCount = 0;
    for (Symbol sym : table.getSymbols()) {
      if (!sym.hasAttribute(IsParamAttribute.class)) {
        variableCount++;
      }
    }

    // Allocate space for local variables
    if (variableCount != 0) {
      codeManager.insert(new Instruction(Operation.LB, (byte) variableCount));
      codeManager.insert(Operation.ALLOC);
    }

    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case PARAM_LIST:
        case SDECL_LIST:
        case SDECL:
          continue;
        default:
          generateStatement(child);
      }
    }

    // Add return statement if none explicitly defined
    Instruction instruction = codeManager.getLastInstruction();
    if (!instruction.getOperation().equals(Operation.RETN)) {
      codeManager.insert(Operation.RETN);
    }

    symbolManager.leaveScope();
  }

  /**
   * Assign global variables a base register.
   * @param table Global symbol table.
   */
  private void allocateGlobals(SymbolTable table) {
    // Assign each symbol a (base, offset) pair
    for (Symbol symbol : table.getSymbols()) {
      allocateVariable(table, symbol);
    }
  }

  /**
   * Add all constants to the code manager to be allocated with offsets.
   */
  private void allocateConstants() {
    for (Symbol symbol : symbolManager.getConstants().getSymbols()) {
      switch (symbol.getType()) {
        case STRING_CONSTANT:
          codeManager.addStringConstant(
              symbol.getFirstAttribute(StringConstantAttribute.class).getConstant(),
              symbol
          );
          continue;
        case FLOAT_CONSTANT:
          codeManager.addFloatConstant(
              symbol.getFirstAttribute(FloatConstantAttribute.class).getConstant(),
              symbol
          );
          continue;
        case INTEGER_CONSTANT:
          codeManager.addIntegerConstant(
              symbol.getFirstAttribute(IntegerConstantAttribute.class).getConstant(),
              symbol
          );
          continue;
        default:
          continue;
      }
    }
  }

  private void allocateMain() {
    SymbolTable table = symbolManager.enterScope("main");
    allocateGlobals(table);
    symbolManager.leaveScope();
  }

  private void allocateVariable(SymbolTable table, Symbol symbol) {
    if (table.getScope() == "main" || table.getScope() == "global") {
      switch (symbol.getType()) {
        case INTEGER_VARIABLE:
        case FLOAT_VARIABLE:
        case BOOLEAN_VARIABLE:
          totalVariables++;
        default:
          return;
      }
    }
  }

  /**
   * Generate code for the main function.
   */
  private void generateMain(Node node) {
    // Get symbol table and assign (base, offset) pairs.
    symbolManager.enterScope("main");

    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case SDECL:
        case SDECL_LIST:
          // Should already be allocated
          continue;
        default:
          generateStatement(child);
      }
    }

    codeManager.insert(Operation.HALT);
  }

  /**
   * Generate code for a statement.
   * @param node Statement node.
   */
  private void generateStatement(Node node) {
    switch (node.getType()) {
      case STATEMENTS:
        for (Node child : node.getChildren()) {
          generateStatement(child);
        }
        return;
      case INPUT:
        generateInput(node);
        return;
      case PRINTLN:
        generatePrint(node, true);
        return;
      case PRINT:
        generatePrint(node, false);
        return;
      case ASSIGN:
        generateAssignment(node);
        return;
      case INCREMENT:
        generateOperationAssign(node, Operation.ADD);
        return;
      case DECREMENT:
        generateOperationAssign(node, Operation.SUB);
        return;
      case STAR_EQUALS:
        generateOperationAssign(node, Operation.MUL);
        return;
      case DIVIDE_EQUALS:
        generateOperationAssign(node, Operation.DIV);
        return;
      case FOR:
        generateFor(node);
        return;
      case IF:
      case IF_ELSE:
        generateIf(node);
        return;
      case FUNCTION_CALL:
        generateFunctionCall(node);
        return;
      case RETURN:
        generateReturn(node);
        return;
      default:
        throw new UnsupportedOperationException(node.toString());
    }
  }

  /**
   * Generate code for stdin.
   * @param node Input node.
   */
  private void generateInput(Node node) {
    for (Node child : node.getChildren()) {
      generateInputVar(child);
    }
  }

  /**
   * Generate code for an input variable.
   */
  private void generateInputVar(Node node) {
    switch (node.getType()) {
      case VARIABLE_LIST:
        for (Node child : node.getChildren()) {
          generateInputVar(child);
        }
        return;
      case SIMPLE_VARIABLE:
        DataType type = AttributeUtils.getDataType(node);
        loadSimpleVariableAddress(node);

        if (type.isInteger()) {
          codeManager.insert(Operation.READI);
        } else if (type.isReal()) {
          codeManager.insert(Operation.READF);
        } else {
          throw new RuntimeException("Received variable that is not numeric.");
        }

        codeManager.insert(Operation.ST);
        return;
      default:
        throw new RuntimeException("Encountered input var of type: " + node.getType().toString());
    }
  }

  /**
   * Generate code for a for loop.
   */
  private void generateFor(Node node) {
    generateAssignment(node.getLeftChild());

    // Generate initial condition and check
    Instruction skipToEndInstruction = new Instruction(Operation.LA0, ByteUtils.toByteArray(0));
    codeManager.insert(skipToEndInstruction);
    generateBool(node.getCentreChild());
    codeManager.insert(Operation.BF);

    // Generate repeat statements
    int startAddress = codeManager.getCodeGenerationPosition();
    generateStatement(node.getRightChild());

    // Repeat loop if necessary
    codeManager.insert(new Instruction(Operation.LA0, ByteUtils.toByteArray(startAddress)));
    generateBool(node.getCentreChild());
    codeManager.insert(Operation.BT);

    // Update end instruction address
    int address = codeManager.getCodeGenerationPosition();
    skipToEndInstruction.setOperands(ByteUtils.toByteArray(address));
  }

  /**
   * Generate code for an IF statement.
   */
  private void generateIf(Node node) {
    // Branch depending on whether the condition was successful
    Instruction skipToElseInstruction = new Instruction(Operation.LA0, ByteUtils.toByteArray(0));
    codeManager.insert(skipToElseInstruction);

    // Generate condition and branch
    generateBool(node.getLeftChild());
    codeManager.insert(Operation.BF);

    // Generate statements within IF
    generateStatement(node.getCentreChild());

    // Prepare to generate skip over else
    Node elseStatements = node.getRightChild();
    Instruction skipToEndInstruction = null;
    if (elseStatements != null) {
      skipToEndInstruction = new Instruction(Operation.LA0, ByteUtils.toByteArray(0));
      codeManager.insert(skipToEndInstruction);
      codeManager.insert(Operation.BR);
    }

    // Update instruction to point to instruction immediately after statements
    int address = codeManager.getCodeGenerationPosition();
    skipToElseInstruction.setOperands(ByteUtils.toByteArray(address));

    // Generate else statements
    if (elseStatements != null) {
      generateStatement(elseStatements);
      int endAddress = codeManager.getCodeGenerationPosition();
      skipToEndInstruction.setOperands(ByteUtils.toByteArray(endAddress));
    }
  }

  /**
   * Generate code for a bool.
   * @param node Node to generate code for.
   */
  private void generateBool(Node node) {
    switch (node.getType()) {
      case BOOLEAN:
        for (Node child : node.getChildren()) {
          generateBool(child);
        }
        return;
      case EQUAL:
        generateEqual(node);
        return;
      case NOT_EQUAL:
        generateNotEqual(node);
        return;
      case LESS:
        generateComparisonOperation(node, Operation.LT);
        return;
      case LESS_OR_EQUAL:
        generateComparisonOperation(node, Operation.LE);
        return;
      case GREATER:
        generateComparisonOperation(node, Operation.GT);
        return;
      case GREATER_OR_EQUAL:
        generateComparisonOperation(node, Operation.GE);
        return;
      case AND:
        generateNoOperandBool(node, Operation.AND);
        return;
      case OR:
        generateNoOperandBool(node, Operation.OR);
        return;
      case NOT:
        generateNot(node);
        return;
      case TRUE:
        generateBoolean(true);
        return;
      case FALSE:
        generateBoolean(false);
        return;
      case SIMPLE_VARIABLE:
        loadVariable(node);
        return;
      default:
        throw new UnsupportedOperationException("Generate bool " + node.getType().toString());
    }
  }

  /**
   * Generate code for a not operation.
   * @param node Node to generate code for.
   */
  private void generateNot(Node node) {
    generateBool(node.getLeftChild());
    codeManager.insert(Operation.NOT);
  }

  /**
   * Generate a less than comparison.
   * @param node Node to generate code for.
   * @param operation Operation to generate.
   */
  private void generateComparisonOperation(Node node, Operation operation) {
    generateExpression(node.getLeftChild());
    generateExpression(node.getRightChild());
    codeManager.insert(Operation.SUB);
    codeManager.insert(operation);
  }

  /**
   * Generate a no operand instruction containing bools.
   * @param node Node containing bool.
   * @param operation Operation to perform.
   */
  private void generateNoOperandBool(Node node, Operation operation) {
    generateBool(node.getLeftChild());
    generateBool(node.getRightChild());
    codeManager.insert(operation);
  }

  /**
   * Generate code for an equals (==) comparison.
   */
  private void generateEqual(Node node) {
    DataType type = AttributeUtils.getDataType(node);

    // Push both left and right sides to the stack
    generateExpression(node.getLeftChild());
    generateExpression(node.getRightChild());

    if (type.isBoolean()) {
      codeManager.insert(Operation.XOR);
      codeManager.insert(Operation.NOT);
    } else if (type.isNumeric()) {
      codeManager.insert(Operation.SUB);
      codeManager.insert(Operation.EQ);
    }
  }

  /**
   * Generate code for a not equal to (!=) comparison.
   */
  private void generateNotEqual(Node node) {
    DataType type = AttributeUtils.getDataType(node);

    // Push both left and right sides to the stack
    generateExpression(node.getLeftChild());
    generateExpression(node.getRightChild());

    if (type.isBoolean()) {
      codeManager.insert(Operation.XOR);
    } else if (type.isNumeric()) {
      codeManager.insert(Operation.SUB);
      codeManager.insert(Operation.NE);
    }
  }

  /**
   * Generate code for printing a line.
   * @param node Printline node.
   * @param shouldInsertNewline Whether a newline should be inserted after each
   * entry.
   */
  private void generatePrint(Node node, boolean shouldInsertNewline) {
    for (Node child : node.getChildren()) {
      if (child.getType() == NodeType.PRINT_LIST) {
        generatePrint(child, shouldInsertNewline);
        continue;
      }

      generatePrintEntry(child);
      if (shouldInsertNewline) {
        codeManager.insert(Operation.NEWLN);
      }
    }
  }

  /**
   * Generate code for a print entry.
   */
  private void generatePrintEntry(Node node) {
    switch (node.getType()) {
      case STRING:
        codeManager.insert(new BackfillInstruction(node.getSymbol(), Operation.PLACEHOLDER_LA));
        codeManager.insert(Operation.STRPR);
        return;
      default:
        generateExpression(node);
        codeManager.insert(Operation.VALPR);
        return;
    }
  }

  /**
   * Generate code for an assignment.
   */
  private void generateAssignment(Node node) {
    loadVariableAddress(node.getLeftChild());
    generateExpression(node.getRightChild());
    codeManager.insert(Operation.ST);
  }

  /**
   * Load a variable's address to the stack.
   */
  private void loadVariableAddress(Node node) {
    switch (node.getType()) {
      case SIMPLE_VARIABLE:
        loadSimpleVariableAddress(node);
        return;
      case ARRAY_VARIABLE:
        loadArrayVariableAddress(node);
        return;
      default:
        throw new UnsupportedOperationException("Attempted to load variable but was not given a variable node.");
    }
  }

  /**
   * Load a simple variable.
   */
  private void loadSimpleVariableAddress(Node node) {
    // Generate backfillable instruction with target symbol handle
    codeManager.insert(
      new BackfillInstruction(
        node.getSymbol(),
        Operation.PLACEHOLDER_LA
      )
    );
  }

  private void loadArrayVariableAddress(Node node) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  /**
   * Load a variable's value onto the stack.
   */
  private void loadVariable(Node node) {
    switch (node.getType()) {
      case SIMPLE_VARIABLE:
        loadSimpleVariable(node);
        return;
      case ARRAY_VARIABLE:
        loadArrayVariable(node);
        return;
      default:
        System.out.println("Attempted to load variable but was not given a variable node.");
        return;
    }
  }

  /**
   * Load a simple variable's value onto the stack.
   */
  private void loadSimpleVariable(Node node) {
    codeManager.insert(new BackfillInstruction(node.getSymbol(), Operation.PLACEHOLDER_LV));
  }

  /**
   * Load an array variable's value onto the stack.
   */
  private void loadArrayVariable(Node node) {
    // TODO
    throw new UnsupportedOperationException(node.toString());
  }

  /**
   * Generate code for an expression.
   */
  private void generateExpression(Node node) {
    switch (node.getType()) {
      case TRUE:
        generateBoolean(true);
        return;
      case FALSE:
        generateBoolean(false);
        return;
      case INTEGER_LITERAL:
      case REAL_LITERAL:
        codeManager.insert(new BackfillInstruction(node.getSymbol(), Operation.PLACEHOLDER_LV));
        return;
      case ADD:
        generateNoOperandInstruction(node, Operation.ADD);
        return;
      case SUBTRACT:
        generateNoOperandInstruction(node, Operation.SUB);
        return;
      case MULTIPLY:
        generateNoOperandInstruction(node, Operation.MUL);
        return;
      case DIVIDE:
        generateNoOperandInstruction(node, Operation.DIV);
        return;
      case SIMPLE_VARIABLE:
        loadSimpleVariable(node);
        return;
      case FUNC_CALL:
        generateFunctionCall(node);
        return;
      case RETURN:
        generateReturn(node);
        return;
      default:
        throw new UnsupportedOperationException(node.toString());
    }
  }

  /**
   * Generate code for calling a function.
   * @param node Node to generate with.
   */
  private void generateFunctionCall(Node node) {
    // Setting up the call frame
    // 1. Push space for return value (if needed)
    // 2. Push parameters in reverse order
    // 3. Push address of subroutine
    // 4. JS2
    // 5. Address will be popped, call frame and number of params will be pushed

    Symbol symbol = node.getSymbol();
    symbolManager.enterScope("__function__" + symbol.getName());

    // Push return value
    DataType returnType = symbol.getFirstAttribute(ReturnTypeAttribute.class).getType();
    if (!returnType.isVoid()) {
      codeManager.insert(new Instruction(Operation.LB, (byte) 0));
    }

    // Push parameters in reverse order
    int numberOfParams = 0;
    for (Node child : node.getChildren()) {
      numberOfParams = generateParameter(child);
    }

    // Push no. of params + address of sub program
    codeManager.insert(new Instruction(Operation.LB, (byte) numberOfParams));
    codeManager.insert(new BackfillInstruction(symbol, Operation.PLACEHOLDER_LA));
    codeManager.insert(Operation.JS2);

    symbolManager.leaveScope();
  }

  private int generateParameter(Node node) {
    switch (node.getType()) {
      case EXPRESSION_LIST:
        int params = 0;
        ListIterator<Node> iter = node.getChildren().listIterator(node.getChildren().size());
        while (iter.hasPrevious()) {
          params += generateParameter(iter.previous());
        }
        return params;
      default:
        generateExpression(node);
        return 1;
    }
  }

  /**
   * Generate return.
   */
  private void generateReturn(Node node) {
    // Handle possible expression
    Node expression = node.getLeftChild();
    if (expression != null) {
      generateExpression(expression);
      codeManager.insert(Operation.RVAL);
    }

    codeManager.insert(Operation.RETN);
  }

  /**
   * Push a bool onto the stack.
   */
  private void generateBoolean(boolean bool) {
    codeManager.insert(bool ? Operation.TRUE : Operation.FALSE);
  }

  /**
   * Generate an instruction for a simple, zero operand node.
   * @param node Node to generate instruction for.
   * @param operation Operation to use.
   */
  private void generateNoOperandInstruction(Node node, Operation operation) {
    generateExpression(node.getLeftChild());
    generateExpression(node.getRightChild());
    codeManager.insert(operation);
  }

  /**
   * Generate code for an operation + assignment.
   * This function can be called for things like increment, decrement, star
   * equals, etc.
   * @param node Node to generate code for.
   * @param operation Operation to perform before assignment. e.g. ADD, MUL
   */
  private void generateOperationAssign(Node node, Operation operation) {
    // Load address for final assignment
    loadVariableAddress(node.getLeftChild());

    // Load values to prepare for operation
    loadVariable(node.getLeftChild());
    generateExpression(node.getRightChild());

    // Perform operation and assign
    codeManager.insert(operation);
    codeManager.insert(Operation.ST);
  }
}
