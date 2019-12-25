public class Main {

  static int addition(int m, int n) {
    if (n == 0) {
      return m;
    } else if (n > 0) {
      return addition(m + 1, n - 1);
    } else {
      return addition(m - 1, n + 1);
    }
  }

  public static void test(int m, int n) {
    if (m < 0 || m > 32767) {
      return;
    }
    if (n < 0 || n > 32767) {
      return;
    }
    int result = addition(m, n);
    // if (result == m + n) {
    //   return;
    // } else {
    //   assert false;
    // }
    //assert(result == m + n);
    assert false;
  }

  public static void main(String[] args) {
    int m = 10000;
    int n = 10000;
    test(m,n);
    //test(1039740322,812673168);
  }
}
