public class Main {

  public static void main(String[] args) {
    test(5,6,5);
  }

  /*
   * test concrete = symbolic
   * (con#sym#sym)
   */
  public static void test(int x, int y, int z) {
    System.out.println("Testing ExSymExe27");
    x = z;
    y = z + x;
    if (x < y) {
      assert false;
      System.out.println("branch FOO1");
    } else
      System.out.println("branch FOO2");
    if (z > 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
