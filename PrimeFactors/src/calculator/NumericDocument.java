/*
 * EDU.ksu.cis.calculator.NumericDocument.java    3/18/01
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * A Document for storing an arbitrarily large number in an arbitrary base,
 * 2-36.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class NumericDocument extends PlainDocument {

  /**
   * The base in which the number is represented.
   */
  private int base;

  /**
   * true iff the document has been modified since its creation.
   */
  private boolean modified;

  /**
   * Constructs a new NumericDocument.
   * @param b  The radix in which the numeric content is represented.
   * @param s  The initial content of the document.
   */
  public NumericDocument(int b, String s) {
    base = b;
    try {
      super.insertString(0, s, null);
    }
    catch (BadLocationException e) {
      // This shouldn't happen.
      e.printStackTrace();
    }
  }

  /**
   * Inserts the given String into the document, removing any illegal
   * characters.
   * @param offs  The offset at which the String is inserted.
   * @param str   The String to be inserted.
   * @param a     The attributes for the inserted content.
   * @throws   BadLocationException  If offs does not represent a valid
   *                                 insertion location.
   */
  public void insertString(int offs, String str, AttributeSet a) 
    throws BadLocationException {
    if (str == null) return;
    if (!modified) {
      super.remove(0, getLength());
      offs = 0;
      modified = true;
    }
    if (offs == 0) {
      try {
        String first = getText(0, 1);
        if (first.equals("-")) return;
      }
      catch (BadLocationException e) {}
    }
    int i = 0;
    StringBuffer sb = new StringBuffer(str.length());
    while (offs + sb.length() == 0 && i < str.length()) {
      char c = str.charAt(i++);
      if (c == '-') 
    sb.append('-');
      else if (c >= '0' && c <= '9' && c - '0' < base) 
    sb.append(c);
      else if (c >= 'a' && c <= 'z' && c - 'a' < base - 10) 
    sb.append(c);
      else if (c >= 'A' && c <= 'Z' && c - 'A' < base - 10) 
    sb.append(Character.toLowerCase(c));
    }
    while (i < str.length()) {
      char c = str.charAt(i++);
      if (c >= '0' && c <= '9' && c - '0' < base) 
    sb.append(c);
      else if (c >= 'a' && c <= 'z' && c - 'a' < base - 10) 
    sb.append(c);
      else if (c >= 'A' && c <= 'Z' && c - 'A' < base - 10) 
    sb.append(Character.toLowerCase(c));
    }
    if (sb.length() > 0) {
      super.insertString(offs, sb.toString(), a);
      modified = true;
    }
  }

  /**
   * Returns true iff this NumericDocument has been modified since its
   * creation.
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * Removes the specified substring from this NumericDocument.  If the
   * content has not been modified since its creation, the removal is
   * suppressed.  This forces the first modification to be an insertion,
   * which in fact replaces any content entirely.
   * @throws BadLocationException  If the given remove position is not
   *                               a valid position within the document.
   */
  public void remove(int offs, int len) throws BadLocationException {
    if (modified) super.remove(offs, len);
  }
}
