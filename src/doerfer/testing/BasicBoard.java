package doerfer.testing;

import doerfer.preset.*;

import java.util.*;


/**
 * Stores the game board's information about placed Tiles.<br>
 * Contains methods to validate TilePlacements and calculate possible locations
 * for the placement of the next Tile.<br>
 *<br> 
 *
 * Every Player instance has its own BasicBoard
 *
 */
public class BasicBoard{

    /** Stores the current width of the board.**/
    private int width;
    /** Stores the current height of the board**/
    private int height;

    /** Stores the offset for columns calculated by control**/
    private int offSetX;

    /** Stores the offset for rows calculated by control **/
    private int offSetY;


    /** Stores the already placed Tiles of the game<br>
     * 2 dimensional coordinates are represented as index used as key in the TreeMap<br>
     *<br>
     * index = (row - offSetX) * height + (column + offSetY)<br>
     *<br>
     * offSetX, offSetY, width, height are calculated by doerfer.testing.Control
     * to determine a finite game board size. This is a representation of a rectangle
     * game board reduced to the size of legally reachable positions.
     *
     * **/
    private TreeMap<Integer,BasicTile> tilesTree;


    /** Stores the uncovered but not yet placed tiles of a game.<br>
     * New cards are put in the 0 position. So the last element is
     * the card which can be placed in the current turn
     * **/
    private Vector<BasicTile> openCards;


    /** Reference of the games central control instance**/
    private BasicControl control;

    /**
     * Is used to lock tileplacements via the GUI when a
     * new Tile is placed by a human but its rotation not yet confirmed
     * by finalize rotation.
     */
    private boolean activeInsertion;

    /** Reference of the central BasicGui of the application**/
    private BasicGui userInterface;

    /** Is set false when the calculation of possible moves does not
     * find a possible placement for the current tile **/
    private boolean turnCanBeMade;

    /** Is set false if the game is no longer running **/
    private boolean running;

    /** reference to the games config*/
    private BasicConf config;

    /**
     * Stores the number of rotations during the current placing process.<br>
     * (From the start of the human placement procedure until the confirmation
     * of the final rotation).
     */
    private int rotations;

    /**
     * Stores the reference of the human owner.<br>
     * (If the owner is actually human)
     */
    private BasicHumanPlayer boardOwner;

    /**
     * Stores the indexes of the board owners own
     * tiles. Used for finding possible positions.
     */
    private ArrayList<Integer> indexOfOwnTiles;

    /**
     * Stores the Player ID of the board owner
     */
    private int ownerID;

    /**
     * Stores the column of a tile which is placed to the gui
     * but is not yet finally confirmed.<br>
     * Is switched if a human player decides to place elsewhere)
     */
    private int candidateX;

    /**
     * Stores the row of a tile which is placed to the gui
     * but is not yet finally confirmed.<br>
     * Is switched if a human player decides to place elsewhere)
     */
    private int canditateY;

    /**
     * Is true if the boardOwner is human and
     * false otherwise.<br><br>
     *
     * (Is used to determine what to do when no possible fields
     * could be found)<br>
     */
    private boolean humanBoard;

    /**
     * Stores a reference of the games Settings
     */
    private Settings settings;

    /**
     *
     * Constructor of a BasicBoard Object<br>
     *<br>
     * is called by BasicControl
     *
     * @param w width of the game board's internal rectangular representation
     * @param h height of the game board's internal rectangular representation
     * @param osx the offset of columns for the index creation
     * @param osy the offset of rows for the index creation
     * @param ui reference to the games central GUI
     * @param cont reference of the central Control Instance
     * @param bo the Player Object who owns this board
     * @param settings1 the settings of the game
     *
     */
    public BasicBoard(int w, int h, int osx, int osy, BasicGui ui, BasicControl cont,  Player bo, Settings settings1){
        width = w;
        height = h;
        control = cont;
        boardOwner = (BasicHumanPlayer) bo;
        userInterface = ui;
        offSetX = osx;
        offSetY = osy;
        // as the vectors positions contains the allready placed tiles
        openCards = new Vector<BasicTile>();
        activeInsertion = false;
        running = true;
        settings = settings1;
        config = new BasicConf(settings);
        turnCanBeMade = false;
        indexOfOwnTiles = new ArrayList<Integer>();
        // is set negative for AI players in their init call
        humanBoard = true;
        tilesTree = new TreeMap<Integer,BasicTile>();
        
    }

