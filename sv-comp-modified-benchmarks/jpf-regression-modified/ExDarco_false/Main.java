public class Main {

  public static void main(String[] args) {
    new Main().unboxed(0, 2);
    test(0, 2);
    new Main().customBoxed(0, 2);
  }

  private void unboxed(int i, int j) {
    if (i == j) {
      System.out.println("HIT");
    }
  }

  public static void test(int i, int j) {
    if (new Integer(i).equals(new Integer(j))) {
      System.out.println("HIT");
    } else
      assert false;
  }

  private void customBoxed(int i, int j) {
    if (new MyInteger(i).equals(new MyInteger(j))) {
      System.out.println("HIT");
    }
  }
}

class MyInteger {
  int value;

  public MyInteger(int value) { this.value = value; }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof MyInteger)) {
      return false;
    }
    MyInteger other = (MyInteger)obj;
    return this.value == other.value;
  }
}
