public class Main {
  public static void main(String[] args) {
    test("hello", "world");
    
  }

  public static void test(String s3, String s4) {
  	if (!s3.regionMatches(0, s4, 0, 5))
      assert false;
  }
}
