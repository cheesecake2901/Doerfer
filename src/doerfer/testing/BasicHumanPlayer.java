package doerfer.testing;

import doerfer.preset.*;
import doerfer.testing.Points.*;
import java.util.*;

/** Class for a human player, chooses his/her own cards to place more or less competent
 */

public class BasicHumanPlayer implements Player {
    
    /** A reference to the used config*/
    private BasicConf config;
    /** A reference to the used userInterface*/
    private BasicGui userInterface;
    /** The ID the player holds*/
    private int id;
    /** The score the player has*/
    private int score;
    /** The name of the player*/
    private String name;
    /** The TilePlacement the Player gets throug notifyTilePlacement*/
    private TilePlacement placing;
    /** A reference to the control */
    private BasicControl control;
    /** Stores the turns made, used to check if it is indeed the players turn */
    private int turns;
    /** The random seed of the player */
	private RNG seed;
    /** A List of all cards placed during the game used for verifying the game */
    private List<BasicTile> allTiles;
    /** The TilePlacement this player chose to put on the board */
    private TilePlacement lastOwnMove;
    /** A HashMap storing all openEdges */
    private HashMap <Triple,Points> openEdges = new HashMap <Triple,Points>();
    /** The private scoreboard of this player */
    private int [] scoreboard = new int [4];
    /** A reference to the settings of the game */
    private Settings settings;
    /** A boolean to check if this player has been notified of a new uncovered tile */
    private boolean notified;
    /** A boolean to add preplaced cards to the board */
    private boolean prePlacing;
    /** A boolean to check if the randomSeed only get's asked for once*/
    private boolean requested;
    /** Das Board der Playerinstanz */
    private BasicBoard gameBoard;
    /** A reference to the type the player has */
    PlayerType type;

    /** 
     * Constructs a new Human_Player
     * @param w the width of the board
     * @param h the height of the board
     * @param ui    the ui to be used
     * @param cont  the controlreference
     * @param n     the name of the player
     * @param s     the settings to be used
     */


    public BasicHumanPlayer(int w, int h, BasicGui ui, BasicControl cont, String n, Settings s){
        type = PlayerType.HUMAN;
        settings = s;
        config = null;
        id= -1;
        score = 0;
        turns = 0;
        userInterface = ui;
        name = n;
        placing = null;
		control = cont;
		seed = new RNG();
        allTiles = new ArrayList<>();

		try{
			gameBoard = new BasicBoard(w,h,this.control.getOffSetX(),this.control.getOffSetY(),ui,cont,this,settings);
		}catch (Exception e){
			throw new RuntimeException(e);
		}

    }
    
    /** 
     * Initiates the game with the gameConfiguration and playerid. <br>
     * Throws a exception if tried to be initiated twice.
     * @param gameConfiguration the gameConfiguration which is used for the game
     * @param playerid  the ID the player gets
     * @exception Exception if initiated twice
     */
    @Override
    public void init(GameConfiguration gameConfiguration, int playerid) throws Exception{
        if (config == null){
            config = (BasicConf) gameConfiguration;
            id = playerid;
            this.gameBoard.setBoardID(id);
            prePlacing = true;
            try {
                placePreplacedTiles();
            }
            catch(Exception e) {                                        // Das wird vom Starter schon abgefangen
                throw new Exception("Defective GameConfiguration");
            }
        }
        else{
            throw new Exception ("Initiated twice");
        }
    }
    
    /**
     * Reads in the preplaced Tiles and puts them onto the players map. <br>
     * Also invokes the readInNewEdges method to read in the open edges of the preplaced tiles.
     * @throws Exception If no preplaced tiles are present or in wrong format
     */
     
    public void placePreplacedTiles() throws Exception {
        for(int i = 0; i< this.config.getPreplacedTiles().size();i++){
            BasicTile tileToPlace = (BasicTile) this.config.getPreplacedTiles().get(i);
            this.gameBoard.insertTile(this.config.getPreplacedTilesPlacements().get(i),tileToPlace,this.config.getPreplacedTilesPlayerIDs().get(i));
            readInNewEdges(this.config.getPreplacedTilesPlacements().get(i),tileToPlace);
        }
    }
    
