package doerfer.testing;
import doerfer.preset.*;
import doerfer.preset.graphics.GPanel;
import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * controls game order <br>
 * - organises turn order <br>
 * - ends game <br>
 */
public class BasicControl{
    /**
     * ID of active Player
     */
    private int activePlayerID;

    /**saves Tile list of preplaced Tiles */
    private List<Tile> preplacedTiles;

    /** Saves postion list of preplaced Tiles */
    private List<TilePlacement> preplacedPositions;

    /** temp Player */
    private Player tmpPlayer;
    /**
     * Reference of Configuration
     */
    private BasicConf config;


    /**
     * active game board
     */
    private BasicBoard activeGameBoard;

    /**
     * reference of all players
     */
    private List<Player> players;


    /**
     * increases if player cannot place a Tile. If all players consecutively couldnt place a Tile then game Over
     */
    private int consecutiveImpossibleMoves;

    /** reference of settings */
    private Settings settings;


    /**
     * reference of gui
     */
    private BasicGui gui;

    /**
     * reference of mainframe
     */
    public Frame parentFrame;

    /** Saves last placed Tile*/
    private BasicTile lastPlacedTile;

    /**  Saves position of last placed Tile*/
    private int indexOfLastPlacedTile;

    /** Saves remaining number of Tiles in stack */
    private int remainingCards;


    /** Saves the Number of Tiles in stack */
    private int placedTilesCount;

    /** Saves Width of board in Hexfeldern */
    private int boardWidthHex;

    /** Saves Height of board in Hexfiels */
    private int boardHeightHex;

    /** Saves active HumanPlayer */
    private BasicHumanPlayer activeHumanPlayer;


    /** Requests, if active Player is human */
    private boolean activePlayerHuman;
    /**
     * Dummy Player, who has Tiles which no Player owns
     */
    private Random_AIPlayer controlPlayer;
    /**
     * TilePlacement of last Move
     */
    private TilePlacement lastPlacedMove;
    /**
     * Offset of X-Koordinate
     */
    private int offSetX;
    /**
     * oddSet of Y-Koordinate
     */
    private int offSetY;
    /**
     * request, if game is Over
     */
    private boolean gameIsOver;
    /**
     * Number of Players
     */
    private int playerCount;
    /**
     * active Player
     */
    private Player activePlayer;
    /**
     * Saves last Move of each Player
     */
    private TilePlacement[] lastMoveOfPlayer;
    /**
     * List of open Cards from stack
     */
    private List<BasicTile> openCards;
    /**
     * List of Owners of preplaced Tiles
     */
    private List<Integer> OwnerPreplaced;
    /**
     * 
     *
     * Starts game and controls game process
     *
     * @param f Mainframe of game
     * @param parsedSettings reference of settings, which were changed from Argument Parser
     *
     */
    BasicControl(Frame f, Settings parsedSettings){
        //init übergebene Frame, Setting
        parentFrame = f;
        settings = parsedSettings;

        activePlayerID = 1; //Setzt den ersten Spieler als aktiven Spieler ein
        playerCount = settings.playerTypes.size(); //Anzahl der Spieler von Settings erkennen
        players = new ArrayList<>(); //init Liste der Spieler
        config = new BasicConf(settings); //Einlesen der Konfigurationen
        indexOfLastPlacedTile = -1;
        openCards = new ArrayList<>();

        //init Variablen, welche die Eingaben aus der Configuration erhalten
        remainingCards = this.config.getNumTiles();
        preplacedTiles = this.config.getPreplacedTiles();
        preplacedPositions = this.config.getPreplacedTilesPlacements();
        OwnerPreplaced =  this.config.getPreplacedTilesPlayerIDs();

        placedTilesCount = 0; //Anzahl der bereits gesetzten Tiles auf 0 setzen

        //Größe des Boards und OffSet erhalten
        boardWidthHex = this.findBoardSizeX();
        boardHeightHex = this.findBoardSizeY();
        offSetY = this.findBoardOffsetY();
        offSetX = this.findBoardOffsetX();
        //init Gui
        gui = new BasicGui(this.boardWidthHex,this.boardHeightHex,this.offSetX,this.offSetY,this); // Ruft die GUI auf.

        //Erstelle ControlPlayer
        controlPlayer = new Random_AIPlayer(this.boardWidthHex, this.boardHeightHex, gui, this, "none", settings);

        lastMoveOfPlayer = new TilePlacement[this.playerCount]; //Speichert letzte Platzierung aller Spieler
        gameIsOver = false; //Spiel ist noch nicht vorbei

        
        try
        {
            controlPlayer.init(config,0); //initialisiere den controlPlayer
        }
        catch (Exception e)
        {
            throw new RuntimeException("Fehler beim Initialisieren des Control-Players");
        }

        // Erschafft die Player Instanzen.
        for(int i = 0; i < playerCount; i++){

            try{
                //Überprüft, ob Human Player oder Random_Ai Player, dann diesen erstellen und der Player Liste hinzufügen
                if(this.settings.playerTypes.get(i) == PlayerType.HUMAN){
                    players.add(new BasicHumanPlayer(this.boardWidthHex, this.boardHeightHex, gui, this, this.settings.playerNames.get(i),settings));
                }
                if(this.settings.playerTypes.get(i) == PlayerType.RANDOM_AI){
                    players.add(new Random_AIPlayer(this.boardWidthHex, this.boardHeightHex, gui, this, this.settings.playerNames.get(i),settings));
                }
                players.get(i).init(config,i+1);//Initialisiere Player
            }
            catch (Exception e){
                throw new RuntimeException("Fehler beim Initialisieren des Spielers mit ID " + (i + 1));
            }
        }

        this.gui.setPlayers(players, this.settings.playerColors);//Übergebe der Gui die Spieler und deren Farbe
    }

