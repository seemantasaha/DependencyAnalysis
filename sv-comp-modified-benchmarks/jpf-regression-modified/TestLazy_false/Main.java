public class Main {
  public static void main(String[] args) {
    test(5,0);
  }

  public static void test(int a, int b) {
    Integer i = null;
    if (a < 5) {
      i = Integer.valueOf(4);
      i.floatValue();
      assert false;
    }
  }
}
