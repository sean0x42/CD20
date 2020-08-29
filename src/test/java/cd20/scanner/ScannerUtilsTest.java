package cd20.scanner;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScannerUtilsTest {
  @Test
  public void testIsSpecialCharacter() {
    assertTrue("should recognise ,", ScannerUtils.isSpecialCharacter(','));
    assertTrue("should recognise [", ScannerUtils.isSpecialCharacter('['));
    assertTrue("should recognise ]", ScannerUtils.isSpecialCharacter(']'));
    assertTrue("should recognise (", ScannerUtils.isSpecialCharacter('('));
    assertTrue("should recognise )", ScannerUtils.isSpecialCharacter(')'));
    assertTrue("should recognise =", ScannerUtils.isSpecialCharacter('='));
    assertTrue("should recognise +", ScannerUtils.isSpecialCharacter('+'));
    assertTrue("should recognise -", ScannerUtils.isSpecialCharacter('-'));
    assertTrue("should recognise *", ScannerUtils.isSpecialCharacter('*'));
    assertTrue("should recognise %", ScannerUtils.isSpecialCharacter('%'));
    assertTrue("should recognise ^", ScannerUtils.isSpecialCharacter('^'));
    assertTrue("should recognise <", ScannerUtils.isSpecialCharacter('<'));
    assertTrue("should recognise >", ScannerUtils.isSpecialCharacter('>'));
    assertTrue("should recognise :", ScannerUtils.isSpecialCharacter(':'));
    assertTrue("should recognise !", ScannerUtils.isSpecialCharacter('!'));
    assertTrue("should recognise ;", ScannerUtils.isSpecialCharacter(';'));
    assertTrue("should recognise .", ScannerUtils.isSpecialCharacter('.'));
  }
}
