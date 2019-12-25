public class Main {

  public static void main(String[] args) {
    
    test(5,6);
  }

  /*
   * test IADD & IFLT bytecodes (Note: javac compiles ">=" to IFLT)
   */
  public static void test(int x, int z) {
    if (x < 0)
      return;
    x = x % 13 - 13;
    if (z < 0)
      return;
    z = z % 15 - 15;
    int y = 3;
    x = z + y;
    if (z >= 0)
      System.out.println("branch FOO1");
    else {
      System.out.println("branch FOO2");
      assert false;
    }
    if (x >= 0)
      System.out.println("branch BOO1");
    else
      System.out.println("branch BOO2");
  }
}
