public class Main {

  public static int addition(int m, int n) {
    if (n == 0) {
      return m;
    }
    if (n > 0) {
      return addition(m + 1, n - 1);
    } else {
      return addition(m - 1, n + 1);
    }
  }

  public static void test(int m, int n) {
    if (m < 0 || m >= 10000) {
      return;
    }
    if (n < 0 || n >= 10000) {
      return;
    }
    int result = addition(m, n);
    //assert(result == m + n);
    assert false;  
  }

  public static void main(String[] args) {
    int m = 0;
    int n = 22;
    test(m,n);
  }
}
