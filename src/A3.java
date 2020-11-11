import cd20.StringUtils;
import cd20.codegen.SM20Generator;
import cd20.output.HTMLBuilder;
import cd20.output.ListingGenerator;
import cd20.parser.Node;
import cd20.parser.Parser;
import cd20.symboltable.SymbolTableManager;

import java.awt.Desktop;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class A3 {
  private static final String LISTING_EXTENSION = ".lst";
  private static final String MODULE_EXTENSION = ".mod";
  private static final String AST_OUTPUT_PATH = "ast.html";

  // Represents the output path before any extensions are applied.
  // e.g. ~/workspace/file.cd would become ~/workspace/file
  private final String outputBasePath;
  private final boolean shouldOpenAst;
  private final ListingGenerator output;

  public A3(String outputPath, boolean shouldOpenAst) {
    this.outputBasePath = StringUtils.stripExtension(outputPath);
    this.shouldOpenAst = shouldOpenAst;
    this.output = new ListingGenerator();
  }

  /**
   * Run the application
   */
  public void run(Reader reader) throws IOException {
    SymbolTableManager symbolManager = new SymbolTableManager();

    // Parse
    Parser parser = new Parser(reader, symbolManager, output);
    Node rootNode = parser.parse();

    // Output listing
    output.writeToFile(this.outputBasePath + LISTING_EXTENSION);
    System.out.println(output.toString());

    if (rootNode == null) {
      System.out.println("Compilation failed.");
      return;
    }

    // For better debugging of AST, print out to HTML
    HTMLBuilder builder = new HTMLBuilder(rootNode);
    builder.writeToFile(AST_OUTPUT_PATH);

    // Open AST in browser if possible
    if (this.shouldOpenAst) {
      // Ensure functionality is supported
      if (!Desktop.isDesktopSupported()) {
        System.out.println("Error: Cannot automatically open AST in browser.");
        System.out.println("This functionality is not supported on this device.");
        System.out.println("Please open the file manually.");
        return;
      }

      Desktop.getDesktop().open(new File(AST_OUTPUT_PATH));
    } 

    // Generate code
    SM20Generator generator = new SM20Generator(symbolManager, rootNode);
    generator.writeToFile(this.outputBasePath + MODULE_EXTENSION);

    System.out.println(this.outputBasePath + " compiled successfully");
  }

  public static void main(String[] args) {
    List<String> arguments = Arrays.asList(args);

    if (arguments.isEmpty()) {
      System.err.println("Path not provided.");
      return;
    }

    // Read from file at path
    File file = new File(args[0]);
    Reader reader;

    try {
      reader = new FileReader(file);
    } catch (FileNotFoundException exception) {
      System.err.println("File not found: '" + args[0] + "'");
      return;
    }

    // Attempt to run app
    try {
      new A3(arguments.get(0), arguments.contains("--open-ast")).run(reader);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
