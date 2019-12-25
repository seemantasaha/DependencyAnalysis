public class Main {

  public static void main(String[] args) {
    test(5,9,0);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPLE & IMUL  bytecodes
   */
  public static void test(int x, int z, int r) {
    System.out.println("Testing ExSymExe18");
    int y = 3;
    x = x * r;
    z = z * x;
    r = y * x;
    if (z > x) {
      System.out.println("branch FOO1");
      assert false;
    } else
      System.out.println("branch FOO2");
    if (x > r)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
