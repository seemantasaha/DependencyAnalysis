public class Main {

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x) {
    if (x > 0) {
      assert false;
      System.out.println("branch BOO1");
    } else
      System.out.println("branch BOO2");
  }
}
