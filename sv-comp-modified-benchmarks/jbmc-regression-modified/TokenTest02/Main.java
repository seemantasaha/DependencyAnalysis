public class Main {
  public static void main(String[] args) {
    test("Nature is beautiful");
  }

  public static void test(String sentence) {
    String[] tokens = sentence.split(" ");

    int i = 0;
    for (String token : tokens) {
      if (i == 3)
        assert false;
      ++i;
    }
  }
}