    /** Returns the players name
     * @return the name of this player
     * @throws NullPointerException if this players name has not been set before
     */
    @Override
    public String getName() throws Exception{
        return this.name;
    }

    /** 
     * Returns the chosen TilePlacement of this player <br>
     *  Checks if it's the players turn and if it's not called twice i.e. the game has incorrectly skipped a player <br>
     * @return this players TilePlacement
     * @throws Exception Requested two times if this player has been requested to place two times and it's not this players turn
     */
    @Override
    public TilePlacement requestTilePlacement() throws Exception{
        this.turns++;
        prePlacing = false;
        if(this.id != this.control.getActivePlayerID() && (this.turns%4 != this.id)) {
            throw new Exception ("Requested two times");
        }
        notified = false;
        return this.lastOwnMove;
    }

     /** 
      * Notifies this player that a tile has been placed on another players board.<br>
      * Tries to place the card as a TilePlacement on this players board and proves if the card is placed correctly otherwise throws an exception <br>
      * Also reads in the newly added edges of the tile into the openEdges HashMap <br>
      * Can only be called when it's not this players turn, otherwise it throws an exception <br>
      * @param tp the card another player has placed on his/her board
      * @throws Exception if this player has been notified of a placed card while it's his/her turn or the placement of the card could not be verified on this players board
      */
    @Override
    public void notifyTilePlacement(TilePlacement tp) throws Exception{
        prePlacing = false;
        if(notified == true){                                           // wenn geskipped wurde, steht notified auf true aber züge könnten nicht mehr stimmen
            while(this.turns%this.control.getActivePlayerID() !=0){
                turns++;
            }
        }
        if(this.turns%this.control.getActivePlayerID() !=0 &&this.control.getActivePlayerID()==this.id && notified == false){            // Darf nicht aufgerufen werden wenn Spieler selbst am Zug ist, und wenn vorher keine neue Karte aufgedeckt wurde
            throw new Exception ("That should not have happened!");
        }
        notified = false;

        this.placing = tp;
		if(tp != null){
				if(this.gameBoard.isValidMove(tp,this.gameBoard.currentCard())){
                    readInNewEdges(this.placing,this.gameBoard.currentCard());

					this.gameBoard.insertTile(this.placing, this.gameBoard.currentCard(), this.control.getActivePlayerID());
				}else{
                    throw new Exception("Es wurde gecheatet");
				}
				
			}
		}
        
    /** Returns this players scoreboard
     * @return this players scoreboard
     */ 
    
    public int [] getScoreBoard(){
        return this.scoreboard;
    }

    /**  
     * Sets the lastOwnMove TilePlacement of this player to his/her chosen placement
     * @param tp the TilePlacement this player chose
     */

    public void humanDidMove(TilePlacement tp){
        this.lastOwnMove = tp;

        if(tp != null){
            try {
            readInNewEdges(tp,this.gameBoard.currentCard());
            this.gameBoard.insertTile(tp, this.gameBoard.currentCard(), this.id);
            }
            catch (Exception e) {
            throw new RuntimeException(e);
            }
        }
    }
    
    /** Notifies this player that a new card has been uncovered from the deck <br>
     * Adds it to the stack of cards this player holds for verifying the game and adds it to the boards open cards
     * @param tile the tile representing the new card that has been uncovered
     * @throws Exception if the tile is null
     */
    @Override
    public void notifyNewUncoveredTile(Tile tile) throws Exception{
        notified = true;
		BasicTile tmp = (BasicTile) tile;
        allTiles.add(tmp);

		this.gameBoard.addNextOpenTile(tmp);
    }
    
    /** Returns this players score
     * @return this players score
     */
    @Override
    public int getScore() throws Exception{
        return this.score;
    }
    
    /** Sets this players score
     * @param s the score to be added to this players score
     */
    
    public void setScore(int s){
        this.score +=s;
     }
     
     /** Returns the gameBoard of this player
      * @return this players gameBoard
      */
      
     public BasicBoard getGameBoard() {
         return this.gameBoard;
     }
     