    /**
     * Is used by the Player to inform the board about a new uncoverd tile
     * @param newTile The new uncovered tile
     */
    public void addNextOpenTile(BasicTile newTile){
        this.openCards.add(0,newTile);
    }

    /**
     * Sets the owners id
     * @param id Player ID
     */
    public void setBoardID(int id){
        this.ownerID = id;
    }

    /**
     * Debugging Function
     * @param pos position in Fortlaufender Zaehlung
     */
    public void showTileInfo(int pos){
        BasicTile t = this.tilesTree.get(pos);
        
    }

    /**
     * Returns the oldest open card -<br>
     * which is the one the current turn could place
     *
     * @return opencards last element.
     */
    public BasicTile getLastHandCard (){
        return openCards.lastElement();
    }

    /**
     * Is used by the GUI's mouse listener to determine if a tile is placed
     * but not finally rotated confirmed. Is used for human players
     * @return  true - if the current placement is startet by a first but not finalized by a second click.
     * false otherwise
     */
    public boolean isActiveInsertion(){
        return activeInsertion;
    }

    /**
     * Is called by the gui to inform that a rotation is confirmed by a second click.<br>
     * Checks if the placement would be legal. Shows an error if not.<br>
     * If its legal it initiates the end of the human turn by calling finishHumanTurn().
     */
    public void finalizeRotation(){
        BasicTile candidate = this.openCards.lastElement(); // the tile which is to be placed
        candidate = candidate.getRotated(this.rotations); // returns a tile with the players choosed rotation

        if(this.isValidMove(candidate,this.candidateX,this.canditateY)) {
            this.activeInsertion = false;
            this.finishHumanTurn(); // finish up the ongoing human turn
        }
        else
        {
            this.userInterface.setInfoWrongRotation(); // to show a info about a illegal rotation
        }
    }

    /**
     * Is used in the AIs init call to inform the BasicBoard
     * it belongs to an AI
     */
    public void setBoardforAI()
    {
        this.humanBoard = false;
    }


    /**
     * Ends the ongoing round and informs control that the turn is finished
     *
     */
    public void finishHumanTurn(){
        TilePlacement thisMove = new TilePlacement(this.canditateY,this.candidateX,this.rotations);
        this.rotations = 0;
        this.boardOwner.humanDidMove(thisMove);
        this.control.turnOver();
    }

    /**
     * Is used by control to inform itself if there was a possible
     * postion for the currently active player<br>
     * @return true if a possible position was found and false otherwise.
     */
    public boolean couldTurnBeMade() {
        return this.turnCanBeMade;
    }

    /**
     * Wird von BasicControl aufgerufen um sich zu informieren
     * ob noch Karten zum legen uebrig sind.<br>
     * @return Gibt false zurueck wenn der Stapel an offenen Karten leer ist.
     */
    public boolean trashNoMoreCards(){
        return this.openCards.isEmpty();
    }

    /**
     * Informiert darueber ob BasicBoard davon ausgeht das die Partie noch laeuft
     * @return ist true wenn das Spiel noch nicht beeendet ist.
     */
    public boolean isRunning(){
        return this.running;
    }



