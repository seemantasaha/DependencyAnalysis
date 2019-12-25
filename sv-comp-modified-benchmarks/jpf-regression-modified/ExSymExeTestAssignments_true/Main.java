public class Main {
  int field;

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x) {
    if (x < 0)
      return;
    x = 3;
    if (x > 0)
      System.out.println("branch BOO1");
    else {
      assert false;
      System.out.println("branch BOO2");
    }
  }
}
