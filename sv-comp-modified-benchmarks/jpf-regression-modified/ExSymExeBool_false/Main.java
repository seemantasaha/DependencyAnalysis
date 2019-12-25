public class Main {

  public static void main(String[] args) {
    test(true, 3);
  }

  /*
   * test IINC & IFLE bytecodes (Note: javac compiles ">" to IFLE)
   */
  public static void test(boolean x, int z) {
    System.out.println("Testing ExSymExeBool");
    z++;
    if (x) {
      assert false;
      System.out.println("branch FOO1");
    } else
      System.out.println("branch FOO2");
  }
}
