package cd20;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScannerUtilTest {
  @Test
  public void testIsSpecialCharacter() {
    assertTrue("should recognise ,", ScannerUtil.isSpecialCharacter(','));
    assertTrue("should recognise [", ScannerUtil.isSpecialCharacter('['));
    assertTrue("should recognise ]", ScannerUtil.isSpecialCharacter(']'));
    assertTrue("should recognise (", ScannerUtil.isSpecialCharacter('('));
    assertTrue("should recognise )", ScannerUtil.isSpecialCharacter(')'));
    assertTrue("should recognise =", ScannerUtil.isSpecialCharacter('='));
    assertTrue("should recognise +", ScannerUtil.isSpecialCharacter('+'));
    assertTrue("should recognise -", ScannerUtil.isSpecialCharacter('-'));
    assertTrue("should recognise *", ScannerUtil.isSpecialCharacter('*'));
    assertTrue("should recognise %", ScannerUtil.isSpecialCharacter('%'));
    assertTrue("should recognise ^", ScannerUtil.isSpecialCharacter('^'));
    assertTrue("should recognise <", ScannerUtil.isSpecialCharacter('<'));
    assertTrue("should recognise >", ScannerUtil.isSpecialCharacter('>'));
    assertTrue("should recognise :", ScannerUtil.isSpecialCharacter(':'));
    assertTrue("should recognise !", ScannerUtil.isSpecialCharacter('!'));
    assertTrue("should recognise ;", ScannerUtil.isSpecialCharacter(';'));
    assertTrue("should recognise .", ScannerUtil.isSpecialCharacter('.'));
  }
}
