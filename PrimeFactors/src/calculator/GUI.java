/*
 * EDU.ksu.cis.calculator.UserInterface.java    3/17/2001
 *
 * Copyright (c) 2001 Rod Howell, All Rights Reserved.
 */

package calculator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.JPanel;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * This is the driver class for the arbitrary-precision integer calculator.
 * It may be run either as an application or an applet.  When run as
 * an application, it accepts two optional command-line arguments.  The
 * first is the name of the package containing the classes implementing
 * the calculator model.  The name must be terminated by a period.  If
 * omitted, <tt>EDU.ksu.cis.calculator.defaultmodel.</tt> is used.  The
 * second command-line argument gives the name of a class implementing
 * the user interface to be used.  This class must implement the
 * interface {@link CalculatorUI}.  If omitted, the class 
 * {@link DefaultUI} is used.  If packages other than:
 * <ul>
 * <li> {@link EDU.ksu.cis.calculator.defaultmodel} or
 * <li> {@link EDU.ksu.cis.calculator.javamodel}
 * </ul>
 * are used for the calculator model, or if classes other than 
 * {@link DefaultUI} are used for the user interface, care must be taken
 * that the model package provides all of the classes required by the
 * user interface class.  See {@link DefaultUI} for more details.
 *
 * When run as an applet, these values can be passed using the following
 * <tt>param</tt> tags:
 * <ul>
 * <li> <tt>ModelLocation</tt>: Package containing the model
 * <li> <tt>UIName</tt>: Class name of the user interface
 * </ul>
 *
 * @author Rod Howell
 *         (<a href="mailto:howell@cis.ksu.edu">howell@cis.ksu.edu</a>)
 */
public class GUI {

  /**
   * The package containing the default model.
   */
  private static final String DEFAULT_MODEL 
    = "calculator.largemodel.";

  /**
   * The default user interface class.
   */
  private static final String DEFAULT_UI = "calculator.DefaultUI";

  /**
   * The package containing the calculator model, terminated by a period.
   */
  private String modelLoc;

  /**
   * The class used as the user interface.  This class must implement
   * {@link CalculatorUI}.
   */
  private String uiName;

  /**
   * The applet parameter information returned by {@link #getParameterInfo()}.
   */
  private static final String[][] paramInfo = {
        {"ModelLocation", "String", "Package containing the model."},
        {"UIName", "String", "Class name of the user interface."}
    };

  /**
   * Does initialization common to applets and applications.
   */
  private void setup(JFrame frame) {
    if (modelLoc == null) modelLoc = DEFAULT_MODEL;
    if (uiName == null) uiName = DEFAULT_UI;
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
      // This shouldn't happen
      Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, e);
    }
    try {
      Class uiClass = Class.forName(uiName);
      Constructor uiConstr = uiClass.getConstructor(new Class[] {
          Class.forName("java.lang.String"),
          Class.forName("javax.swing.JFrame")
      });
      uiConstr.newInstance(new Object[] {modelLoc, frame});
    } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
      // If we can't instantiate the user interface, nothing useful is
      // going to happen.
      Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, e);
    }
  }

  /**
   * @return information for applet parameters.
   * @see javax.swing.JApplet#getParameterInfo()
   */
  public String[][] getParameterInfo() {
    return paramInfo;
  }

  /**
   * Initializes the application.
   * @param args  An array of optional String arguments.  If present,
   *              the first argument gives the name of the package containing
   *              the calculator model.  This name must be terminated with
   *              a period.  If a second argument is present, it gives the
   *              name of the user interface class.
   */
  public static void main(String[] args) {
    JFrame f = new JFrame();
    GUI ui = new GUI();
    if (args.length > 0) {
      ui.modelLoc = args[0];
      if (args.length > 1) {
        ui.uiName = args[1];
      }
    }
    f.setContentPane(new JPanel());
    ui.setup(f);
    f.setTitle(ui.modelLoc);
    f.pack();
    f.setVisible(true);
  }
}
