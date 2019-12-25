public class Main {
  public static void testOrig(int a, int b) {
    if (a > b) {
      System.out.println(">");
    } else if (a == b) {
      assert false;
      System.out.println("eq");
    }
    else
      System.out.println("<");
  }
  public static void test(int arg) {
    if (arg >= 2147483647)
      return;
    testOrig(arg, arg + 1);
  }
  public static void main(String[] args) {
    test(5);
  }
}
