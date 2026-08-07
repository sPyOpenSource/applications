
package jx.start;

/**
 *
 * @author xuyi
 */
import javax.swing.*;
import java.awt.*;

public class SimpleOS extends JFrame {
    public SimpleOS() {
        setTitle("Java OS");
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Volledig scherm
        setUndecorated(true); // Verwijder standaard Windows/Mac randen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // De "Desktop"
        JDesktopPane desktop = new JDesktopPane();
        desktop.setBackground(new Color(45, 52, 54)); // Donkergrijze achtergrond

        // Een voorbeeldvenster toevoegen
        JInternalFrame frame = new JInternalFrame("Documenten", true, true, true, true);
        frame.setSize(300, 200);
        frame.setVisible(true);
        desktop.add(frame);

        add(desktop);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimpleOS().setVisible(true));
    }
}
