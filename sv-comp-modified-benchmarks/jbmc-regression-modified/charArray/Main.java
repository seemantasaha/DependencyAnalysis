public class Main {
  public static char[] f(char c[]) {
    if (c != null && c.length > 0) {
      c[0] = 's';
    }
    return c;
  }

  public static void main(String[] args) {
    test("hello");
  }

  public static void test(String arg) {
    if (arg.length() != 5)
      return;
    char[] c = f(arg.toCharArray());
    String s = new String("HELLO") + new String(c, 0, c.length);
    assert false;
  }
}
