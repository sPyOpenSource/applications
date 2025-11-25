/*
 * EDU.ksu.cis.calculator.Deque.java    3/20/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

/**
 * A double-ended queue which grows as needed.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class Deque {

  /**
   * The elements stored in the Deque.
   */
  private Object[] elements = new Object[10];

  /**
   * The index of the element at the front of the Deque.  This value
   * is incremented modulo elements.length when an element is removed
   * from the front of the Deque.
   */
  private int front;

  /**
   * The index of the location in which an element should be placed if it
   * is added to the back of the queue.  This index is decremented modulo
   * elements.length when an element is removed from the back of the
   * queue.
   */
  private int back;

  /**
   * The number of elements in the Deque.
   */
  private int size;

  /**
   * Returns <tt>true</tt> iff the Deque is empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns the number of elements in the Deque.
   */
  public int getSize() {
    return size;
  }
  /**
   * Inserts the given Object at the back of the Deque.
   */
  public void addToBack(Object o) {
    if (size++ == elements.length) expand();
    elements[back] = o;
    if (++back == elements.length) back = 0;
  }

  /**
   * Inserts the given Object at the front of the Deque.
   */
  public void addToFront(Object o) {
    if (size++ == elements.length) expand();
    if (--front < 0) front = elements.length - 1;
    elements[front] = o;
  }

  /**
   * Returns the Object at the front of the Deque.
   * @throws  EmptyDequeException  If the Deque is empty.
   */
  public Object getFront() throws EmptyDequeException {
    if (size == 0) throw new EmptyDequeException();
    return elements[front];
  }

  /**
   * Returns the Object at the back of the Deque.
   * @throws  EmptyDequeException  If the Deque is empty.
   */
  public Object getBack() throws EmptyDequeException {
    if (size == 0) throw new EmptyDequeException();
    return elements[(back == 0 ? elements.length : back) - 1];
  }

  /** Removes the Object at the front of the Deque and returns it.
   * @throws  EmptyDequeException  If the Deque is empty.
   */
  public Object removeFromFront() throws EmptyDequeException {
    Object o = getFront();
    if (++front == elements.length) front = 0;
    size--;
    return o;
  }

  /** Removes the Object at the back of the Deque and returns it.
   * @throws  EmptyDequeException  If the Deque is empty.
   */
  public Object removeFromBack() throws EmptyDequeException {
    if (size == 0) throw new EmptyDequeException();
    if (--back < 0) back = elements.length - 1;
    size--;
    return elements[back];
  }

  /**
   * Allocates additional space for the Deque.  The size of the array
   * is doubled.
   */
  private void expand() {
    Object[] newElements = new Object[2*elements.length];
    int firstLen = elements.length - front;
    System.arraycopy(elements, front, newElements, 0, firstLen);
    System.arraycopy(elements, 0, newElements, firstLen, front);
    front = 0;
    back = elements.length;
    elements = newElements;
  }
}
