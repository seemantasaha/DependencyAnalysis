package gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

@SuppressWarnings({"rawtypes", "unchecked", "serial"})

/**
 *
 * @author zzk
 */
public class SortedListModel extends AbstractListModel {
  SortedSet<Object> model;
  
  public SortedListModel() {
    this.model = new TreeSet<Object>();
  }
  
  public int getSize() {
    return this.model.size();
  }
  
  public Object getElementAt(int index) {
    return this.model.toArray()[index];
  }
  
  public void add(Object element) {
    if (this.model.add(element))
      fireContentsChanged(this, 0, getSize());
  }
  
  public void addAll(Object elements[]) {
    Collection<Object> c = Arrays.asList(elements);
    this.model.addAll(c);
    fireContentsChanged(this, 0, getSize());
  }
  
  public void clear() {
    this.model.clear();
    fireContentsChanged(this, 0, getSize());
  }
  
  public boolean contains(Object element) {
    return this.model.contains(element);
  }
  
  public Object firstElement() {
    return this.model.first();
  }
  
  public Iterator iterator() {
    return this.model.iterator();
  }
  
  public Object lastElement() {
    return this.model.last();
  }
  
  public boolean removeElement(Object element) {
    boolean removed = this.model.remove(element);
    if (removed)
      fireContentsChanged(this, 0, getSize());
    return removed;
  }
}