    /**
     *
     * Finds positions suitable for the active players turn.<br>
     * Sets turnCanBeMade to true if there is at least one possible turn.<br>
     * Sets turnCanBeMade to false if there is no possible turn.<br>
     *
     */
   public void markPossibleFields(){
        this.turnCanBeMade = false; // stays false if no position could be found
        Set<Integer> validPositions = new HashSet<Integer>();
        int id = this.control.getActivePlayerID();
        BasicTile tileTemp = null;
        int indexOfN = -1;
        int t = -1;
        for(int i = 0; i < this.indexOfOwnTiles.size(); i++) { // iterates through the index of own tiles
                    for (int j = 0; j < 6; j++) { // checks to every direction for neighbours
                        indexOfN = this.adjacentTile(this.indexOfOwnTiles.get(i),j);
                        if(indexOfN > 0) { // would be -1 for a non existing neighbour
                            if (!this.tilesTree.containsKey(indexOfN)) { // the position must be unoccupied to allow for a turn
                                if(this.openCards.size() > 0){ // if there are no more cards to place no position could be found
                                    if (this.isValidPosition(this.openCards.get(openCards.size()-1), indexOfN)) {
                                        if (indexOfN > 0) // index ist minus bei Spielbrettueberschreitung
                                        {
                                            this.turnCanBeMade = true; // is set true because one valid position is found
                                            if(!validPositions.contains(indexOfN))
                                            {
                                                validPositions.add(indexOfN); // if not already in it the position is added to indexOf
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
        }
        if((this.turnCanBeMade != true) && this.humanBoard) // if no turn could be made and the board is human
        {
            this.boardOwner.humanDidMove(null); // the board owner is informed that move can be made.
        }
        Set<TilePlacement> validPlacers = new HashSet<TilePlacement>();
        for (Integer position:validPositions) // goes through the list of possible positions and creates TilePlacements
        {
            validPlacers.add(new TilePlacement(this.indexToY(position),this.indexToX(position),1));
        }
        this.userInterface.setValidTilePlacements(validPlacers); // send them to the gui to be drawn.
    }

    /**
     * Creates a list of possible placing positions
     *<br>
     * @return list of possible positions to play the tile
     */
     public ArrayList<Integer> getPossibleFields() {
     this.turnCanBeMade = false;
     BasicTile tileTemp = null;
     int indexOfN = -1;
     ArrayList <Integer> possibilities = new ArrayList<Integer>();
     for (int i = 0; i < this.indexOfOwnTiles.size();i++) { // iterates through the index of own tiles
               for (int j = 0; j<6; j++){ // checks to every direction for neighbours
                   indexOfN = this.adjacentTile(indexOfOwnTiles.get(i),j);
                   if(!this.tilesTree.containsKey(indexOfN)){ // the position must be unoccupied to allow for a turn
                       if(this.isValidPosition(this.openCards.lastElement(),indexOfN)){
                           possibilities.add(indexOfN);
                           this.turnCanBeMade = true; // is set true because one valid position is found
                       }
                   }
               }
           }
      return possibilities;
      }




    /**
     * Rotates the last tile placed to the gui
     * used by the guis mouselistener
     *<br>
     * @param i the steps of a clockwise rotation.
     * @throws Exception from drawTiles operations
     */
    public void rotateActiveHex(int i) throws Exception{
        this.rotations += i;
        this.rotations = this.rotations % 6;
        BasicTile t = this.openCards.lastElement().getRotated(this.rotations);
        int x = this.candidateX;
        int y = this.canditateY;
        this.userInterface.cleanLastPaintedTile();
        this.userInterface.drawTile(x,y,t,this.control.getActivePlayerID());
        this.userInterface.repaint();
    }




    /**
     *
     * Places the current tile to play on the GUI.
     * Checks if the position is empty and a placement in
     * at least one rotation would be correct.
     *<br>
     * Is only used by Humanplayers
     *<br>
     *
     * @param position   index value of the tilesTree
     * @throws Exception from drawTile operations
     */
    public void insertNext(int position) throws Exception {

        BasicTile candidate = this.openCards.lastElement();
        boolean valid = this.isValidPosition(candidate,position);
        valid = valid && (!this.tilesTree.containsKey(position));
        if (valid) {
            int col = this.indexToX(position);
            int row = this.indexToY(position);
            this.candidateX = col; // Fuer die Drehung
            this.canditateY = row; // Fuer die Drehung
            TilePlacement t = new TilePlacement(row,col,0);
            this.userInterface.drawTileToGui( t, this.openCards.lastElement(), this.control.getActivePlayerID());
            this.activeInsertion = true;
            this.userInterface.repaint();
        }
    }




    /** Returns the Biome of a specific neighbour edge.
    <br>
    @param index Index of the actual tile.
    @param d Direction of the neighbouring edge.<br>
                 0<br>
               5   1<br>
               4   2<br>
                3<br>
     @return the biome of the neighbouring edge or null if there is no neighbouring edge
    **/
    public Biome getNeighbourEdge(int index, int d){
        //int index = x * this.height + y;
        int indexOfN = this.adjacentTile(index,d);
        BasicTile t = null;
        if(indexOfN > 0)
            t = this.tilesTree.get(indexOfN);
        if(t != null)
            return t.getEdge((d + 3) % 6);
        else
            return null;
    }

    /**
     * Is used to ask for the owner of a placed tile
     * @param index index value in the tilesTree
     * @param d direction of the neighbour in question
     * @return the Player ID of the neighbour tiles owner or -1 if there is no tile.
     */
    public int getOwnerOfNeighbour(int index, int d){
        //int index = x * this.height + y;
        BasicTile t = null;
        int indexOfN = this.adjacentTile(index,d);
        if(indexOfN > 0)
            t = this.tilesTree.get(indexOfN);
        if(t != null)
            return t.getOwnerID();
        else
            return -1;
    }


    /**
     * Is used to ask for the owner of a placed tile
     * @param x Column of the actual tile
     * @param y Column of the actual tile
     * @param d direction of the neighbour in question
     * @return the Player ID of the neighbour tiles owner or -1 if there is no tile.
     */
    public int getOwnerOfNeighbour(int x, int y, int d){
        int index = this.xyToIndex(x,y);
        return getOwnerOfNeighbour(index, d);

    }


    /**
     * Is used to find calculate a neighbouring tile's index in the tilesTree<br>
     * @param x Column of the actual tile
     * @param y Column of the actual tile
     * @param direction direction of the neighbour in question
     * @return index of an adjacent hexfield in the tiles vector, -1 if the hex is out of the index range (see tilesTree)
     *
     **/
    public int adjacentTile(int x, int y, int direction){
        int index = this.xyToIndex(x,y);
        return adjacentTile(index, direction);
    }




    /**
     * Is used to find calculate a neighbouring tile's index in the tilesTree
     * @param index Index of the actual tile in the tilesTree
     * @param direction direction of the neighbour in question
     * @return index of an adjacent hexfield in the tiles vector, -1 if the hex is out of the index range (see tilesTree)
     */
    public int adjacentTile(int index, int direction){
        int indexAT = 0;
        int y = this.indexToY(index);
        int x = this.indexToX(index);

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
     * Used to place a Tile on the gameboard.<br>
     *<br>
     * Inserts a tile in the tilesTree at the specified Position<br>
     * index = (row - offSetX) * height + (column + offSetY)<br>
     *<br>
     * If Tile and owner have the same ID it is also stored in the indexOfOwnTiles<br>
     *<br>
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @param t the Tile to be placed
     * @param id the Player ID of the player to whom the tile belongs
     * @throws RuntimeException if the position is already occupied
     */
    public void insertTile(int x, int y, BasicTile t, int id) throws RuntimeException{
        int index = this.xyToIndex(x,y);
        t.setOwnerID(id);
        if(this.ownerID==id){ // if the tile belongs to the owner to owner of the board
            this.indexOfOwnTiles.add(index); // its added to the list of its own tiles
        }
        if(this.tilesTree.containsKey(index))
        {
            throw new RuntimeException("versucht einen tile auf einer bereits besetzten Position hinzuzufügen!");
        }
        else {
            this.tilesTree.putIfAbsent(index, t); // inserts the tile into the boards tilesTree
            if (this.openCards.size() > 0) {
                this.openCards.remove(this.openCards.lastElement()); // after a turn is made the last element is removed
            }
        }
    }


    /**
     * Used to place a Tile on the game-board.<br><br>
     *
     * Inserts a tile in the tilesTree at the specified Position<br>
     * index = (row - offSetX) * height + (column + offSetY)<br>
     *<br>
     * If Tile and owner have the same ID it is also stored in the indexOfOwnTiles<br>
     *
     * @param p TilePlacer with the insertions postion and rotation
     * @param t the Tile to be placed
     * @param id the Player ID of the player to whom the tile belongs
     */
    public void insertTile(TilePlacement p, BasicTile t, int id){
        int x = p.getColumn();
        int y = p.getRow();
        int r = p.getRotation();
        BasicTile temp = t.getRotated(r);
        insertTile(x,y,temp,id);
    }


    /**
     * Returns the Player ID of a placed tile
     * which is stored in the tilesTree
     * @param index position of the tile in the tilesTree
     * @return Player ID of the tile owner, -1 if the position is empty
     */
    public int getTileOwner(int index){
        if(this.tilesTree.get(index) != null){
            return this.tilesTree.get(index).getOwnerID();
        }
        else
            return -1;
    }

    /**
     * Returns the Player ID of a placed tile
     * which is stored in the tilesTree
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @return Player ID of the tile owner, -1 if the position is empty
     */
    public int getTileOwner(int x, int y){
        int index = xyToIndex(x,y);
        return getTileOwner(index);
    }



    /**
     * Checks if the placement of a tile would be a legal move.
     * @param tp TilePlacer containing the information of the insertion attempt.
     * @param t The Tile which is to be placed with tp.
     * @return returns true is the move would be legal, false if not.
     */
    boolean isValidMove(TilePlacement tp, BasicTile t){
        int x = tp.getColumn();
        int y = tp.getRow();
        int r = tp.getRotation();
        BasicTile temp = t.getRotated(r);
        return this.isValidMove(temp,x,y);
    }


    /**
     * Checks if the placement of a tile (in its current rotation) would be a legal move.
     * @param t The Tile which is to be placed.
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @return returns true is the move would be legal, false if not.
     */
    boolean isValidMove(BasicTile t, int x, int y){
        int index = this.xyToIndex(x,y);
        return this.isValidMove(t, index);
    }

    /**
     * Checks if the placement of a tile (in its current rotation) would be a legal move.
     * @param t The Tile which is to be placed.
     * @param index Index in the tileTree logic where the tile should be placed
     * @return returns true is the move would be legal, false if not.
     */
    public boolean isValidMove(BasicTile t, int index){
        boolean nextToOwnTile = false;   // is set true if it finds at least one neighbour with the same id
        boolean allContactsLegal = true; // is set false if it finds at least one illegal contact

        for(int i = 0; i < 6; i++){
            Biome b = this.getNeighbourEdge(index,i);
            if(this.control.getActivePlayerID() == this.getOwnerOfNeighbour(index,i))
            {
                nextToOwnTile = true;
            }
            if(b == Biome.WATER){
                if(t.getEdge(i) != Biome.WATER){
                    allContactsLegal = false;}}
            if(b == Biome.TRAINTRACKS){
                if(t.getEdge(i) != Biome.TRAINTRACKS){
                    allContactsLegal = false;}}
            if(t.getEdge(i) == Biome.WATER){
                if((b != Biome.WATER) && (b != null)){
                    allContactsLegal = false;}}
            if(t.getEdge(i) == Biome.TRAINTRACKS){
                if((b != Biome.TRAINTRACKS) && (b != null)){
                    allContactsLegal = false;}}
        }
        return (nextToOwnTile && allContactsLegal); // a move may be made if it has legal contacts and a neighbour with the same id
    }


    /**
     * Checks if a specific Tile can be placed to a certain position in at least one rotation.
     * @param t The tile to be placed.
     * @param index Index in the tileTree logic where the tile should be placed
     * @return true - if there is at least one rotation which makes the move legal. false - if not.
     */
    public boolean isValidPosition(BasicTile t, int index){

        for(int i = 0; i < 6; i++){
            if(this.isValidMove(t.getRotated(i),index))
                return true;
        }
        return false;
    }

    /**
     * Checks if a specific Tile can be placed to a certain position in at least one rotation.
     * @param t The tile to be placed.
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @return true - if there is at least one rotation which makes the move legal. false - if not.
     */
    public boolean isValidPosition(BasicTile t, int x, int y){
        int index = this.xyToIndex(x,y);
        return isValidPosition(t, index);
    }


    /**
     * Returns the last element of the openCards list.
     * This is the Tile which would be placed in the current turn.
     * @return the tile of the current turn.
     */
    public BasicTile currentCard()
    {
        return this.openCards.lastElement();
    }


    /**
     * To be used during a humans turn.
     * returns a TilePlacement with the information of a tile
     * placed to the gui by the human player which is not finaly confirmed.
     * @return The TilePlacing descriping how the current Tile is about to be placed.
     */
    public TilePlacement getPlacementOfTurn() {
        int r = this.rotations;
        int x = this.candidateX;
        int y = this.canditateY;
        return new TilePlacement(y,x,r);
    }

    /**
     * Calculates a game boards column value from an index in tileTree logic
     * @param index index in tileTree logic
     * @return column value
     */
    public int indexToX(int index){
        return (index / this.height) + this.offSetX;
    }

    /**
     * Calculates a game boards row value from an index in tileTree logic
     * @param index index in tileTree logic
     * @return row value
     */
    public int indexToY(int index){
        return (index % this.height) + this.offSetY;
    }

    /**
     * Calculates an index in tileTree logic from row and column values
     * @param x Column of the game-board
     * @param y Row of the game-board
     * @return column value
     */
    private int xyToIndex(int x, int y)
    {
        return (((x - this.offSetX) * this.height) + (y - this.offSetY));
    }

}
