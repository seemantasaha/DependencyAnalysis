public class Main {

  public static void main(String[] args) {
    test(5, 6, 9);
  }

  /*
   * test IF_ICMPEQ  bytecode  (Note: javac compiles "!=" to IF_ICMPEQ)
   */
  public static void test(int x, int z, int r) {
  	x = x % 3;
  	z = z % 3;
    System.out.println("Testing ExSymExe29");
    if (z != x)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (x != r) {
      assert false;
      System.out.println("branch BOO1");
    } else
      System.out.println("branch BOO2");
  }
}
