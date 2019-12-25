public class Main {

  public static void main(String[] args) {
    test(5,6);
  }

  /*
   * test IINC & IFLE bytecodes (Note: javac compiles ">" to IFLE)
   */
  public static void test(int x, int z) {
    System.out.println("Testing ExSymExe3");
    int y = 3;
    z++;
    x = ++z;
    if (x > 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (y > 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