    /** Returns the next random number
     * @return the next random number
     * @throws Exception if this player has not been notified of a new uncovered tile from the deck
     */
    @Override
    public long requestNextRandomNumber() throws Exception{
        if(this.turns != 0 && notified == true){
            throw new Exception ("That should not have happend");
        }
		return seed.next();
    }
    
    /** Returns this players random number seed
     * @return this players random number seed
     * @throws Exception if this player has already been asked for his random number seed
     */
    @Override
    public long requestRandomNumberSeed() throws Exception{
        if(requested == true){
            throw new Exception ("That should not have happend");
        }
        requested = true;
        return seed.getSeed();
    }
    
    /**
     * Verifies the Game<br>
     * This player gets a list of all players random number seeds and a list of all players scores<br>
     * To verify the correct order of used cards this players uses his/her own list of uncovered cards during the game and generates a list of cards starting with the seeds of all players to check if they are equal <br>
     * To verify the score this player uses his own scoreboard to check if the list of players scores are equal to his/her own list
     * @param seeds a list of all the players random number seeds
     * @param scores a list of all the players scores
     * @throws Exception if either the list of seeds or the list of scores could not be verified
     */
    @Override
    public void verifyGame(List<Long> seeds, List<Integer> scores) throws Exception{
	//Erhält speichert seed als RNG
        List<RNG> RNGList= new ArrayList<>();
        for(long s : seeds){
            RNGList.add(new RNG(s));
        }
	//prüft ob die Seeds übereinstimmen
        if( seeds.get(id-1) == seed.getSeed()){
                for (BasicTile t : allTiles) {
                    List<Long> random = new ArrayList<Long>();
                    long newNumber;
                    TileGenerator generator = new TileGenerator(this.config);
                    for (int j = 0; j < seeds.size(); j++) {
                        random.add(RNGList.get(j).next()); // generiert für jeden seed Zufallszahlen
                    }
                    newNumber = random.get(0);
                    for (int z = 1; z < random.size(); z++) {

                    newNumber = newNumber ^ random.get(z); //berechnet neue Nummer durch alle generierten Zufallszahlen
                    }

                    List<Biome> biomesL = generator.generateTile(newNumber); // erzeugt neues Biom aus Zufallszahl
                    BasicTile tempTile = new BasicTile(biomesL);
                    if(!tempTile.toString().equals(t.toString())){ //Falls eigene gespeicherte Tile nicht mit dem neu generierten Tile übereinstimmt, hat einer gecheated
                        throw new Exception("Cheated");
                    }

                }
        }
        else{
            throw new Exception("Cheated");
        }
	//Falls scores nicht übereinstimmen, hat ein Spieler auch gecheated
        for(int k = 0; k<scores.size();k++){
            if(scores.get(k) != this.scoreboard[k]){
                    throw new Exception("Cheated");
            }
        }
    }
    
    /**
     * Reads in newly added edges into this players openEdges HashMap<br>
     * It creates a new HashMap consisting of the connected biomes on the new tile represented as a Points object as values and its open edges as keys <br>
     * For each key it checks wether or not the opposite open edge exists in the openEdges HashMap and trys to merge them <br>
     * If no open edge exists the new edge is stored into the openEdges HashMap <br>
     * If the merge is succesfull the value of the newly connected biome overrides the value in the openEdges HashMap so creating a new connected biome <br>
     * Wether or not the merge was succesfull, the edge is closed and the values of both keys are added into a list for further processing in the checkForPoints method
     * @see doerfer.testing.Points methods to check wether a biome could be connected or merged
     * @param tp    the TilePlacement informations of the new card to be added
     * @param t     the BasicTile informations of the new card to be added
     */
    
