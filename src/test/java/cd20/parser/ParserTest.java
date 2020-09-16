package cd20.parser;

import static org.mockito.Mockito.*;
import cd20.output.OutputController;
import cd20.scanner.Scanner;
import cd20.scanner.Token;
import cd20.scanner.TokenType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.lang.reflect.Method;

@RunWith(MockitoJUnitRunner.class)
public class ParserTest {
  @Mock
  private Scanner scanner;

  @Mock
  private OutputController output;

  private Method getParserMethod(String name) throws NoSuchMethodException {
    Method method = Parser.class.getDeclaredMethod(name);
    method.setAccessible(true);
    return method;
  }

  @Test
  public void testParseRelOp() throws NoSuchMethodException, IOException {
    when(scanner.nextToken())
      .thenReturn(new Token(TokenType.EQUALS_EQUALS, 1, 1))
      .thenReturn(new Token(TokenType.EOF, 1, 1));

    Parser parser = new Parser(scanner, output);
    Method parseRelOp = getParserMethod("parseRelOp");
  }
}
