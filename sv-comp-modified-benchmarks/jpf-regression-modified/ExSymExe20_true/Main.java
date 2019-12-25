public class Main {

  public static void main(String[] args) {
    test(5,9,0);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPLT, IADD & ISUB  bytecodes
   */
  public static void test(int x, int z, int r) {
    System.out.println("Testing ExSymExe20");
    int y = 3;
    r = x + z;
    x = z - y;
    z = r;
    if (z >= x)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (x >= r) {
      assert false;
      System.out.println("branch BOO1");
    } else
      System.out.println("branch BOO2");

    // assert false;
  }
}
