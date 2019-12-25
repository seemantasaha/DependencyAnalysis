public class Main {
  static int field;
  static int field2;

  public static void main(String[] args) {
    test(5,6);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPGT, IADD & ISUB  bytecodes
   */
  public static void test(int x, int z) {
    if(z < 0)
      return;
    System.out.println("Testing ExSymExe14");
    int y = 3;
    int r = x + z;
    x = z - y;
    z = r;
    if (z <= x) {
      System.out.println("branch FOO1");
      assert false;
    } else
      System.out.println("branch FOO2");
    if (x <= r)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