    /**
     * 
     *
     * Initializes new game <br>
     * -Called from Constructor <br>
     *
     */
    public void initNewGame(){

        this.gameIsOver = false;

        // Informiert die Playerinstanzen ueber die Preplaced Tiles

        this.activePlayerID = 1; //Der erste Zug beginnt mit dem Spieler der ID 1

        //Speichert die zuletzt gelegten preplacedTiles der jeweiligen Spieler
        for(int j = 0; j < this.getPlayerCount(); j++) {
            for(int z = 0; z < this.config.getPreplacedTiles().size(); z++){
                if(this.config.getPreplacedTilesPlayerIDs().get(z) == j+1){
                    this.lastMoveOfPlayer[j] = this.preplacedPositions.get(z);
                    
                }
            }
        }

        // Prueft ob der erste Spieler menschlich ist.
        if(this.settings.playerTypes.get(0) == PlayerType.HUMAN){
            tmpPlayer = players.get(0);
            this.activeHumanPlayer = (BasicHumanPlayer) tmpPlayer;
            this.activeGameBoard = this.activeHumanPlayer.getGameBoard(); //holt das Brett des meschlichen Spielers
            this.activePlayerHuman = true;
        }
        else{
            this.activePlayerHuman = false;
        }

        //zeichnet Tile auf Gui
        //zeichnet die preplacedTiles auf die GUI
        for (int i = 0; i < this.config.getPreplacedTiles().size(); i++) {
            try {
                this.gui.drawTileToGui(this.preplacedPositions.get(i), (BasicTile) this.preplacedTiles.get(i), this.config.getPreplacedTilesPlayerIDs().get(i));


            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Zeichnen eines Preplaced Tiles an Position " + this.preplacedPositions.get(i));
            }
        }


        // Ziehen der offenen Karten
        for(int z = 0; z < this.playerCount+1;z++) {
            this.DrawNewOpenCard();
        }
        this.updateOpenCardsOnGui();
        
        this.gui.focus(lastMoveOfPlayer[activePlayerID-1]); // focus auf den ersten Spieler
        this.gui.setTilesLeft(this.config.getNumTiles()-this.placedTilesCount); //gibt der GUI wieviele Karten noch auf dem Stack liegen
        this.startNextTurn();
    }


