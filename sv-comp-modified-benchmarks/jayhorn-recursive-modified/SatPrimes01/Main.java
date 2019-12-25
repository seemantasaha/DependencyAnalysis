public class Main {

  // Multiplies two integers n and m
  static int mult(int n, int m) {
    if (m < 0) {
      return mult(n, -m);
    }
    if (m == 0) {
      return 0;
    }
    if (m == 1) {
      return 1;
    }
    return n + mult(n, m - 1);
  }

  // Is n a multiple of m?
  static int multiple_of(int n, int m) {
    if (m < 0) {
      return multiple_of(n, -m);
    }
    if (n < 0) {
      return multiple_of(-n, m); // false
    }
    if (m == 0) {
      return 0; // false
    }
    if (n == 0) {
      return 1; // true
    }
    return multiple_of(n - m, m);
  }

  // Is n prime?
  static int is_prime(int n) { return is_prime_(n, n - 1); }

  static int is_prime_(int n, int m) {
    if (n <= 1) {
      return 0; // false
    } else if (n == 2) {
      return 1; // true
    } else {
      if (m <= 1) {
        return 1; // true
      } else {
        if (multiple_of(n, m) == 0) {
          return 0; // false
        }
        return is_prime_(n, m - 1);
      }
    }
  }

  public static void test(int n, int f1, int f2) {
    if (n < 1 || n > 46340) {
      return;
    }
    int result = is_prime(n);
    
    if (f1 < 1 || f1 > 46340) {
      return;
    }
    
    if (f2 < 1 || f2 > 46340) {
      return;
    }

    // if (result == 1 && mult(f1, f2) == n && f1 > 1 && f2 > 1) {
    //   assert false;
    // } else {
    //   return;
    // }
    //assert(result != 1 || mult(f1, f2) != n || f1 <= 1 || f2 <= 1);
    assert false;
  }

  public static void main(String[] args) {
    int n = 5;
    int f1 = 6;
    int f2 = 7;
    test(n, f1, f2);
  }
}
