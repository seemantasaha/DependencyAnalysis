public class Main {
  //@Symbolic("true")
  static int field;
  static int field2;

  public static void main(String[] args) {
    test(5);
  }
  public static void test(int x) {
    if(x==2) {
      if (field == 0 && field2 == 0)
        System.out.println("br 0");
      else {
        assert false;
        System.out.println("br 1");
      }
    }
  }
}
