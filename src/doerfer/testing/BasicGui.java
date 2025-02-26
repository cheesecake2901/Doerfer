package doerfer.testing;

import doerfer.preset.*;
import doerfer.preset.graphics.GElement;
import doerfer.preset.graphics.GPanel;
import doerfer.preset.graphics.GText;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.FileReader;
import java.util.*;
import java.awt.event.*;
import java.util.List;


/**
 * Used switch the opacity of the HUD if the mouse enters or leaves the background of the hud.
 */
class HudMouseOverListener extends MouseAdapter {

    /**
     * The BasicGui with the GPanel and its  HUDLAYER.
     */
    private BasicGui target;

    /**
     * Constructor is called with reference of the BasicGui
     * @param bg the BasicGui to which LAYER_HUD the listener should belong.
     */
    HudMouseOverListener(BasicGui bg)
    {
        target = bg;
    }

    /**
     * If the mouse enters the HUDs Background svg its opacity is set false
     * @param e the event to be processed
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        this.target.setHudOpac(false);
    }

    /**
     * If the mouse leaves the HUDs Background svg its opacity is set true
     * @param e the event to be processed
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        this.target.setHudOpac(true);
    }

}

/**
 * Used to listen if a possible position is clicked.<br>
 * It should be added to a SVG Tile indicating a possible position.
 */
class OpenPositionListener extends MouseAdapter {
    /**
     * The BasicGui of the game. The LayerMain with the painted Game Board sits here.
     */
    private BasicGui target;

    /**
     * reference to the central Control instance of the Game
     */
    private BasicControl control;

    /**
     * Index in BasicBoards.treeMap logic, the position to which the OpenPositionListener belongs
     */
    private int index;


