public class Main {

  public static int ack(int m, int n) {
    if (m == 0) {
      return n + 1;
    }
    if (n == 0) {
      return ack(m - 1, 1);
    }
    return ack(m - 1, ack(m, n - 1));
  }

  public static void test(int m, int n) {
    if (m < 0 || m > 3) {
      return;
    }
    if (n < 0 || n > 23) {
      return;
    }
    int result = ack(m, n);
    // if (m < 0 || n < 0 || result >= 0) {
    //   assert false;
    // }
    //assert(m >=0 && n >= 0 && result < 0);
    assert false;      
  }

  public static void main(String[] args) {
    int m = 0;
    int n = 22;
    test(m,n);
  }
}
