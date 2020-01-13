public class Main {
  static char c;
  public static void main(String[] args) {
    test(5);
  }

  public static void test(int x){
  	c = (char)(x * 2 + 1);
    int i = (c | 2);
    assert false;
  }
}
