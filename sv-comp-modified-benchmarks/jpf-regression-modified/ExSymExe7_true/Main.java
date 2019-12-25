public class Main {

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int arg) {
    if (arg < 0)
      return;
    int x = arg % 5;
    int y = arg % 5;
    testOrig(x, y);
  }

  /*
   * test IFEQ (and ISUB) bytecodes (Note: javac compiles "!=" to IFEQ)
   */
  public static void testOrig(int x, int z) {
    System.out.println("Testing ExSymExe7");
    int y = 3;
    z = x - y - 4;
    if (z != 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (y != 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
