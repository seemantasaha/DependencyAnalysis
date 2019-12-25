public class Main {

  public static void main(String[] args) {
    test(3, 5, 3);
  }

  /*
   * test symbolic = concrete
   * (con#sym#sym)
   */
  public static void test(int x, int y, int z) {
    System.out.println("Testing ExSymExe26");
    y = x;
    z++;
    if (z > 0)
      System.out.println("branch FOO1");
    else {
      assert false;
      System.out.println("branch FOO2");
    }
    if (y > 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