    public void readInNewEdges(TilePlacement tp, BasicTile t) {

        HashMap <Triple,Points> edgesToCheck;           
        int idForPoints = this.control.getActivePlayerID();
        
        if (prePlacing == true){
            edgesToCheck = Points.startPoints(tp,t,t.getOwnerID());
        }

        else{
        edgesToCheck = Points.startPoints(tp,this.gameBoard.currentCard(),idForPoints);
        }
        ArrayList <Points> biomeValues = new ArrayList <Points>();
            for(Triple key : edgesToCheck.keySet()){                    //key set der zurückgegeben Kanten des neuen Tiles
                Triple oppositeEdge = key.getOpposite();                // berechne die daran angrenzende OppositeKante
                Points oldConnectedBiome = openEdges.get(oppositeEdge);         // ==> das gibt das alte Points Objekt (value alt) zurück
                Points newConnectedBiome = edgesToCheck.get(key);                   // ==> das gibt das neue Points Objekt (value neu) zurück
                if (newConnectedBiome != null){                 
                    if(openEdges.containsKey(oppositeEdge)){        // wenn die opposite edge in der openEdge Liste liegt dann
                        if(Points.checkBiome(newConnectedBiome,oldConnectedBiome)){    // versuche die beiden Biome zu mergen
                            for(Triple replaceKey : openEdges.keySet()){
                                openEdges.replace(replaceKey,oldConnectedBiome,newConnectedBiome);      // aufs gleiche objekt zeigen lassen
                            }
                            if(biomeValues.contains(oldConnectedBiome)){
                                biomeValues.remove(oldConnectedBiome);
                            }

                            biomeValues.add(newConnectedBiome);  // füge das Biome der Liste der möglicherweise abgeschlossenen Gebiete hinzu
                            openEdges.remove(oppositeEdge);     // Kante aus offener Kantenliste entfernen
                        }
                        else {
                            edgesToCheck.put(key,null);        // ansonsten einfach so entfernen
                            if(!biomeValues.contains(oldConnectedBiome)){
                                biomeValues.add(oldConnectedBiome);
                            }
                            openEdges.remove(oppositeEdge);     // s.o.
                        }
                    }
                    else {
                        openEdges.put(key,edgesToCheck.get(key));
                        edgesToCheck.put(key,null);           // keine Kante entfernen aus openEdges
                    }
                }
            }
        checkForPoints(biomeValues);
	}

     /**
     * Checks the merged biomes. Takes a specified list of Point objects to check wether or not those objects contain any points
     * For each value, i.e. connected and completed biome, it checks if the openEdges HashMap contains that value, if so the biome still hasn't been completed <br>
     * If not, the specified list is checked if this value exists more than once, so that no double calculation happens and given to calculatePoints for further processing
     * @param biomeValues   the merged biomes readInNewEdges found
     */

    public void checkForPoints(ArrayList<Points> biomeValues){
            
        ArrayList <Points> alreadyChecked = new ArrayList<Points>();
            for (Points value : biomeValues){ 
                if(alreadyChecked.size()==0)  {    //noch nichts überprüft
                    if(!openEdges.containsValue(value)){  // gib Punkte
                        calculatePoints(value);
                        alreadyChecked.add(value);
                    }
                }
                else if (!alreadyChecked.contains(value)) { // Objekt noch nicht in Liste
                     if(!openEdges.containsValue(value)){  // gibt Punkte
                        calculatePoints(value);
                        alreadyChecked.add(value);
                    }
                }
            }
    }
    
    /**
     * Calculates points by using the specified completed Points object that holds information about how many inter-tile crossings the objects has <br>
     * Reads the IDs of the involved players while considering the ID of preplaced tiles<br>
     * Than calculates the points this object will give, that is: (inter-tile crossings*involved players)+floor(inter-tile crossings^1.5) <br>
     * At last adding this points to each of the involved players scoreboardentry and setting a new score of this player if he/she has been involved in this object
     * @param biomeToCheck the Points object that is completed i.e. has no more open edges in the openEdges HashMap as calculated through
     */
        
    public void calculatePoints(Points biomeToCheck){
    
    int possiblePoints = Points.getPoints(biomeToCheck);
        if(possiblePoints>0){
            ArrayList <Integer> playerList = Points.getId(biomeToCheck);
            if(playerList.contains(0)){
                playerList.remove(0);
            }
            int newScore = possiblePoints*playerList.size()+(int) Math.floor(Math.pow(possiblePoints,1.5));
                for (int j = 0; j<playerList.size();j++){;
                    this.scoreboard[playerList.get(j)-1] += newScore;
                        }
                    if(playerList.contains(this.id)){
                        this.setScore(newScore);
                        }
        }
    }  


}
