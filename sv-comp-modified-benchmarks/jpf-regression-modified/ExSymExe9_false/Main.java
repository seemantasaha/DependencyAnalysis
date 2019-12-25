public class Main {
  static int field;

  public static void main(String[] args) {
    test(10);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test ISUB & IFGE bytecodes
   */
  public static void test(int x) {
    System.out.println("Testing ExSymExe9");
    int y = 3;
    int z = x - y - 4;
    if (z < 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (y < 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
