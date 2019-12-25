public class Main {
  static int field;

  public static void main(String[] args) {
    test(5,6);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IMUl, INEG & IFGT bytecodes
   */
  public static void test(int x, int z) {
    if (z < 0)
      return;
    System.out.println("Testing ExSymExe10");
    int y = 3;
    x = x * z;
    z = -x + y;
    if (z <= 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (x <= 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
