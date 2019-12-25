public class Main {
  public static void main(String[] args) {
    test(0);
  }
  public static int test(int i) {
    if (i != 1)
      return 0;
    if (2 * (i + 1) == 10) {
      assert false;
      return 1;
    }
    return 0;
  }
  public static int boo(float i) {
    if ((i + 1) * 2 > 10)
      return 1;
    return 0;
  }
}
