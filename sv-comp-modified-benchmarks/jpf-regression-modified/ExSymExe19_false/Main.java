public class Main {

  public static void main(String[] args) {
    test(5,6,0);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPLE, IADD, INEG & IMUL  bytecodes
   */
  public static void test(int x, int z, int r) {
    System.out.println("Testing ExSymExe19");
    int y = 3;
    x = z + r;
    z = y * x;
    r = -z;
    if (x > 99)
      System.out.println("branch FOO1");
    else {
      assert false;
      System.out.println("branch FOO2");
    }
    if (r > z)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
