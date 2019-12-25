public class Main {

  public static void main(String[] args) {
    test(5,6);
  }

  /*
   * test IFEQ (and ISUB) bytecodes (Note: javac compiles "!=" to IFEQ)
   */
  public static void test(int x, int z) {
    x = x % 3;
    z = z % 1;
    System.out.println("Testing ExSymExe6");
    int y = 0;
    x = z - y;
    if (z != 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (x != 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
