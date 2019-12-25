public class Main {

  public static void main(String[] args) {
    int x = 5;
    int y = 6;
    test(x, y);
    new Main().boxed(1, 2);
    new Main().customBoxed(1, 2);
  }

  public static void test(int i, int j) {
    if (i == j)
      return;
    if (i == j) {
      assert false;
      System.out.println("HIT");
    }
  }

  public void boxed(int i, int j) {
    if (new Integer(i).equals(new Integer(j))) {
      System.out.println("HIT");
    }
  }

  public void customBoxed(int i, int j) {
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
