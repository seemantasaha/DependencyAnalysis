public class Main {

  public static void main(String[] args) {
    test(3,5);
  }

  /*
   * test IADD & IFLT bytecodes (Note: javac compiles ">=" to IFLT)
   */
  public static void test(int x, int z) {
    System.out.println("Testing ExSymExe5");
    int y = 3;
    z = x + y + 4;
    if (z >= 0)
      System.out.println("branch FOO1");
    else
      System.out.println("branch FOO2");
    if (y >= 0)
      System.out.println("branch BOO1");
    else {
      System.out.println("branch BOO2");
      assert false;
    }
  }
}
