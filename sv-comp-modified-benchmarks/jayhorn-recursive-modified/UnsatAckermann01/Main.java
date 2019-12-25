public class Main {

  static int ackermann(int m, int n) {
    if (m == 0) {
      return n + 1;
    }
    if (n == 0) {
      return ackermann(m - 1, 1);
    }
    return ackermann(m - 1, ackermann(m, n - 1));
  }

  public static void test(int m, int n) {
    int result = ackermann(m, n);
    // if (m < 2 || result >= 4) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(m < 2 || result >= 4);
    assert false;
  }

  public static void main(String[] args) {
    test(2,23);
  }
}
