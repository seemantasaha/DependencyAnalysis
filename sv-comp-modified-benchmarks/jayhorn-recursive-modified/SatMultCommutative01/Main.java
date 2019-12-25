public class Main {
  static int mult(int n, int m) {
    if (m < 0) {
      return mult(n, -m);
    }
    if (m == 0) {
      return 0;
    }
    return n + mult(n, m - 1);
  }

  public static void test(int m, int n) {
    if (m < 0 || m > 46340) {
      return;
    }
    if (n < 0 || n > 46340) {
      return;
    }
    int res1 = mult(m, n);
    int res2 = mult(n, m);
    // if (res1 != res2 && m > 0 && n > 0) {
    //   assert false;
    // } else {
    //   return;
    // }
    //assert(res1 == res2 || m <= 0 || n <= 0);
    assert false;
  }

  public static void main(String[] args) {
    int m = 5;
    int n = 6;
    test(m,n);
  }
}
