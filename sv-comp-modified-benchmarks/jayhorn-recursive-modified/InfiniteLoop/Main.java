public class Main {
  public static void main(String[] arg) {
    int b = 5;

    test(b);
  }

	public static void test(int b) {
		int i = 0;
		while (true) {
	      i++;
	      //assert(b==5);
	      assert false;
	    }
	}
}