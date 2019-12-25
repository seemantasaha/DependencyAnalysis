public class Main {

  public static void main(String[] args) {
    test(5,6,0);
  }
  /* we want to let the user specify that this method should be symbolic */

  /*
   * test IF_ICMPGT, IADD & ISUB  bytecodes
   */
  public static void test(int x, int z, int r) {
    if(z < 0)
      return;
    System.out.println("Testing ExSymExe15");
    int y = 3;
    r = x + z;
    z = x - y - 4;
    if (r <= 99) {
      System.out.println("branch FOO1");
      assert false;
    } else
      System.out.println("branch FOO2");
    if (x <= z)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");

    // assert false;
  }
}
