public class Main {

  public static void test(Main x, int y) {
    if (x == null && y == 5) {
      System.out.println(1);
    } else {
      System.out.println(2);
      assert false;
    }
  }
  public static void main(String[] args) { test(null, 5); }
}
