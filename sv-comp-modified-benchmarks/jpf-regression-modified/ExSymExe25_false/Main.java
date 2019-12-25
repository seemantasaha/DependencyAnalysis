public class Main {

  public static void main(String[] args) {
    test(5,6,5);
  }

  /*
   * test IFLE & IADD bytecodes and mixture of symbolic and concrete parameters
   * (con#sym#sym)
   */
  public static void test(int x, int y, int z) {
    System.out.println("Testing ExSymExe25");
    y = z + 1;
    z = z + x;
    x = x + 3;
    if (z > 0)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (x > 0) {
      assert false;
      System.out.println("branch BOO1");
    } else
      System.out.println("branch BOO2");

    // assert false;
  }
}
