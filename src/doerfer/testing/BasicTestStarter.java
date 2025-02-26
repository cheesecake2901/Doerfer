package doerfer.testing;
import javax.swing.*;

import doerfer.preset.graphics.*;
import doerfer.preset.ArgumentParser;
import java.util.ArrayList;
import java.util.List;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Frame of game
 */
@SuppressWarnings("serial")
class AppFrame extends JFrame {
    public AppFrame(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}

/**
 * Resizes frame
 */
class frameResizeListener extends ComponentAdapter {

    GPanel panel;

    public frameResizeListener(GPanel gpBoard)
    {
        super();
        panel = gpBoard;
    }

    /**
     * Resizes Component of frame <br>
     * @param evt the event to be processed
     */

    public void componentResized(ComponentEvent evt) {
        this.panel.updateScale();
        this.panel.repaint();
    }

}

/**
 * Mainclass, which starts System
 */

public class BasicTestStarter
{


    /**
     * Mainfunction <br>
     * -get command line from Argument.txt file mit ant run, ant human oder ant random <br>
     * @param args command line or command from Argument.txt file
     */
    public static void main( String[] args )
    {
        //Listen der Autoren
        List<String> Autoren = new ArrayList<>();
        Autoren.add("Celine");
        Autoren.add("Thorsten");
        Autoren.add("Morten");
        //Setzt Attribute aus Kommandozeile in Settings
        ArgumentParser parsedSettings = new ArgumentParser(args, "cmtProductions","1.0", Autoren, null,true);
        //init Frame, Panel und Control
        JFrame frame = new AppFrame("Doerfer");
        GPanel panel = null;
        BasicControl control = new BasicControl(frame,parsedSettings);



        //Panel vom ersten Board anzeigen
        try{
            panel = control.getGameBoardGPanel();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
        
        frameResizeListener frl = new frameResizeListener(panel);
        frame.addComponentListener(frl);
        //Panel wird in Frame eingefügt
        frame.add(panel);

        //Frame Optionen angepasst
        panel.repaint();
        frame.pack();
        frame.setSize(1280,720);
        frame.setVisible(true);
        frame.setResizable(true);
        panel.updateScale();

        //Spiel wird gestartet
        control.initNewGame();
    }
}
