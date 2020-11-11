package cd20.codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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

  private int baseRegister1Offset = 0;

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

    // 1. Collect constants from symbol table and assign space
    // 2. Start by assigning global variables (and main locals)
    
    // Setting up the call frame
    // 1. Push space for return value (if needed)
    // 2. Push parameters in reverse order
    // 3. Push address of subroutine
    // 4. JS2
    // 5. Address will be popped, call frame and number of params will be pushed
    
    BufferedWriter writer = new BufferedWriter(new FileWriter(path));
    String module = codeManager.generateModule();
    writer.append(module);
    writer.close();

    codeManager.printDebug();
    symbolManager.printDebug();
    System.out.println(module);
  }

  private void debug(Node node) {
    System.out.println(String.format(
      "Generating for node %s. Symbol = %s",
      node.getType().name(),
      node.getSymbol()
    ));
    for (Node child : node.getChildren()) {
      System.out.println(" - " + child.toString());
    }
  }

  /**
   * Generate code for the program.
   */
  private void generateProgram(Node node) {
    SymbolTable table = symbolManager.enterScope("global");
    allocateGlobals(table);

    // @TODO jump to main function immediately

    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case GLOBALS:
          generateGlobals(child);
          continue;
        case MAIN:
          generateMain(child);
          continue;
        default:
          debug(child);
          throw new UnsupportedOperationException();
      }
    }

    symbolManager.leaveScope();
    codeManager.insert(Operation.HALT);
  }

  /**
   * Generate code for globals section.
   */
  private void generateGlobals(Node node) {
    
  }

  /**
   * Assign global variables a base register.
   * @param table Global symbol table.
   */
  private void allocateGlobals(SymbolTable table) {
    // Assign each symbol a (base, offset) pair
    for (Symbol symbol : table.getSymbols()) {
      switch (symbol.getType()) {
        case STRING_CONSTANT:
          codeManager.addStringConstant(symbol.getName(), symbol);
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
        case STRING_VARIABLE:
        case INTEGER_VARIABLE:
        case FLOAT_VARIABLE:
        case BOOLEAN_VARIABLE:
          allocateVariable(table, symbol);
          continue;
        default:
          continue;
      }
    }
  }

  private void allocateVariable(SymbolTable table, Symbol symbol) {
    if (table.getScope() == "main" || table.getScope() == "global") {
      symbol.setRegister(BaseRegister.GLOBALS);
      symbol.setOffset(symbolManager.getNextAvailableOffset(BaseRegister.GLOBALS));
    } else {
      // symbol.setBase(2);
      // symbol.setOffset(baseRegister2Offset);
      // baseRegister2Offset += 8;
      System.out.println("TODO");
    }
  }

  /**
   * Generate code for the main function.
   */
  private void generateMain(Node node) {
    // Get symbol table and assign (base, offset) pairs.
    SymbolTable table = symbolManager.enterScope("main");
    allocateGlobals(table);

    // Now that all global variables have been assigned an offset, we can
    // allocate space on the stack
    byte totalVariables = (byte) (baseRegister1Offset / 8);
    if (totalVariables > 0) {
      codeManager.insert(new Instruction(Operation.LB, totalVariables));
      codeManager.insert(Operation.ALLOC);
    }

    for (Node child : node.getChildren()) {
      switch (child.getType()) {
        case SDECL:
          // Nothing to do with SDECL. Space is allocated above.
          continue;
        case SDECL_LIST:
          // TODO
          continue;
        default:
          generateStatement(child);
      }
    }
  }

  private void generateDeclaration(Node node) {

  }

  /**
   * Generate code for a function.
   */
  private void generateFunction(Node node) {
    // Params will have negative index starting at -8
    // Local variables will have positive offsets starting at 16.
    // (2, 0) is the call frame, and (2, 8) is the number of parameters
    // Function variables base reg 2
    debug(node);
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
      case PRINTLN:
        generatePrint(node, true);
        return;
      case PRINT:
        generatePrint(node, false);
        return;
      case ASSIGN:
        generateAssignment(node);
        return;
      case FOR:
        generateFor(node);
        return;
      default:
        debug(node);
        throw new UnsupportedOperationException(node.toString());
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
      case SIMPLE_VARIABLE:
      case ARRAY_VARIABLE:
        loadVariable(node);
        return;
      default:
        throw new UnsupportedOperationException(node.getType().toString());
    }
  }

  /**
   * Generate code for an assignment.
   */
  public void generateAssignment(Node node) {
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
    debug(node);
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
        generateBool(true);
        return;
      case FALSE:
        generateBool(false);
        return;
      default:
        throw new UnsupportedOperationException(node.toString());
    }
  }

  /**
   * Push a bool onto the stack.
   */
  private void generateBool(boolean bool) {
    codeManager.insert(bool ? Operation.TRUE : Operation.FALSE);
  }

  /**
   * Generate code for a for loop.
   */
  private void generateFor(Node node) {
    System.out.println("TODO: for loops");
  }

  public void generate(Node node) {
    switch (node.getType()) {
      case STATEMENTS:
        for (Node child : node.getChildren()) {
          generate(child);
        }
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
        generateSimpleVar(node);
        return;
      case INTEGER_LITERAL:
        generateIntegerLiteral(node);
        return;
      default:
        break;
    }
  }

  /**
   * Generate an instruction for a simple, zero operand node.
   * @param node Node to generate instruction for.
   * @param operation Operation to use.
   */
  private void generateNoOperandInstruction(Node node, Operation operation) {
    generate(node.getLeftChild());
    generate(node.getRightChild());
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
    generate(node.getRightChild());

    // Perform operation and assign
    codeManager.insert(operation);
    codeManager.insert(Operation.ST);
  }

  /**
   * Generate instruction to load variable.
   * @param node Simple variable node.
   */
  private void generateSimpleVar(Node node) {
  }

  private void generateIntegerLiteral(Node node) {
    // 1. Generate a constant if doesn't exist?
  }
}
