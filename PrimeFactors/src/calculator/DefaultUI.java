/*
 * EDU.ksu.cis.calculator.DefaultUI.java    8/19/02
 *
 * Copyright (c) 2001, 2002 Rod Howell, All Rights Reserved.
 */

package calculator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JFrame;

import javax.swing.text.JTextComponent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.BoxLayout;
import javax.swing.text.Keymap;
import javax.swing.KeyStroke;

/**
 * The default user interface.  This UI is appropriate for an RPN
 * calculator supporting integer operations.  The first argument to the single
 * constructor gives the name of the package containing the classes that
 * implement the model of the calcultator.  One of these classes must
 * be named <tt>CalculatorImpl</tt> and implement the interface
 * {@link Calculator}.  Furthermore, the <tt>CalculatorImpl</tt> class
 * must contain a 2-argument constructor whose first argument is of type
 * {@link CalculatorUI} and whose second argument is of type <tt>int</tt>.
 * Other classes in that package which this
 * class will attempt to instantiate include:
 * <ul>
 * <li> ClearStack
 * <li> RotateUp
 * <li> RotateDown
 * <li> Remainder
 * <li> Power
 * <li> TwoToPower
 * <li> TenToPower
 * <li> Divide
 * <li> Multiply
 * <li> Subtract
 * <li> Add
 * <li> ChangeSigns
 * <li> Pop
 * <li> Push
 * </ul>
 * In order for these classes to be used, they must have a visible
 * default constructor and implement the interface {@link Operation}.
 * If any of these classes are missing or cannot be constructed, their
 * corresponding buttons will be diabled.
 * <p>
 * This UI supports any radix 2-36 and allows conversion between them.
 * Buttons are provided for the digits 0-9, but any digit valid for the
 * current radix may be entered directly from the keyboard (see
 * {@link java.lang.Integer#toString(int, int)}).  Keyboard equivalents are
 * not provided for the operations.
 *
 * Menus are provided for editing (cut, copy, paste, select all) and
 * changing whether the bases are "sticky"; i.e., if the current radix
 * has changed since a value was pushed onto the stack and the bases are
 * sticky, then if that value is moved to the top of the stack, it is
 * displayed in the current base.  Otherwise, it is displayed in the
 * base in which it was originally entered or computed.  The model
 * is responsible for implementing this behavior by accepting a
 * {@link java.lang.Boolean} as the first argument to 
 * {@link Calculator#changeSettings(Object, String)}.
 *
 * The Home and End keys move the text caret to the beginning and end,
 * respectively, of the display.
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class DefaultUI implements CalculatorUI {

  /**
   * This array gives the bindings for each button.
   * The elements of the main array each describe one button, listed by
   * row.  Each of these elements is an array of Strings.  The first
   * element is required, and gives the text to be shown on the button.
   * The second element, if present, gives the name of the class performing
   * the button's operation.  The full name will be obtained by prepending
   * this name with the value of the the parameter to the DefaultUI
   * constructor.  This class must be a subclass of Operation.  If the class 
   * is found, an instance will be constructed using the default constructor.
   * This instance will be passed to the calculator model by the Action
   * associated with the button.  If the class is not found, or if the
   * constructor fails, the button will be disabled.  If the array element
   * is not present, the button will be treated as a text-entry button.
   */
  private static final String[][] BINDINGS = new String[][]
  {
    // Row 1
    {"Clear", "ClearStack"},
    {   "Up", "RotateUp"},
    { "Down", "RotateDown"},
    {  "Rem", "Remainder"},
    // Row 2
    { "x^y", "Power"},
    { "2^x", "TwoToPower"},
    {"10^x", "TenToPower"},
    {   "/", "Divide"},
    // Row 3
    {"7"},
    {"8"},
    {"9"},
    {"*", "Multiply"},
    // Row 4
    {"4"},
    {"5"},
    {"6"},
    {"-", "Subtract"},
    // Row 5
    {"1"},
    {"2"},
    {"3"},
    {"+", "Add"},
    // Row 6
    {"0"},
    {   "+/-", "ChangeSigns"},
    {"Delete", "Pop"},
    { "Enter", "Push"}
  };

  /**
   * The name of the class implementing the model of the calculator.
   * This class must be in the package specified in the first argument
   * to the constructor.
   */
  private static final String CALCULATOR_CLASS = "CalculatorImpl";

  /**
   * The initial radix.
   */
  private static final int DEFAULT_BASE = 10;

  /**
   * The default cursor.
   */
  private static final Cursor DEFAULT_CURSOR = 
    new Cursor(Cursor.DEFAULT_CURSOR);

  /**
   * The busy cursor.
   */
  private static final Cursor BUSY_CURSOR = new Cursor(Cursor.WAIT_CURSOR);

  /**
   * The calculator's display.
   */
  private final JTextArea display = new JTextArea();

  /**
   * The model of the display.
   */
  private final NumericDocument theDocument = new NumericDocument(DEFAULT_BASE, "");

  /**
   * The component for selecting the radix.
   */
  private JComboBox radixBox;

  /**
   * Constructs a new user interface.
   * @param modelLoc  The name of the package containing the calculator
   *                  model.  The name must be terminated by a period.
   * @param parent    The applet on which the user interface is diplayed.
   *                  If the calculator is running as an application,
   *                  the applet behaves like a Panel.
   * @throws ClassNotFoundException  If there is no class CalculatorImpl
   *                                 in the package specified by modelLoc.
   * @throws NoSuchMethodException   If the CalculatorImpl class does not
   *                                 have a 2-arg constructor whose first
   *                                 argument is of type {@link CalculatorUI}
   *                                 and whose second argument is of type int.
   * @throws IllegalAccessException  If the above constructor is inaccessible.
   * @throws InvocationTargetException If the above constructor throws an
   *                                   exception.
   * @throws InstantiationException  If CalculatorImpl is an abstract class.
   * @throws ClassCastException      If CalculatorImpl does not implement
   *                                 {@link Calculator}.
   */
  public DefaultUI(String modelLoc, JFrame parent) 
    throws ClassNotFoundException, NoSuchMethodException, 
       InstantiationException, IllegalAccessException,
       InvocationTargetException, ClassCastException {
    Calculator calc = getModel(modelLoc);
    Container content = parent.getContentPane();
    content.setLayout(new FlowLayout());
    JPanel c = new JPanel();
    c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
    parent.setJMenuBar(getMenuBar(calc));
    c.add(getScrollingDisplay());
    c.add(getBasePanel(calc));
    c.add(getButtonPanel(modelLoc, calc));
    content.add(c);
  }

  /**
   * Returns a new instance of the calculator model defined in the class
   * CalculatorImpl of the package specified by modelLoc.
   * @throws ClassNotFoundException  If there is no class CalculatorImpl
   *                                 in the package specified by modelLoc.
   * @throws NoSuchMethodException   If the CalculatorImpl class does not
   *                                 have a 2-arg constructor whose first
   *                                 argument is of type {@link CalculatorUI}
   *                                 and whose second argument is of type int.
   * @throws IllegalAccessException  If the above constructor is inaccessible.
   * @throws InvocationTargetException If the above constructor throws an
   *                                   exception.
   * @throws InstantiationException  If CalculatorImpl is an abstract class.
   * @throws ClassCastException      If CalculatorImpl does not implement
   *                                 {@link Calculator}.
   */
  private Calculator getModel(String modelLoc) 
    throws ClassNotFoundException, NoSuchMethodException, 
       InstantiationException, IllegalAccessException,
       InvocationTargetException, ClassCastException {
    Class calcClass = Class.forName(modelLoc + CALCULATOR_CLASS);
    Constructor calcConstr = calcClass.getConstructor(new Class[] {
        Class.forName("calculator.CalculatorUI"),
        Integer.TYPE
      });
    return (Calculator) calcConstr.newInstance(new Object[] {this, DEFAULT_BASE});
  }

  /** 
   * Returns the menu bar for this user interface.
   * @param calc  The calculator model.
   */
  private JMenuBar getMenuBar(Calculator calc) {
    JMenuBar menuBar = new JMenuBar();
    JMenu editMenu = new JMenu("Edit");
    editMenu.add(new SelectAllAction(display));
    Action cut = new DefaultEditorKit.CutAction();
    cut.putValue(Action.NAME, "Cut");
    editMenu.add(cut);
    Action copy = new DefaultEditorKit.CopyAction();
    copy.putValue(Action.NAME, "Copy");
    editMenu.add(copy);
    Action paste = new DefaultEditorKit.PasteAction();
    paste.putValue(Action.NAME, "Paste");
    editMenu.add(paste);
    menuBar.add(editMenu);
    JMenu optionsMenu = new JMenu("Options");
    JCheckBoxMenuItem stickyBase = new JCheckBoxMenuItem("Sticky Bases", true);
    ActionListener changeSticky = new SetSticky(stickyBase, calc, this);
    stickyBase.addActionListener(changeSticky);
    optionsMenu.add(stickyBase);
    menuBar.add(optionsMenu);
    return menuBar;
  }

  /**
   * Returns the scrolling display.
   */
  private JScrollPane getScrollingDisplay() {
    display.setFont(new Font("Monospaced", Font.PLAIN, 14));
    display.setDocument(theDocument);
    Keymap map = display.getKeymap();
    KeyStroke homeKey = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0);
    map.removeKeyStrokeBinding(homeKey);
    map.addActionForKeyStroke(homeKey, new HomeAction(display));
    KeyStroke endKey = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0);
    map.removeKeyStrokeBinding(endKey);
    map.addActionForKeyStroke(endKey, new EndAction(display));
    return new JScrollPane(display, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
               JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
  }

  /**
   * Returns the panel containing the combo box for selecting the radix.
   * @param calc  The calculator model.
   */
  private JPanel getBasePanel(Calculator calc) {
    JPanel basePanel = new JPanel();
    basePanel.add(new JLabel("Base:"));
    Integer[] bases = new Integer[35];
    for (int i = 0; i < 35; i++) {
      bases[i] = i + 2;
    }
    radixBox = new JComboBox(bases);
    radixBox.setSelectedIndex(DEFAULT_BASE - 2);
    basePanel.add(radixBox);
    radixBox.addItemListener(new BaseListener(radixBox, calc, this));
    return basePanel;
  }

  /**
   * Returns the panel containing the buttons.
   * @param modelLoc  The location of the package containing the calculator
   *                  model.  Must be terminated by a period.
   * @param calc      The calculator model.
   */
  private JPanel getButtonPanel(String modelLoc, Calculator calc) {
    JPanel btnPanel = new JPanel(new GridLayout(6, 4));
      for (String[] BINDINGS1 : BINDINGS) {
          JButton btn = new JButton(BINDINGS1[0]);
          if (BINDINGS1.length > 1) {
              try {
                  Class opClass = Class.forName(modelLoc + BINDINGS1[1]);
                  Operation op = (Operation) opClass.newInstance();
                  btn.addActionListener(new ButtonAction(this, calc, op));
              }catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                  btn.setEnabled(false);
              } } else {
              btn.addActionListener(new TextButtonAction(this, BINDINGS1[0]));
          }
          btnPanel.add(btn);
      }
    return btnPanel;
  }

  /**
   * Inserts the given string into the display at the text caret.  If
   * there is a selected region, it is replaced.
   */
  @Override
  public void insert(String s) {
    display.replaceSelection(s);
    display.requestFocus();
  }

  /**
   * @return any input the user has provided since the model last updated
   * the display.  Returns <tt>null</tt> if there is no such input.
   */
  @Override
  public String getInput() {
    return ((NumericDocument) display.getDocument()).isModified() ? 
      display.getText() : null;
  }

  /**
   * Displays the given String, and the given radix.
   */
  @Override
  public void setOutput(String s, int b) {
    display.setDocument(new NumericDocument(b, s));
    if (radixBox.getSelectedIndex() != b - 2)
      radixBox.setSelectedIndex(b - 2);
    display.requestFocus();
  }

  /**
   * Displays the given Throwable in a message dialog.
   */
  @Override
  public void showError(Throwable e) {
    Object message = e;
    if (e instanceof EmptyDequeException)
      message = "Stack Underflow";
    else if (e instanceof ArithmeticException)
      message = "Invalid Operand";
    else if (e instanceof OutOfMemoryError)
      message = "Insufficient Memory for Operation";
    JOptionPane.showMessageDialog(null, message, "Error", 
                  JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Sets the cursor to "Wait" if <tt>busy = true</tt> or "Default" 
   * otherwise.
   */
  @Override
  public void setBusy(boolean busy) {
    //if (busy) parent.setCursor(BUSY_CURSOR);
    //else parent.setCursor(DEFAULT_CURSOR);
  }
}

/**
 * Interrupt handler for buttons performing operations.
 */
class ButtonAction extends AbstractAction {

  /**
   * The calculator model.
   */
  private final Calculator theCalculator;

  /**
   * The user interface.
   */
  private final CalculatorUI theView;

  /**
   * The operation to be performed when this button is pressed.
   */
  private final Operation theOperation;

  /**
   * Constructs a new ButtonAction.
   * @param  v   The user interface.
   * @param  c   The calculator model.
   * @param  op  The operation to be performed when this button is pressed.
   */
  public ButtonAction(CalculatorUI v, Calculator c, Operation op) {
    theCalculator = c;
    theView = v;
    theOperation = op;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent ev) {
    theView.setBusy(true);
    try {
      String input = theView.getInput();
      theCalculator.doOperation(theOperation, input);
      theView.setBusy(false);
    } catch (Exception e) {
      theView.setBusy(false);
      theView.showError(e);
    }
  }
}

/**
 * Event handler for a button whose function is text entry.
 */
class TextButtonAction extends AbstractAction {

  /**
   * The user interface.
   */
  private final CalculatorUI theView;

  /**
   * The text associated with this button.
   */
  private final String text;

  /**
   * Constructs a new TextButtonAction.
   * @param  v  The user interface.
   * @param  s  The text associated with this button.
   */
  public TextButtonAction(CalculatorUI v, String s) {
    theView = v;
    text = s;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    theView.insert(text);
  }
}

/**
 * The event handler for the "Select All" event.
 */
class SelectAllAction extends AbstractAction {

  /**
   * The component displaying the text.
   */
  private final JTextComponent display;

  /**
   * Constructs a new SelectAllAction.
   * @param  t  The component displaying the text.
   */
  public SelectAllAction(JTextComponent t) {
    super("Select all");
    display = t;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    display.selectAll();
  }
}

/**
 * Event handler for the Home key.
 */
class HomeAction extends AbstractAction {

  /**
   * The component displaying the text.
   */
  private final JTextComponent display;

  /**
   * Constructs a new HomeAction.
   * @param  t  The component displaying the text.
   */
  public HomeAction(JTextComponent t) {
    display = t;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    display.setCaretPosition(0);
  }
}

/**
 * Event handler for the End key.
 */
class EndAction extends AbstractAction {

  /**
   * The component displaying the text.
   */
  private final JTextComponent display;

  /**
   * Constructs a new EndAction.
   * @param  t  The component displaying the text.
   */
  public EndAction(JTextComponent t) {
    display = t;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    display.setCaretPosition(display.getDocument().getLength());
  }
}

/**
 * Event handler for radix change.
 */
class BaseListener implements ItemListener {

  /**
   * The component to which this event handler is listening.
   */
  private final JComboBox theBox;

  /**
   * The calculator model.
   */
  private final Calculator theCalculator;

  /**
   * The user interface.
   */
  private final CalculatorUI theView;

  /**
   * Constructs a new BaseListener.
   * @param  b  The component to which this event handler is listening.
   * @param  c  The calculator model.
   * @param  v  The user interface.
   */
  public BaseListener(JComboBox b, Calculator c, CalculatorUI v) {
    theBox = b;
    theCalculator = c;
    theView = v;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void itemStateChanged(ItemEvent e) {
    try {
      String input = theView.getInput();
      theView.setBusy(true);
      theCalculator.doOperation(new ConvertBase(theBox.getSelectedIndex() + 2),
                input);
      theView.setBusy(false);
    } catch (Exception ex) {
      theView.setBusy(false);
      theView.showError(ex);
    }
  }
}

/**
 * Event handler for the event which changes whether bases are sticky.
 */
class SetSticky implements ActionListener {

  /**
   * The component to which this event handler is listening.
   */
  private final JCheckBoxMenuItem item;

  /**
   * The calculator model.
   */
  private final Calculator theCalculator;

  /**
   * The user interface.
   */
  private final CalculatorUI theView;

  /**
   * Constructs a new SetSticky.
   * @param  b  The component to which this event handler is listening.
   * @param  c  The calculator model.
   * @param  v  The user interface.
   */
  public SetSticky(JCheckBoxMenuItem b, Calculator c, CalculatorUI v) {
    item = b;
    theCalculator = c;
    theView = v;
  }

  /**
   * Handles the event.  The parameter is ignored.
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      theCalculator.changeSettings(item.getState(), 
                   theView.getInput());
    } catch (Exception t) {
      theView.showError(t);
    }
  }
}
