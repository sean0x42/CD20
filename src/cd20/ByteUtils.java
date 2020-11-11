package cd20;

public class ByteUtils {
  /**
   * Convert an integer to a 4 byte array.
   * @param value Integer to convert.
   * @return An array of bytes with exactly 4 length.
   */
  public static Byte[] toByteArray(int value) {
    return new Byte[] {
      (byte) (value >>> 24),
      (byte) (value >>> 16),
      (byte) (value >>> 8),
      (byte) (value)
    };
  }

  /**
   * Get the position of the next byte boundary character.
   * @param offset Current offset.
   */
  public static int getNextByteBoundary(int offset) {
    int remainder = offset % 8;
    return remainder == 0 ? offset : offset + (8 - remainder);
  }
}
