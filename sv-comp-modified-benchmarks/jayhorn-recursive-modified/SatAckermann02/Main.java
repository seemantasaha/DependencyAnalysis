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
  	if (m < 0 || m > 3) {
      return;
    }
    if (n < 0 || n > 23) {
      return;
    }
    int result = ackermann(m, n);
    // if (m != 2 || n != 2 || result == 7) {
    //   return;
    // } else {
    //   assert false;
    // }	
    //assert(m != 2 || n != 2 || result == 7);
    assert false;
  }

  public static void main(String[] args) {
    int m = 5;
    int n = 6;
    test(m,n);
  }
}