    /**
     * Constructor creates a new OpenPositionListener<br>
     * @param g the BasicGUI to which the SVG Tile is listens belong
     * @param c the BasicControl of the Game
     * @param i the index of the tile listens to (BasicBoards.treeMap logic)
     */
    OpenPositionListener(BasicGui g, BasicControl c, int i)
    {
        this.target = g;
        this.control = c;
        this.index = i;
    }
    /**
     * Used to handle a mouse click on a SVG Tile marking a possible position for a turn.
     * @param e the event to be processed
     */
    public void mouseClicked(MouseEvent e)
    {

        // checks if the current Player is actually human
        // otherwise no input may be committed
        if (this.control.isActivePlayerHuman() && (e.getClickCount() > 0)) {

            if (!this.control.getActiveGameBoard().trashNoMoreCards())
            {
                this.target.setInfoConfirmRotate(); // updates the tool tip
                try {
                    if (this.target.getLastSelection() == this.index) // checks if the clicked position is the one clicked before
                    {
                        this.control.getActiveGameBoard().finalizeRotation(); // move is confirmed try to finalize the turn.
                    }
                    else { // the position was not target of the previous click
                            if (this.control.getActiveGameBoard().isActiveInsertion()) // the player already placed a tile
                            {
                                this.target.cleanLastPaintedTile(); // the previously drawn Tile has to be removed
                            }
                            this.control.getActiveGameBoard().insertNext(this.index); // the game board is informed about a new placement candidate
                            this.target.setLastSelection(this.index);  // the position is marked and will finalize if the next click also goes here
                    }
                }
                catch (Exception ex)
                {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}

/**
 * Used by the GUI to listen to Keyboard inputs.<br>
 * Arrow Inputs are used to Scroll over the mainlayer<br>
 * +/-  are used to zoom in and out<br>
 * 0 is used to reset the focus<br>
 */
class MyKeyListener implements KeyListener{
    private BasicGui gui;
    private BasicControl control;
    MyKeyListener(BasicGui gui1, BasicControl contrl){
        gui = gui1;
        control = contrl;
    }
    @Override
    /**
     * Listens to Keyboard inputs and reacts to +,- and 0 inputs.<br>
     *
     */
    public void keyTyped(KeyEvent k) {
        if(k.getKeyChar() == '+')
        {
            this.gui.zoom(0.1f);
        }
        else if (k.getKeyChar() == '-')
        {
            this.gui.zoom((-0.1f));
        }
        else if (k.getKeyChar() == '0')
        {
            this.gui.setZoom(1); // resets to the predefined zoom
            this.gui.focus(this.gui.getLastPlacementOfView());
        }
    }

    @Override
    /**
     * Listens to Keyboard inputs and reacts to pressed arrow Keys
     *
     */
    public void keyPressed(KeyEvent keyEvent) {
        int k = keyEvent.getKeyCode();
        if (k == KeyEvent.VK_UP)
        {
            this.gui.moveBoard(0,1);
        }
        else if (k == KeyEvent.VK_DOWN)
        {
            this.gui.moveBoard(0,(-1));
        }
        else if (k == KeyEvent.VK_RIGHT)
        {
            this.gui.moveBoard((-1),0);
        }
        else if (k == KeyEvent.VK_LEFT)
        {
            this.gui.moveBoard(1,0);
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent)
    {
        // nothing to do here
    }


}
/**
 * Listen to a mousewheel rotation.<br>
 * Used during a human turn to rotate the tile which is about to be placed.
 */
class MouseWheel implements MouseWheelListener {

    /** Reference of the central Control Instance
     */
    private BasicControl control;

    /**
     * Constructor called with a reference to the central control instance
     * @param contr Reference of the central Control Instance
     */
    MouseWheel(BasicControl contr){
        control = contr;
    }

    /**
     * Handles the rotation of the mousewheel.<br>
     * Rotates the Active Hex accordingly<br>
     * @param e the event to be processed
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // checks if the current Player is actually human
        // otherwise no input may be committed
        if (this.control.isActivePlayerHuman() && this.control.getActiveGameBoard().isActiveInsertion()) {
            int i = e.getWheelRotation() * (-1); // -1 for a more intuitive experience
            try {
                this.control.getActiveGameBoard().rotateActiveHex(Integer.signum(i)); // Calls to rotate the active Hex, slowed by signum.
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}


/**
 * 
 *<br>
 * Handles the graphical IO Tasks of the Game:<br>
 * - paints the game board<br>
 * - paints placed tiles<br>
 * - paints uncovered tiles to the HUD<br>
 * - interprets inputs mouse clicks and wheel rotations<br>
 * - interprets keyboard inputs for navigating / zooming on the game board.<br>
 *
 * Shows HUD Information on a Hud Background on a gpanels layer hud.<br>
 * Shows The Game Board on a gpanels layer main.<br>
 *
 */
class BasicGui implements GameView
{

    /** The left Edge of the HUD Background.
     */
    final static int HUDLEFT = 1200;

    /** sets the opacity of the Hud Background
     */
    final static float HUDOPACITY = 0.8f;

    /** sets the distance of hud information from the left hud background border**/
    final static int HUDBORDERSPACE = 10;

    /** sets the height of where the active player is shown on the hud
     */
    final static int HUDACTIVEPLAYERHEIGHT = 100;

    /** sets the height of where a skipped player is indicated on the hud**/
    final static int HUDPLAYERSKIPPEDHEIGHT = 480;

    /** sets the maximal number of characters of a players name displayed on the hud
     */
    final static int HUDMAXNAMELENGTH = 12;

    /** sets the height of tooltips first line **/
    final static int HUDTOOLTIPHEADLINEHEIGHT = 745;

    /** sets the space between tooltip lines **/
    final static int HUDTOOLTIPLINESPACING = 25;

    /** sets the position of the displayed player name/score complex **/
    final static int HUDPLAYERNAMESHEIGHT = 350;

    /** sets the position of the skipped player in the hud **/
    final static int HUDREMAININGTURNSHEIGHT = 690;

    /** sets the height of the discoverd tiles displayed in the hud **/
    final static int HUDDISCOVEREDTILESHEIGHT = 550;

    /** the height of a SVG Tile **/
    final static float  HEXHEIGHT = 86.6025403784f;

    /** the width of a SVG Tile **/
    final static float HEXWIDTH = 100.0f;

    /** sets how many layers of grey outline hexes are painted around placed tiles
     */
    final static int OUTLINEDEPTH = 2;


    /**
     * The GPanel which is used to which all graphics are painted
     */
    private GPanel panel;

    /**
     * stores the elements from which the last tile was composed<br>
     * (used to delete them if a human player decides to place on another position)
     */
    private Vector<GElement> lastTileElements;

    /**
     * stores the references to GElements which are painted to the board
     * used to remove them after a turn is made.
     */
    private HashMap<Integer,GElement> lastMarkerElements;

    /**
     * a set of Integers containing the indexes of occupied positions
     * used to draw the grey outlines around positions.
     */
    private Set<Integer> occupiedPositions;

    /**
     * Stores references to the GElements composing the last drawn uncovered tiles
     */
    private Vector<GElement> lastHandCardElements;


    /**
     * Listener used rotate the tile during a human turn
     */
    private MouseWheel mouseWheel;

    /** Listener used to detect mouse in/out on the hud background and change opaacity **/
    private HudMouseOverListener hudListener;

    /**
     * Listener used for zooming and scrolling the game board
     */
    private MyKeyListener keyBoard;

    /**
     * The SVG Element used as background for the HUD information.
     */
    private GElement hudbackground;

    /**
     * Stores the last position where the focus would reset to.
     */
    private TilePlacement lastPlacementOfView;
    /**
     * width of the gameboard in BasicBoard.treeMap logic
     */
    private int width;

    /**
     * height of the gameboard in BasicBoard.treeMap logic
     */
    private int height;
    /** Stores the offset for columns calculated by control**/
    private int offSetX;
    /** Stores the offset for rows calculated by control**/
    private int offSetY;

    /**
     * reference of the central control instance
     */
    private BasicControl control;

    /**
     * stores the current zoom level
     */
    private float scale;

    /** stores the index of the last clicked open position (during a humans turn)**/
    private int lastSelection;

    /** stores the current X translation of the main layer, used to do incremental scrolls **/
    private float viewTranslationX;

    /** stores the current Y translation of the main layer, used to do incremental scrolls **/
    private float viewTranslationY;

    /** this GText is used to indicate the active player on the HUD**/
    private GText activePlayerGT = null;

    /** used to store the reference of the GTexts Elements displaying the Player's names **/
    private GText[] playerNames;

    /** used to store the reference of the GTexts Elements displaying the Player's scores **/
    private GText[] scoresGT;

    /** this GText shows the remaining turns on the HUD **/
    private GText remainingTurnsGT;

    /** used to display tool tips at the bottom of the HUD Background area **/
    private GText headlineInformationGT;

    /** used to display tool tips at the bottom of the HUD Background area **/
    private GText firstLineInformationGT;

    /** used to display tool tips at the bottom of the HUD Background area **/
    private GText secondLineInformationGT;

    /** used to indicate that the Player before was skipped */
    private GText skippedPlayer = null;

    /** keeps track if the game is still ongoing **/
    boolean gameActive;

    /**
     * Contructor for a BasicGui object<br>
     * Is usually called by Control<br>
     *<br>
     * @param w width in BasicBoard hashTree logic
     * @param h height in BasicBoard hashTree logic
     * @param osx offset for columns in BasicBoard hashTree logic
     * @param osy offset for columns in BasicBoard hashTree logic
     * @param con reference of the games central control object.
     */
    BasicGui(int w, int h, int osx, int osy, BasicControl con) {

        // contructor parameters
        width= w;
        height = h;
        offSetX = osx;
        offSetY = osy;
        control = con;

        lastTileElements = new Vector<GElement>();
        lastHandCardElements = new Vector<GElement>();
        scale = 1.0f;
        viewTranslationX = 0;
        viewTranslationY = 0;

        occupiedPositions = new TreeSet<>();
        scoresGT = new GText[this.control.getPlayerCount()];
        keyBoard = new MyKeyListener(this,this.control);
        hudListener = new HudMouseOverListener(this);
        gameActive = true;

        lastMarkerElements = new HashMap<Integer,GElement>();

        // creates the panel
        try {
            panel = new GPanel();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }

        // listener
        mouseWheel = new MouseWheel(this.control);
        panel.addMouseWheelListener(mouseWheel);
        this.control.parentFrame.addKeyListener(keyBoard);

        // adds the Background for the HUD
        try {
            this.hudbackground = this.panel.loadSVG(new FileReader("graphics/components/hudbackground.svg"));
            this.hudbackground.transform().translate(480,0);
            this.hudbackground.setFillOpacity(HUDOPACITY);
            this.hudbackground.setMouseListener(this.hudListener);
            this.panel.getLayerHUD().addChild(this.hudbackground);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        // creates the tooltips area
        headlineInformationGT = new GText(HUDLEFT + HUDBORDERSPACE, HUDTOOLTIPHEADLINEHEIGHT, "");
        headlineInformationGT.setFontSize(16);
        headlineInformationGT.setFill(Color.red);
        headlineInformationGT.setBold(true);
        firstLineInformationGT    = new GText(HUDLEFT + HUDBORDERSPACE, HUDTOOLTIPHEADLINEHEIGHT + HUDTOOLTIPLINESPACING, "Hello there!" );
        firstLineInformationGT.setFontSize(16);
        secondLineInformationGT   = new GText(HUDLEFT + HUDBORDERSPACE, HUDTOOLTIPHEADLINEHEIGHT + 2 * HUDTOOLTIPLINESPACING, "To make a move click on a highlighted position");
        secondLineInformationGT.setFontSize(16);
        this.panel.getLayerHUD().addChild(headlineInformationGT);
        this.panel.getLayerHUD().addChild(firstLineInformationGT);
        this.panel.getLayerHUD().addChild(secondLineInformationGT);

        // creates the GText which shows a skipped player
        remainingTurnsGT = new GText(HUDLEFT + HUDBORDERSPACE, HUDREMAININGTURNSHEIGHT,"Remaining Turns: ");
        remainingTurnsGT.setFontSize(24);
        this.panel.getLayerHUD().addChild(this.remainingTurnsGT);

    }

    /**
     * Informs the GUI about a game over.
     *<br>
     * @param gameover whether the game is over (mostly relevant when implementing a tournament modus)
     */
    @Override
    public void setGameOver(boolean gameover){
        this.gameActive = gameover;
    }

    /**
     * Displays an error message in a nice and human readable form.
     *
     * @param e the error
     */
    public void displayError(Exception e){
        String error = e.toString();
        JOptionPane.showMessageDialog(this.control.parentFrame, "A problem occured: " + e);
    }


    /**
     * Sets the focus on a specific location
     * @param location the TilePlacement to which the focus should be set.
     */
    @Override
    public void focus(TilePlacement location){
        
        if(location != null)
        {
        
        this.lastPlacementOfView = location; // to let typing 0 on the keyboard focus to this position
        this.viewTranslationY = 0;
        this.viewTranslationX = 0;

        int x = (-1) * (location.getColumn());
        int y = (-1) * (location.getRow());

        // the column which is the center with scale 1.
        int centerColumn = 7;

        // the row which is the center with scale 1.
        int centerRow = 4;

        int zoomOffsetX = (int) (centerColumn / this.scale);
        int zoomOffsetY = (int) (centerRow / this.scale);
        
        
        this.moveBoard(x + zoomOffsetX,y + zoomOffsetY);
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
        
        }
        
    }

    /** Sets if the background of the HUD has opacity or not.<br>
     * Expected to be called by the mouse over listener
     * @param setOpac true enables opacity, false disables.
     */
    public void setHudOpac(boolean setOpac)
    {
        if(setOpac)
        {
            this.hudbackground.setFillOpacity(HUDOPACITY);
        }
        else
        {
            this.hudbackground.setFillOpacity(1.0f);
        }
        this.panel.getLayerHUD().update();
        this.panel.repaint();
    }

    /**
     * used by to set the focus back to the last one
     * when 0 is pressed.<br>
     * @return the current TilePlacement defining the focus
     */
    public TilePlacement getLastPlacementOfView(){
        return this.lastPlacementOfView;
    }

    /**
     * Updates the list of currently uncovered tiles from the tile stack.<br>
     *<br>
     * The list of tiles is sorted by age.<br>
     * The first element in the list is always the oldest and thus the one that is currently being placed.<br>
     *<br>
     * Handled elsewhere
     * @see BasicControl  updateOpenCardsOnGui()
     * @see BasicGui markHexforPossiblePosition(), drawOpenTile()
     *
     * @param tiles the list of currently uncovered tiles
     */
    public void setUncoveredTiles(List<Tile> tiles){
        throw new UnsupportedOperationException("not implemented. see DOC for alternative.");
    }

    /**
     * Tells the GUI how many tiles are left to be placed.
     *
     * @param n the number of tiles left to be placed
     */
    public void setTilesLeft(int n)
    {
        String s = " " + n;
        this.remainingTurnsGT.setText("Remaining Turns:" + s);
        this.panel.getLayerHUD().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
    }

    /**
     * Informs the GUI about all possible valid moves.<br>
     *<br>
     * This function is mostly relevant for HUMAN Players.<br>
     *
     * @param validTilePlacements the list of valid TilePlacements
     *
     *
     */
    @Override
    public void setValidTilePlacements(Set<TilePlacement> validTilePlacements){
        this.removeLastMarkers();
        Set<TilePlacement> currentPossibilities = new HashSet<TilePlacement>();
        this.setInfoStartMove();
        for (TilePlacement validTilePlacement : validTilePlacements)
        {
            int index = this.gxyToIndex(validTilePlacement.getColumn(),validTilePlacement.getRow()) ;
            currentPossibilities.add(validTilePlacement);
            this.markHexforPossiblePosition(index);
        }
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
    }

    /**
     * Request from the active player to place the current tile on a valid position and rotation.<br>
     *<br>
     * This function is mostly relevant for HUMAN Players.<br>
     *
     * @return the selected TilePlacement
     *
     * @see doerfer.preset setValidTilePlacements
     */
    @Override
    public TilePlacement requestTilePlacement() {

        return this.control.getLastPlacedMove();
    }

    /**
     * Calls repaint on the GPanel
     */
    public void repaint() {
            this.panel.repaint();
        }

    /**
     * Used to delete the previous round's markers for possible turns.
     */
    public void removeLastMarkers(){

        // lastMarkers holds all SVG Elements of the last turn
        for (Map.Entry<Integer,GElement> m :this.lastMarkerElements.entrySet()) {
            try{
                this.panel.getLayerMain().removeChild(m.getValue());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
        this.lastMarkerElements.clear();
    }

    /**
     * Used by an openPosition Listener to ask for the last clicked possible location
     *
     * @return index of the lastSelection
     */
    public int getLastSelection()
    {
        return this.lastSelection;
    }

    /**
     * Used by an openPosition Listener to set it's index as the last clicked possible location
     *
     * @param s of the lastSelection
     */
    public void setLastSelection(int s)
    {
        this.lastSelection = s;
    }

    /**
     * Used to write information to the tool tip text fields.<br>
     * @param first Appears in the upper tooltip line of the HUD
     * @param second Appears in the lower tooltip line of the HUD
     */
    private void setInformations(String first, String second)
    {
        this.firstLineInformationGT.setText(first);
        this.secondLineInformationGT.setText(second);
    }

    /**
     * Sets the HUDS's information to those displayed at the beginning
     * of a turn.<br>
     * included an update of the Scores, and tooltip.
     */
    public void setInfoStartMove()
    {
        this.headlineInformationGT.setText("");
        this.setInformations("Hello there!","Left click on a highlighted position to make a move");
        int[] scores = this.control.getScoreList();
        for(int i = 0; i < this.control.getPlayerCount();i++)
        {
            String t = "" + scores[i];
            this.scoresGT[i].setText(t);
        }
    }

    /**
     * Sets the HUD's tooltip to show information of a misplaced rotation.
     */
    public void setInfoWrongRotation()
    {
        this.headlineInformationGT.setText("Wrong rotation:");
        this.setInformations("Rotate tile by mousewheel to a suitable rotation","or choose another highlighted position");
        this.panel.getLayerHUD().update();
        this.panel.repaint();
    }

    /**
     * Sets the HUD's tooltip to show information how to proceed after a first click on possible position during a human turn.
     */
    public void setInfoConfirmRotate()
    {
        this.headlineInformationGT.setText("");
        this.setInformations("Confirm a move by left click, rotate by mousewheel","or choose another highlighted position");
    }

    /**
     * Called at the end of a game. Update the HUD to show information related to the end of the game<br>
     * - Winner(s) are displayed<br>
     * - Reason for game end is shown<br>
     * - Indication of winners at the scores<br>
     *<br>
     * Sets the HUD displayed<br>
     * @param winners a list of the winning Player IDs
     * @param cause a String describing the cause of the game over.
     */
    public void setInfoGameEnd(List<Integer> winners, String cause)
    {
        // Updating the tooltip lines
        this.headlineInformationGT.setText("The Game is over!");
        this.headlineInformationGT.setFill(Color.black);
        this.headlineInformationGT.setBold(true);
        this.firstLineInformationGT.setText("");
        this.firstLineInformationGT.setBold(true);
        this.secondLineInformationGT.setText(cause);

        //replacing the active player
        this.activePlayerGT.setText("Game Over:");
        this.activePlayerGT.setFill(Color.BLACK);
        this.activePlayerGT.setFontSize(25);

        // List the winning player names
        if(winners.size() == this.control.getPlayerCount()) // if everybody wins its declared a draw
        {
            GText gameIsDraw = new GText(HUDLEFT + HUDBORDERSPACE,HUDACTIVEPLAYERHEIGHT + 30,"It's a draw!");
            gameIsDraw.setFontSize(20);
            this.panel.getLayerHUD().addChild(gameIsDraw);
        }
        else
        {
            for(int i = 0; i < winners.size(); i++) // Write all winners names
            {
                String t = this.control.getSettings().playerNames.get(winners.get(i)-1);
                if(t.length() > HUDMAXNAMELENGTH * 2)
                {
                    t = t.substring(0, HUDMAXNAMELENGTH * 2) + ".";
                }
                GText nextWinner = new GText(HUDLEFT + HUDBORDERSPACE,HUDACTIVEPLAYERHEIGHT + (1+i) * 30,t);
                nextWinner.setFill(this.control.getSettings().playerColors.get(winners.get(i)-1));
                this.panel.getLayerHUD().addChild(nextWinner);
            }
            if(winners.size() > 1) // plural
            {
                GText victorious = new GText(HUDLEFT + HUDBORDERSPACE,HUDACTIVEPLAYERHEIGHT + (winners.size()+1) * 30,"are victorious!");
                victorious.setFontSize(20);
                this.panel.getLayerHUD().addChild(victorious);
            }
            else // singular
            {
                GText victorious = new GText(HUDLEFT + HUDBORDERSPACE,HUDACTIVEPLAYERHEIGHT + (winners.size()+1) * 30,"is victorious!");
                victorious.setFontSize(20);
                this.panel.getLayerHUD().addChild(victorious);
            }

        }
        if(winners.size() != this.control.getPlayerCount()) // if its not a draw
        {
            // Set the winning scores bold and add smile to it
            for (int i = 0; i < winners.size(); i++) {
                GText winnerName = this.playerNames[winners.get(i) - 1];
                winnerName.setText(winnerName.getText() + " :)");
                winnerName.setBold(true);
            }
        }
        // repaints
        this.panel.getLayerHUD().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
    }

    /**
     * Deletes the SVG Elements of the last painted Tile
     * used during a human turn to remove the tile if the player
     * decides to play on another position.<br>
     */
    public void cleanLastPaintedTile(){
        for(int i = 0; i < this.lastTileElements.size();i++)
        {
            try {
                this.panel.getLayerMain().removeChild(this.lastTileElements.get(i));
            }
            catch( Exception e){
                throw new RuntimeException(e);
            }
        }
        this.lastTileElements.removeAllElements();
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
    }

    /**
     * Rescales the displayed size of the game board on layer main<br>
     * @param x Factor to which the scaling should be set.
     */
    public void setZoom(float x){

        this.scale = x;
        this.panel.getLayerMain().transform().scale(x,x);
        this.panel.getLayerMain().update();
        this.repaint();
        this.panel.updateScale();
        this.control.parentFrame.repaint();
    }

    /**
     * Used to zoom in and out.
     * @param x incremental change to the current scale factor.
     */
    public void zoom(float x)
    {
        this.setZoom(this.scale + x);
        this.moveBoard((int)(Math.signum(x)*(-1)),(int)Math.signum(x)*(-1));
        this.panel.getLayerMain().update();
        this.panel.repaint();
    }

    /**
     * Is called to mark a position for a possible placement in the current turn.<br>
     * - Draws a lightblue hex to the main layer<br>
     * - creates a OpenPositionListener for the hex<br>
     * - adds the GElement to the lastMarkerElements<br>
     * - adds the position to the occupiedPositions Set<br>
     *
     * @param index Index in BasicBoards tileTree logic
     */
    public void markHexforPossiblePosition(int index){

        int x = gindexToX(index);
        int y = gindexToY(index);

        try {
            GElement highlightedTile = panel.loadSVG(new FileReader("graphics/components/lightblue.svg"));
            highlightedTile.transform().translate(getXTranslation(x),getYTranslation(x,y));
            highlightedTile.setStroke(Color.lightGray);
            OpenPositionListener opl = new OpenPositionListener(this,this.control,index);
            highlightedTile.setMouseListener(opl);
            this.lastMarkerElements.put(index,highlightedTile);
            this.occupiedPositions.add(index);
            this.panel.getLayerMain().addChild(highlightedTile);

        }
        catch (Exception E){
            throw new RuntimeException(E);
        }

    }


    /**
     * draws a grey hex to the specified index.<br>
     * used to reset back to grey during human turns.<br>
     * @param i index in BasicBoard tileTree logic.
     * @see BasicBoard tileTree
     */
    private void resetHex(int i){
        int x = gindexToX(i);
        int y = gindexToY(i);

        try {
            GElement hex = panel.loadSVG(new FileReader("graphics/components/grey.svg"));
            hex.transform().translate(getXTranslation(x), getYTranslation(x, y));
            hex.setStroke(Color.darkGray);
            this.panel.getLayerMain().addChild(hex);
        }
        catch (Exception E){
            throw new RuntimeException(E);
        }

    }

    /**
     * Deletes all GElements composing the current set of discovered tiles displayed in the HUD
     */
    public void clearPreviousOpenCards() {
        try {
            for (int i = 0; i < this.lastHandCardElements.size(); i++) {
                if(this.lastHandCardElements.get(i) != null) {
                    this.panel.getLayerHUD().removeChild(this.lastHandCardElements.get(i));
                }
            }
            this.lastHandCardElements.removeAllElements();
            this.panel.getLayerHUD().update();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * 
     *<br>
     * Displays that it is the Players turn.<br>
     * If the Players name is longer than HUDMAXNAMELENGTH it is shortened.<br>
     * @param p the player whose name should be set as active player
     */
    @Override
    public void setActivePlayer(Player p){

        try{
            String t = p.getName();
            if(t.length() > HUDMAXNAMELENGTH)
            {
                t = t.substring(0,HUDMAXNAMELENGTH) + "."; // shorten the name if necessary
            }
            if(this.activePlayerGT != null){
                this.activePlayerGT.setText(t + "'s turn");
                this.activePlayerGT.setFill(this.control.getSettings().playerColors.get(this.control.getActivePlayerID()-1));
                this.activePlayerGT.setFontSize(36);
                this.activePlayerGT.update();
            }else{ // Happens only at the first call
                this.activePlayerGT = new GText(HUDLEFT+10,HUDACTIVEPLAYERHEIGHT,t + "'s turn");
                this.activePlayerGT.setFill(this.control.getSettings().playerColors.get(this.control.getActivePlayerID()-1));
                this.activePlayerGT.setFontSize(36);
                this.panel.getLayerHUD().addChild(activePlayerGT);
            }

        }catch (Exception e){
            throw new RuntimeException("Fehler beim Setzen des aktiven Spielers");
        }
    }

    /**
     * 
     *<br>
     * Displays the Player names in their Player Color on the HUD<br>
     * - The GTexts containing their scores are added under the name.<br>
     * @param PlayerList the players, sorted in order of playerid
     * @param PlayerColor color of the corresponding player
     */
    @Override
    public void setPlayers(List<Player> PlayerList, List<Color> PlayerColor){
        // stores references to edit the names (e.g. to make them bold if they are winners)
        this.playerNames = new GText[PlayerList.size()];
        String[] names = new String[PlayerList.size()];

        // goes through the list of Players and shortens their display name if necessary
        for(int i = 0; i < PlayerList.size(); i++)
        {
            String t;
            try
            {
                t = PlayerList.get(i).getName();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            if(t.length() > HUDMAXNAMELENGTH)
            {
                t = t.substring(0,HUDMAXNAMELENGTH) + ".";
            }
            names[i] = t;
        }

        int x = HUDLEFT + HUDBORDERSPACE;
        int y = HUDPLAYERNAMESHEIGHT;
        try{
            // finds the longest player name in the first column of the HUD
            // used to sync the indentation
            int maxlength = names[0].length();
            for(int i = 1; i < PlayerList.size(); i +=2)
            {
                if(names[i].length() > maxlength)
                {
                    maxlength = names[i].length();
                }
            }
            // the space between the first row of names and the second
            int rowSpacing = 50;
            // the space between the player's name and score
            int nameScoreSpacing = 20;

            for(int i = 0; i < PlayerList.size();i++){
                // 2 x 2 Table of player names, i==2 indicates 2nd row
                if(i == 2)
                {
                    y = y + rowSpacing;
                    x = HUDLEFT + HUDBORDERSPACE;
                }
                GText newPlayer = new GText(x,y,names[i]);
                newPlayer.setFontSize(24);
                newPlayer.setFill(PlayerColor.get(i));
                // adds the score
                this.scoresGT[i] = new GText(x,y + nameScoreSpacing,"0");
                this.scoresGT[i].setFontSize(24);
                this.scoresGT[i].setFill(PlayerColor.get(i));
                this.panel.getLayerHUD().addChild(this.scoresGT[i]);
                this.playerNames[i] = newPlayer;
                this.panel.getLayerHUD().addChild(newPlayer);
                x +=  maxlength * 20;
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }

    /**
     * 
     *<br>
     * Informs the GUI that a player is skipped.<br>
     * Displays that the player is skipped in the HUD.<br>
     * If the player is not skipped nothing is shown.<br>
     *
     * @param player the player
     * @param skipped true - if the player has been skipped, false - otherwise
     */
    @Override
    public void setPlayerSkipped(Player player, boolean skipped){
        String t;

        try {
            t = player.getName();
            if(t.length() > HUDMAXNAMELENGTH)
            {
                t = t.substring(0,HUDMAXNAMELENGTH) + ".";
            }
            if(skippedPlayer != null)
            {
                if(skipped == true) {
                    skippedPlayer.setText("Skipped: " + t);
                    skippedPlayer.update();

                }else{
                    skippedPlayer.setText("");
                    skippedPlayer.update();
                }

            }
            else
            {
                if(skipped == true)
                {
                    skippedPlayer = new GText(HUDLEFT + HUDBORDERSPACE, HUDPLAYERSKIPPEDHEIGHT, "Skipped: " + player.getName());
                    this.panel.getLayerHUD().addChild(skippedPlayer);
                    skippedPlayer.update();
                }

            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        this.panel.getLayerHUD().update();
        this.panel.repaint();
    }

    /**
     * Is not used.<br>
     * @see BasicGui drawtile for alternative.<br>
     * @param tile a tile to be placed
     * @param tilePlacement the TilePlacement
     * @param owner the owner of the tile
     */
    @Override
    public void placeTile(Tile tile, TilePlacement tilePlacement, Player owner) {
        throw new UnsupportedOperationException("Nicht implementiert");
    }

    /**
     * Draws empty (grey) hexes to the discovered tiles displayed in the hud.<br>
     * Is used at the ending phase of the game when no new tiles are discovered.<br>
     * Indicating that the game is about to end.<br>
     *
     * @param missing number of empty places.
     */
    public void drawEmptyOpenCards(int missing){

        for (int i = 0; i < missing; i++){
            try {
                GElement gecent = this.panel.loadSVG(new FileReader("graphics/components/grey.svg"));
                gecent.transform().translate( HUDLEFT + 330 - (i * 65),HUDDISCOVEREDTILESHEIGHT);
                this.panel.getLayerHUD().addChild(gecent);
                this.lastHandCardElements.add(gecent);
                this.panel.repaint();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Used to translate the Layer Main for scrolling over the game board.<br>
     * Translates in steps of Hexes.<br>
     * @param x number of columns to move right or left
     * @param y number of rows to move up or down
     */
    public void moveBoard(int x, int y){
        // updates the current viewTranslation values
        this.viewTranslationX += (float) x * HEXWIDTH;
        this.viewTranslationY += (float) y * HEXHEIGHT;
        this.panel.getLayerMain().transform().translate(viewTranslationX * this.scale, viewTranslationY * this.scale);
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
        this.panel.updateScale();
    }

    /**
     * Used to calculate the the pixel translation of a hex for a column
     * @param x column of the hex
     * @return pixel coordinate x , (0,0) is  upperleft
     */
    public float getXTranslation(int x){
        return (x * 75);
    }

    /**
     * Used to calculate the the pixel translation of a hex for a row
     * @param x column of the hex
     * @param y row of the hex
     * @return pixel coordinate y , (0,0) is  upperleft
     */
    public float getYTranslation(int x, int y){
        int xt = x;
        int yt = y;
        if(xt % 2 != 0)
            return (float)(HEXHEIGHT / 2.0) + ( yt * HEXHEIGHT);
        else
            return (yt * HEXHEIGHT);
    }

    /**
     * Calculates a game boards column value from an index in tileTree logic
     * @param index index in tileTree logic
     * @return column value
     */
    public int gindexToX(int index){
        return (index / this.height) + this.offSetX;
    }

    /**
     * Calculates a game boards row value from an index in tileTree logic
     * @param index index in tileTree logic
     * @return row value
     */
    public int gindexToY(int index){
        return (index % this.height) + this.offSetY;
    }

    /**
     * Calculates an index in tileTree logic from row and column values
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @return column value
     */
    public int gxyToIndex(int x, int y)
    {
        return ((((x - this.offSetX)) * this.height) + (y - this.offSetY));
    }


    /**
     * Returns the reference of the GPanel containing all graphics.<br>
     * Should be used to pass it to main<br>
     * @return Referenz auf das GPanel mit dem Spielbrett.
     */
    public GPanel getPanel(){
        return this.panel;
    }

    /**
     * Called to draw a tile to the main layer
     * @param p position to draw
     * @param t tile to draw
     * @param id ID of the tile owner (for color)
     * @throws Exception passing drawTile Exception
     */
    public void drawTileToGui(TilePlacement p, BasicTile t, int id) throws Exception{
        int x = p.getColumn();
        int y = p.getRow();
        BasicTile temp = t.getRotated(p.getRotation());
        this.drawTile(p.getColumn(),p.getRow(),temp,id);
        this.repaint();
        this.panel.repaint();
        this.control.parentFrame.repaint();

    }

    /**
     * Called to draw grey hexes around a specific tile.<br>
     * Draws OUTLINEDEPTH deep layers of grey hexes around the specific tile
     * if the positions are unoccupied positions<br>
     *
     * @param index GameBoard tileTrees index around which the grey hexes should be painted
     */
    private void drawGreyOutline(int index) {
        int numberOfpossiblePositions = 0;
        // calculates the number of possible positions for Outlines
        for(int i = 1; i <= OUTLINEDEPTH; i++)
        {
            numberOfpossiblePositions += (6 * i);
        }
        // used to store the indexes of possible positions
        int[] neightbours = new int[numberOfpossiblePositions];
        int n = 0;
        int current = index;
        // goes in an spiral around the index tile determining the neighbours indexes
        for (int layer = 0; layer < OUTLINEDEPTH; layer++) {
            current = this.adjacentTile(current, 0);
            neightbours[n] = current;
            n++;
            for (int direction = 2; direction < 7; direction++)
            {
                for (int j = 0; j <= layer; j++)
                {
                    current = this.adjacentTile(current, direction % 6);
                    neightbours[n] = current;
                    n++;
                }
            }
            for(int j = 0; j < layer;j++)
            {
                current = this.adjacentTile(current,1);
                neightbours[n] = current;
                n++;
            }
            current = this.adjacentTile(current,1);
        }
        // goes through the list and checks for occupation
        for(int i = 0; i < neightbours.length; i++)
        {
            int temp = neightbours[i];
            if(!this.occupiedPositions.contains(temp))
            {
                this.resetHex(temp); // paints it grey
            }
        }
    }

    /**
     * Is used to find calculate a neighbouring tile's index in the tilesTree<br>
     * @param index Index of the actual tile in the tilesTree
     * @param direction direction of the neighbour in question
     * @return index of an adjacent hexfield in the tiles vector, -1 if the hex is out of the index range (see tilesTree)
     */
    private int adjacentTile(int index, int direction){
        int indexAT = 0;
        int y = this.gindexToY(index);
        int x = this.gindexToX(index);

        switch (direction){
            case 0: indexAT -= 1;
                break;
            case 1: indexAT = (x % 2) == 0 ? this.height - 1 : this.height;
                break;
            case 2: indexAT = (x % 2) == 0 ? this.height     : this.height + 1;
                break;
            case 3: indexAT += 1;
                break;
            case 4: indexAT = (x % 2) == 0 ? (this.height) * (-1) : (this.height -1) * (-1);
                break;
            case 5: indexAT = (x % 2) == 0 ? (this.height + 1) * (-1) : (this.height) * (-1);
                break;

            default: return -1;
        }
        return index + indexAT;
    }

    /**
     * Draws a tile to the Layer Main<br>
     * @param x column of the game board
     * @param y row of the game board
     * @param t the tile to place
     * @param id the id of the tile owner
     * @throws Exception throws GPANELS Exceptions, File Operations
     */
    public void drawTile(int x, int y, BasicTile t, int id) throws Exception{
        // remove all graphics from the previous tile
        this.lastTileElements.removeAllElements();
        int index = this.gxyToIndex(x,y);
        this.occupiedPositions.add(index);
        this.drawGreyOutline(index);
        Vector<GElement> parts = new Vector<GElement>();
        Biome center = t.getCenter();
        String file = "";
        Color pcolor;
        // if the id is 0 it belongs to no player
        if(id == 0){
            pcolor = Color.BLACK;
        }else {
            pcolor = this.control.getSettings().playerColors.get(id - 1);
        }
        // chooses the file to load the center graphics
        switch (center) {
            case TRAINTRACKS:
                file = "graphics/components/simple_traintracks_center.svg";
                break;
            case HOUSES:
                file = "graphics/components/simple_houses_center.svg";
                break;
            case WATER:
                file = "graphics/components/simple_water_center.svg";
                break;
            case FIELDS:
                file = "graphics/components/simple_fields_center.svg";
                break;
            case FOREST:
                file = "graphics/components/simple_forest_center.svg";
                break;
            case PLAINS:
                file = "graphics/components/simple_plains_center.svg";
                break;
        }
        GElement gecent = this.panel.loadSVG(new FileReader(file));
        // Puts the center in position and adds it
        gecent.transform().translate(getXTranslation(x),getYTranslation(x,y));
        this.panel.getLayerMain().addChild(gecent);
        this.lastTileElements.add(gecent);

        // draws the edges
        for(int i = 0; i < 6; i++){

            Biome edge = t.getEdge(i);

            switch (edge) {
                case TRAINTRACKS:
                    file = "graphics/components/simple_traintracks_edge.svg";
                    break;
                case HOUSES:
                    file = "graphics/components/simple_houses_edge.svg";
                    break;
                case WATER:
                    file = "graphics/components/simple_water_edge.svg";
                    break;
                case FIELDS:
                    file = "graphics/components/simple_fields_edge.svg";
                    break;
                case FOREST:
                    file = "graphics/components/simple_forest_edge.svg";
                    break;
                case PLAINS:
                    file = "graphics/components/simple_plains_edge.svg";
                    break;
            }
            GElement edgePart = this.panel.loadSVG(new FileReader(file));
            // Players color is added
            edgePart.setStroke(pcolor);
            edgePart.setStrokeOpacity(0.8f);
            parts.add(edgePart);
            this.lastTileElements.add(edgePart);
        }
        // Rotates the edges according to their position on the tile
        for(int i = 0; i < 6; i++) {

            int rotation = 0; // saves the angle of rotation
            double xanchor = 0.0; // saves the x value of the rotations fix point
            double yanchor = 0.0; // saves the y value of the rotations fix point

            switch (i) {
                case 0:
                    rotation += 0;
                    break;
                case 1:
                    rotation += 60;
                    xanchor = 75;
                    break;
                case 2:
                    rotation += 120;
                    xanchor = 37.5;
                    yanchor = 21.6506;
                    break;
                case 3:
                    rotation += 180;
                    xanchor = 50;
                    yanchor = 21.6506 / 2.0;
                    break;
                case 4:
                    rotation += (-120);
                    xanchor = 62.5;
                    yanchor = 21.6506;
                    break;
                case 5:
                    rotation += (-60);
                    xanchor = 25;
                    break;
            }

            parts.get(i).transform().rotate(rotation, (float)xanchor, (float)yanchor);
        }

        for(int i = 0; i < 6; i++){

            float xshift = getXTranslation(x);
            float yshift = getYTranslation(x,y);
            // the height of an edge element
            double up   = 21.6506;
            // the distance between edge 0 and 3
            double dist03 = 43.301235094611;
            // the inner lenght of the edge element
            double inneredgelength = 62.5 - 37.5;
            // the distance between center element and the right edge point
            double verticaloverarch = 75.0 - 62.5;

            switch (i){
                case 0:
                    break;
                case 1: yshift += up * 2;
                    xshift += 25;
                    break;
                case 2: yshift += dist03 / 2.0;
                    xshift += inneredgelength + verticaloverarch;
                    break;
                case 3: yshift += dist03 + up;
                    break;
                case 4: yshift += dist03 / 2.0;
                    xshift -= (inneredgelength + verticaloverarch);
                    break;
                case 5: yshift += up * 2;
                    xshift += (-25);
                    break;
            }
            parts.get(i).transform().translate(xshift,yshift);
        }

        for (int i = 0; i < 6; i++) {
            this.panel.getLayerMain().addChild(parts.get(i));
        }
        this.panel.getLayerMain().update();
        this.panel.repaint();
        this.control.parentFrame.repaint();
    }

    /**
     * Draws a discoverd tile to the HUD Layer.<br>
     * @param t the discovered tile
     * @param pos the position it should be placed on the open cards deck
     *            (Between 0 and playercount + 1)
     * @throws Exception File operations, GPanel
     */
    public void drawOpenTile(BasicTile t, int pos) throws Exception{
        Vector<GElement> parts = new Vector<GElement>();
        Biome center = t.getCenter();
        String file = "";

        switch (center) {
            case TRAINTRACKS:
                file = "graphics/components/simple_traintracks_center.svg";
                break;
            case HOUSES:
                file = "graphics/components/simple_houses_center.svg";
                break;
            case WATER:
                file = "graphics/components/simple_water_center.svg";
                break;
            case FIELDS:
                file = "graphics/components/simple_fields_center.svg";
                break;
            case FOREST:
                file = "graphics/components/simple_forest_center.svg";
                break;
            case PLAINS:
                file = "graphics/components/simple_plains_center.svg";
                break;
        }
        GElement gecent = this.panel.loadSVG(new FileReader(file));
        gecent.transform().translate( HUDLEFT + 330 - (pos * 65),HUDDISCOVEREDTILESHEIGHT);

        this.panel.getLayerHUD().addChild(gecent);
        this.lastHandCardElements.add(gecent);

        for(int i = 0; i < 6; i++){

            Biome edge = t.getEdge(i);

            switch (edge) {
                case TRAINTRACKS:
                    file = "graphics/components/simple_traintracks_edge.svg";
                    break;
                case HOUSES:
                    file = "graphics/components/simple_houses_edge.svg";
                    break;
                case WATER:
                    file = "graphics/components/simple_water_edge.svg";
                    break;
                case FIELDS:
                    file = "graphics/components/simple_fields_edge.svg";
                    break;
                case FOREST:
                    file = "graphics/components/simple_forest_edge.svg";
                    break;
                case PLAINS:
                    file = "graphics/components/simple_plains_edge.svg";
                    break;
            }
            parts.add(this.panel.loadSVG(new FileReader(file)));

        }

        // Rotates the edges according to their position on the tile
        for(int i = 0; i < 6; i++) {
               int rotation = 0; // saves the angle of rotation
               double xanchor = 0.0; // saves the x value of the rotations fix point
               double yanchor = 0.0; // saves the y value of the rotations fix point


            switch (i) {
                case 0:
                    rotation += 0;
                    break;
                case 1:
                    rotation += 60;
                    xanchor = 75;
                    break;
                case 2:
                    rotation += 120;
                    xanchor = 37.5;
                    yanchor = 21.6506;
                    break;
                case 3:
                    rotation += 180;
                    xanchor = 50;
                    yanchor = 21.6506 / 2.0;
                    break;
                case 4:
                    rotation += (-120);
                    xanchor = 62.5;
                    yanchor = 21.6506;
                    break;
                case 5:
                    rotation += (-60);
                    xanchor = 25;
                    break;
            }

            parts.get(i).transform().rotate(rotation, (float)xanchor, (float)yanchor);
        }

        for(int i = 0; i < 6; i++){

            double xshift = HUDLEFT + 330 - (pos * 65);
            double yshift = HUDDISCOVEREDTILESHEIGHT;
            // the height of an edge element
            double up   = 21.6506;
            // the distance between edge 0 and 3
            double dist03 = 43.301235094611;
            // the inner lenght of the edge element
            double inneredgelength = 62.5 - 37.5;
            // the distance between center element and the right edge point
            double verticaloverarch = 75.0 - 62.5;

            switch (i){
                case 0:
                    break;
                case 1: yshift += up * 2;
                    xshift += 25;
                    break;
                case 2: yshift += dist03 / 2.0;
                    xshift += inneredgelength + verticaloverarch;
                    break;
                case 3: yshift += dist03 + up;
                    break;
                case 4: yshift += dist03 / 2.0;
                    xshift -= (inneredgelength + verticaloverarch);
                    break;
                case 5: yshift += up * 2;
                    xshift += (-25);
                    break;
            }
            parts.get(i).transform().translate((float)xshift,(float)yshift);
        }

        for (int i = 0; i < 6; i++) {
            this.panel.getLayerHUD().addChild(parts.get(i));
            this.lastHandCardElements.add(parts.get(i));
        }
    }
}
