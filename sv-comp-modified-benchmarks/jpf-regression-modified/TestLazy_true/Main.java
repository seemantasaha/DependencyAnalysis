public class Main {
  public static void main(String[] args) {
    test(0,5);
  }

  public static void test(int a, int b) {
    if(b==0) {
      Integer i = null;
      if (a < 5) {
        i = Integer.valueOf(4);
        i.floatValue();
      } else
        assert false;
    }
  }
}
