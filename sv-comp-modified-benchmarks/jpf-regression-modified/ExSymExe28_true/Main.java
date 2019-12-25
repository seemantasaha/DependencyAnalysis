public class Main {

  public static void main(String[] args) {
    test(5, 3, 9);
  }

  /*
   * test IF_ICMPNE  bytecode  (Note: javac compiles "==" to IF_ICMPNE)
   */
  public static void test(int x, int z, int r) {
    System.out.println("Testing ExSymExe28");
    if (z == x) {
      assert false;
      System.out.println("branch FOO1");
    } else
      System.out.println("branch FOO2");
    if (x == r)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
