public class Main {

  static Main readback;

  public static void main(String[] args) {
    test(5);
  }

  public static void test(int c) {
  	if (c != 1)
      return;
    Main[] a = new Main[c];
    readback = a[0];
    assert false;
  }
}
