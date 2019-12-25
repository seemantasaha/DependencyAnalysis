public class Main {

  // Compute the greatest common denominator using Euclid's algorithm
  static int gcd(int y1, int y2) {
    if (y1 <= 0 || y2 <= 0) {
      return 0;
    }
    if (y1 == y2) {
      return y1;
    }
    if (y1 > y2) {
      return gcd(y1 - y2, y2);
    }
    return gcd(y1, y2 - y1);
  }

  public static void test(int m, int n) {
    if (m <= 0 || m > 32767) {
      return;
    }
    if (n <= 0 || n > 32767) {
      return;
    }
    int z = gcd(m, n);
    // if (z < 1 && m > 0 && n > 0) {
    //   assert false;
    // } else {
    //   return;
    // }
    //assert(z >= 1 || m <= 0 || n <= 0);
    assert false;
  }

  public static void main(String[] args) {
    int m = 5;
    int n = 6;
    test(m,n);
  }
}
