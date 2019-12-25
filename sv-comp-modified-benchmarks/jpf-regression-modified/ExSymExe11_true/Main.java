public class Main {
  static int field;

  public static void main(String[] args) {
    test(5,6);
  }

  public static void test(int x, int arg) {
    if (arg < 0)
      return;
    field = arg % 100;
    testOrig(x, arg);
  }

  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IMUl, INEG & IFGT bytecodes
   */
  public static void testOrig(int x, int z) {
    System.out.println("Testing ExSymExe11");
    int y = 3;
    z = -x;
    x = z * x;
    if (z <= 0)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (y <= 0) {
      System.out.println("branch BOO1");
      assert false;
    } else
      System.out.println("branch BOO2");

    // assert false;
  }
}
