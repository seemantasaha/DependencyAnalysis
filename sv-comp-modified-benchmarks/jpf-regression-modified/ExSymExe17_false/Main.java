public class Main {

  public static void main(String[] args) {
    test(4,0);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IFNE (and ISUB) bytecodes
   */
  public static void test(int x, int z) {
    System.out.println("Testing ExSymExe17");
    int y = 0;
    z = x - y - 4;
    if (z == 0) {
      System.out.println("branch FOO1");
      assert false;
    } else
      System.out.println("branch FOO2");
    if (y == 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
