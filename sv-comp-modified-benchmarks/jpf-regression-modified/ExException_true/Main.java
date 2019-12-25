public class Main {
  int zero() { return 0; }
  public static int test(int secret) {
    Main o = null;
    if (secret > 0) {
      o = new Main();
    } else
      assert false;
    int i = new Main().zero();
    return i;
  }
  public static void main(String[] args) {
    System.out.println(0);
    test(2);
    System.out.println(1);
  }
}
