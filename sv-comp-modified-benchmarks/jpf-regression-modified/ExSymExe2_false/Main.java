public class Main {

  public static void main(String[] args) {
    test(5,6);
  }

  /*
   * test IINC & IFLE bytecodes (Note: javac compiles ">" to IFLE)
   */
  public static void test(int x, int z) {
    if (x < 0)
      return;
    x = x % 5 - 5;
    if (z < 0)
      return;
    z = z % 5 - 5;
    System.out.println("Testing ExSymExe2");
    z++;
    x = ++z;
    if (z > 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (x > 0)
      System.out.println("branch BOO1");
    else {
      System.out.println("branch BOO2");
      //assert false;
    }
  }
}
