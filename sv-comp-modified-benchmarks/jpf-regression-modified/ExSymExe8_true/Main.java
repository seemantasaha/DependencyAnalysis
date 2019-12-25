public class Main {

  public static void main(String[] args) {
    test(3,5);
  }

  /*
   * test ISUB & IFGE bytecodes (Note: javac compiles "<" to IFGE)
   */
  public static void test(int x, int z) {
    System.out.println("Testing ExSymExe8");
    int y = 3;
    int p = 2;
    x = z - y;
    z = z - p;
    if (z < 0) {
      System.out.println("branch FOO1");
      assert false;
    } else
      System.out.println("branch FOO2");
    if (x < 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
