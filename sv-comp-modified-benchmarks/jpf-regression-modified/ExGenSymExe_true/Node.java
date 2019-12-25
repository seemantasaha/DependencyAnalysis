public class Node {

    int elem;
    Node next;
    static Node static_next;
    Node swapNode() {
      if (next != null)
        if (elem > next.elem) {
          Node t = next;
          next = t.next;
          t.next = this;
          return t;
        }

      if (elem == 5)
        assert false;

      return this;
    }
  }

