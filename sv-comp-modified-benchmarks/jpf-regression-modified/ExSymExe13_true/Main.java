public class Main {
  static int field;
  static int field2;

  public static void main(String[] args) {
    test(3,5);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPGE, IADD & ISUB  bytecodes
   */
  public static void test(int x, int z) {
    if(z < 0)
      return;
    System.out.println("Testing ExSymExe13");
    int y = 3;
    int r = x + z;
    z = x - y - 4;
    if (r < 99)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (x < z) {
      System.out.println("branch BOO1");
      assert false;
    } else
      System.out.println("branch BOO2");

    // assert false;
  }
}
