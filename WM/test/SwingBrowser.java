package jx.start;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class SwingBrowser {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Eenvoudige Swing Browser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");

        try {
            String urlString = "https://info.cern.ch/hypertext/WWW/TheProject.html";
            URL url = new URL(urlString);
            
            // 1. Open handmatig de verbinding
            URLConnection connection = url.openConnection();
            
            // 2. VOEG DEZE REGEL TOE: Identificeer jezelf als een browser
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            // 3. Laad de inhoud in de editorPane
            editorPane.setPage(url); 
            // Let op: JEditorPane.setPage(URL) opent intern een nieuwe verbinding. 
            // In sommige Java-versies werkt dit nog steeds niet direct. 
            // Een betere methode voor complexe sites is de HTML streamen:
            editorPane.read(connection.getInputStream(), null);


            frame.add(new JScrollPane(editorPane));
            frame.setSize(800, 600);
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