    /**
     * 
     *
     * Starts turn of active Player <br>
     * -marks all possible fields <br>
     *
     */
    private void startNextTurn() {
        
        this.gui.focus(this.lastMoveOfPlayer[this.activePlayerID-1]);
        try {
            if(!activePlayerHuman) {
                Thread.sleep(this.settings.delay);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        this.activePlayer = this.players.get(this.activePlayerID-1); //erhält den momentan aktiven Spieler
        this.gui.setActivePlayer(this.activePlayer);

        
            // Prüfe, ob der aktive Spieler ein Mensch ist
        if (this.activePlayer instanceof BasicHumanPlayer) {
            this.activePlayerHuman = true;
            ((BasicHumanPlayer) this.activePlayer).getGameBoard().markPossibleFields();

            // Falls der Mensch keinen Zug machen kann, direkt zum nächsten Zug springen
            if (!this.activeGameBoard.couldTurnBeMade()) {
                this.turnOver();
            }
            return;
        }

        // Falls der Spieler eine KI ist, starte Zug in einem separaten Thread
        this.activePlayerHuman = false;
        new Thread(() -> {
            try {
                Thread.sleep(this.settings.delay); // Wartezeit für KI-Zug
                if (this.activePlayer instanceof Random_AIPlayer) {
                    ((Random_AIPlayer) this.activePlayer).getGameBoard().markPossibleFields(); // KI-Felder markieren
                }
                Thread.sleep(this.settings.delay);
                turnOver(); // KI-Zug ausführen
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        }
        
    



    /**
     * 
     *
     * Notifies all Players of turn and places Tile in GUI <br>
     * -tests if game is over <br>
     * -starts nextTurn if the game isn't over <br>
     *
     */
    public void turnOver(){
        // Erfragt das TilePlacement des aktiven Players (also den Zug)
        try {
            this.lastPlacedMove = this.players.get(this.activePlayerID-1).requestTilePlacement();
        }
        catch (Exception e){
            throw new RuntimeException("Fehler beim Abrufen des TilePlacement des Spielers mit ID " + this.activePlayerID);
        }
        
        if(this.lastPlacedMove != null) {
            
            this.lastMoveOfPlayer[this.activePlayerID-1] = this.lastPlacedMove; //Speichert letzten Zug des Spielers
            this.gui.setPlayerSkipped(this.players.get(this.activePlayerID-1),false); // Wird nicht geskipped
            this.placedTilesCount++; //erhöht die anzahl der Züge
            this.lastPlacedTile = this.openCards.get(this.openCards.size()-1); //der letzte platzierte Tile der erste Tile aus dem Stapel
            this.consecutiveImpossibleMoves = 0; // Falls Zug möglich, Variable zum checken, ob es noch alle Spieler noch legen können auf 0 setzen
            try {
                //notify andere Spieler über Zug
                for (int i = 0; i < this.playerCount; i++) {
                    if(i == this.activePlayerID-1)
                    {
                        continue; // Der aktive Spieler soll nicht informiert werden.
                    }
                    else {
                        this.players.get(i).notifyTilePlacement(this.lastPlacedMove);
                    }
                }
                //auch nicht existierender Spieler wird informiert
                this.controlPlayer.notifyTilePlacement(this.lastPlacedMove);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if(!this.activePlayerHuman) {
                try {
                    this.gui.drawTileToGui(this.lastPlacedMove, this.lastPlacedTile, this.activePlayerID); //zeichnet platzierten Tile
                    this.gui.focus(this.lastMoveOfPlayer[this.activePlayerID-1]);

                    Thread.sleep(this.settings.delay);

                    
            
                } catch (Exception e) {
                    throw new RuntimeException("Fehler beim Zeichnen des zuletzt platzierten Tiles durch Spieler " + this.activePlayerID);
                }

            }

        }else {
            this.gui.setPlayerSkipped(this.players.get(this.activePlayerID-1),true); //Angezeigt, dass Player geskipped
            this.consecutiveImpossibleMoves++;
        }


        this.gui.setTilesLeft(this.config.getNumTiles()-this.placedTilesCount); //zeigt die übrigen Tiles im Stapel an
        //Falls es eine ganze Runde gab an dem kein Spieler legen, konnte dann Game Over
        if(this.consecutiveImpossibleMoves >= this.playerCount) {
            this.gameOver("The game ended, no player could place this tile!");
        }

        //Game Over falls keine Karten mehr im stapel
        if(this.placedTilesCount >= this.config.getNumTiles()) {
            this.gui.drawEmptyOpenCards(this.playerCount+1);
            this.gameOver("The game ended, there are no cards left!");
        }
        //Falls kein Game Over und nicht geskipped, dann neue Karte ziehen
        if(!this.gameIsOver) {
            if(this.consecutiveImpossibleMoves == 0) {
                this.DrawNewOpenCard();
                this.updateOpenCardsOnGui();
            }

            this.activePlayerID++; //nächster Spieler
            this.activePlayerID = this.activePlayerID > this.getPlayerCount() ? 1 : this.activePlayerID; // Falls davor letzter Spieler, dann wieder auf ersten setzen

            //Falls menschlicher Spieler, dann GameBoard von Spieler erhalten
            if (this.settings.playerTypes.get(this.activePlayerID-1) == PlayerType.HUMAN) {
                tmpPlayer = this.players.get(this.activePlayerID-1);
                this.activeHumanPlayer = (BasicHumanPlayer) tmpPlayer;
                this.activeGameBoard = this.activeHumanPlayer.getGameBoard();
                this.activePlayerHuman = true;
            } else {
                this.activePlayerHuman = false;
            }


        }
        //Falls Spiel nicht vorbei, mit dem nächsten Zug anfangen
        if(!gameIsOver) {
            this.startNextTurn();
        }
    }



    /**
     * 
     *
     * Draws new random open Card on gui <br>
     * -notifies all Players <br>
     */
    private void DrawNewOpenCard(){

        try{
            //falls noch Karten vom Stapel übrig sind, Karte von den offen gelegten Karten löschen
            if((this.placedTilesCount > 0) && (this.openCards.size() > 0))  {
                this.openCards.remove((this.openCards.size()-1));
            }
            //falls noch Karten übrig sind neue Karte auf dem Stapel der offenen Karten generieren
            if(this.remainingCards>0) {

                List<Long> random = new ArrayList<Long>(); //Liste der von den Spielern neue generierte Zahl, um einen neuen Tile daraus zu schaffen
                long newNumber;
                TileGenerator generator = new TileGenerator(this.config);
                for (int j = 0; j < this.playerCount; j++) {
                    random.add(this.players.get(j).requestNextRandomNumber()); //erhält von jedem Spieler neue Zufallszahl

                }
                //Zufallszahlen werden zusammengerechnet und daraus ein Tile generiert
                newNumber = random.get(0);
                for (int z = 1; z < random.size(); z++) {

                    newNumber = newNumber ^ random.get(z);
                }
                List<Biome> biomesL = generator.generateTile(newNumber); //neue Karte wird aus Zufallszahl generiert
                BasicTile tempTile = new BasicTile(biomesL);

                // notifys alle Spieler über neue offene Karte
                for(int i = 0; i < this.playerCount; i++) {
                    this.players.get(i).notifyNewUncoveredTile(tempTile);
                }
                this.controlPlayer.notifyNewUncoveredTile(tempTile);

                this.openCards.add(0, tempTile); // added neue Karte zu den offenen Karten

            }
            this.remainingCards--;

        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }


    /**
     * 
     *
     * Used to update the uncovered tiles on the gui. <br>
     * - draws new open Card on HUD <br>
     */
    private void updateOpenCardsOnGui(){
        // the player count + 1 is the number of discoverd tiles
        int pcount = this.playerCount+1;
        // the number of the remaining tiles which could be drawn
        int ocount = this.remainingCards;

        this.gui.clearPreviousOpenCards();

        if(ocount <= 0){ // if the game is in its last moves empty hexes are painted
            this.gui.drawEmptyOpenCards(ocount * (-1));
            for(int i = Math.abs(ocount); i < pcount; i++){
                try {
                    this.gui.drawOpenTile(this.openCards.get(i + ocount), i);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        else{
            for(int i = 0; i < pcount; i++){
                try {
                    this.gui.drawOpenTile(this.openCards.get(i), i);
                }
                catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        this.gui.repaint();
    }

    /**
     * 
     *
     * Ends game <br>
     * - Informs that no moves can be made anymore <br>
     * @param cause the reason why the game is over
     *
     */
    public void gameOver(String cause){

        this.gameIsOver = true; // Informiert, dass das Ende des Spiels ist
        this.gui.setInfoStartMove(); //aktualisiert Punkte
        int winningID = 0;
        ArrayList<Integer> winningIDList = new ArrayList<Integer>();
        //Rechnet den Spieler mit den meisten Punkten aus
        for(int i = 0; i < this.playerCount ;i++) {
            if(this.controlPlayer.getScoreBoard()[i] > this.controlPlayer.getScoreBoard()[winningID]) {
                winningID = i;
                winningIDList.clear();
                winningIDList.add(i+1);
            }
            else if(this.controlPlayer.getScoreBoard()[i] == this.controlPlayer.getScoreBoard()[winningID]) {
                winningID =i;
                winningIDList.add(i+1);
            }
        }
        List<Integer> Pscore = new ArrayList<>();
        List<Long> Pseeds = new ArrayList<>();
        try {
            //Erhält Random seed und Score jedes Spielers
            for (int z = 0; z < this.players.size(); z++) {
                Pseeds.add(this.players.get(z).requestRandomNumberSeed());
                Pscore.add(this.players.get(z).getScore());

            }
        }catch (Exception e){
            throw new RuntimeException("Fehler beim Abrufen des Zufalls-Seeds oder Scores");
        }
        try {
            //Verify durch Seed und Score
            for (int i = 0; i < players.size(); i++) {
                players.get(i).verifyGame(Pseeds,Pscore);
            }
        }
        catch (Exception e) {
            this.gui.setInfoGameEnd(winningIDList,cause + "nicht verifiziert!");
            JOptionPane.showMessageDialog(this.parentFrame, "Spiel konnte nicht verifiziert werden!");
            throw new RuntimeException(e);
        }

        try {
            //Informiert, welcher Spieler gewonnen hat
            this.gui.setInfoGameEnd(winningIDList, cause);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Fehler beim Setzen der Informationen des Spielendes");
        }
        JOptionPane.showMessageDialog(this.parentFrame, "This game is over!");
    }

    /**
     * Returns settings of the game <br>
     * @return settings of the game
     */
    public Settings getSettings(){
        return settings;
    }

    /**
     * Gets Panel from GUI <br>
     * @return GPanel which is drawn on
     */
    public GPanel getGameBoardGPanel(){
        return this.gui.getPanel();
    }

    /**
     * Returns ID of active Player <br>
     * @return active Player ID
     */
    public int getActivePlayerID(){
        return this.activePlayerID;
    }



    /**
     * Returns Board of active Player <br>
     * @return active Board
     */
    public BasicBoard getActiveGameBoard(){
        return this.activeGameBoard;
    }

    /**
     * Returns Number of Players <br>
     * @return Number of Players
     */
    public int getPlayerCount()
    {
        return this.playerCount;
    }


    /**
     * Return Last placed Move <br>
     * @return last placed Move
     */
    public TilePlacement getLastPlacedMove()
    {
        return this.lastPlacedMove;
    }

    /**
     * Returns last placed Tile <br>
     * @return last placed Tile
     */
    public BasicTile getLastPlacedTile(){
        return this.lastPlacedTile;
    }


    /**
     * Returns if the active Player is HUMAN (Used in GUI) <br>
     * @return true if active Player is HUMAN
     */
    public boolean isActivePlayerHuman(){
        return this.activePlayerHuman;
    }

    /**
     * 
     *
     * This function is used to find a virtual Boardsize which could encompasses all legaly reachable <br>
     * positions. This rectancluar board is used to create indexes as keys for BasicBoards <br>
     * hashTable of all placed tiles. <br>
     *
     * @return the number of columns
     *
     */
    private int findBoardSizeX(){
        int rightBorder = 0;

        // Iterates through the pre placed tiles and finds the maximum column values
        for (TilePlacement preplacedPosition : this.preplacedPositions) {
            rightBorder = Math.max(preplacedPosition.getColumn(),rightBorder);
        }

        // Addiert die Zahl der Tiles da diese im Worstcase (nur ein Spieler kann gültige Züge machen)
        // in gerader Linie gelegt die maximale Ausbreitung des Spielfeldes definieren.
        int tiles = this.config.getNumTiles();
        return rightBorder + tiles + Math.abs(findBoardOffsetX());
    }



    /**
     * 
     *
     * This function is used to find a virtual Boardsize which could encompasses all legaly reachable <br>
     * positions. This rectancluar board is used to create indexes as keys for BasicBoards <br>
     * hashTable of all placed tiles.
     *
     * @return the number of rows
     */
    private int findBoardSizeY(){
        int lowerBorder = 0;

        // Iterates through the pre placed tiles and finds the maximum column values
        for (TilePlacement preplacedPosition : this.preplacedPositions) {
            lowerBorder = Math.max(preplacedPosition.getRow(),lowerBorder);
        }

        // Addiert die Zahl der Tiles da diese im Worstcase (nur ein Spieler kann gültige Züge machen)
        // in gerader Linie gelegt die maximale Ausbreitung des Spielfeldes definieren.
        int tiles = this.config.getNumTiles();
        return lowerBorder + tiles + Math.abs(findBoardOffsetY());
    }


    /**
     * 
     *
     * This function is used to find an offset for the virtual Boardsize which could encompasses all legaly reachable <br>
     * positions with non-negative row/ column coordinates. Its used in the virtual rectancluar board to create indexes as keys for BasicBoards <br>
     * hashTable of all placed tiles. <br>
     *
     * @return the column offset
     */
    private int findBoardOffsetX(){
        int leftBorder = 0;

        // Durchläuft die vorplatzierten Tiles und ermittelt die Minimalwerte der X Koordinaten.
        for (TilePlacement preplacedPosition : this.preplacedPositions) {
            leftBorder = Math.min(preplacedPosition.getColumn(),leftBorder);
        }

        // Addiert die Zahl der Tiles da diese im Worstcase (nur ein Spieler kann gültige Züge machen)
        // in gerader Linie gelegt die maximale Ausbreitung des Spielfeldes definieren.
        int tiles = this.config.getNumTiles();
        return leftBorder - tiles;
    }

    /**
     * 
     *
     * This function is used to find an offset for the virtual Boardsize which could encompasses all legaly reachable <br>
     * positions with non-negative row/ column coordinates. Its used in the virtual rectancluar board to create indexes as keys for BasicBoards <br>
     * hashTable of all placed tiles. <br>
     *
     * @return the row offset
     */
    private int findBoardOffsetY(){
        int upperBorder = 0;

        // Durchläuft die vorplatzierten Tiles und ermittelt die Minimalwerte der X Koordinaten.
        for (TilePlacement preplacedPosition : this.preplacedPositions) {
            upperBorder = Math.min(preplacedPosition.getColumn(),upperBorder);
        }

        // Addiert die Zahl der Tiles da diese im Worstcase (nur ein Spieler kann gültige Züge machen)
        // in gerader Linie gelegt die maximale Ausbreitung des Spielfeldes definieren.
        int tiles = this.config.getNumTiles();
        return upperBorder - tiles;
    }

    /**
     * Get the OffSetX value for BasicBoard Indexes <br>
     * @return controls calcultated column offset
     */
    public int getOffSetX(){
        return offSetX;
    }

    /**
     * Get the OffSetY value for BasicBoard Indexes <br>
     * @return controls calcultated row offset
     */
    public int getOffSetY(){
        return offSetY;
    }

    /**
     * Returns an Integer list with the scores calculate by the controlPlayer instance <br>
     * @return Integer Array containing the scores
     */
    public int[] getScoreList()
    {
        int[] t = this.controlPlayer.getScoreBoard();
        return t;
    }

    public BasicGui getGui() {
        return null;
    }

}
