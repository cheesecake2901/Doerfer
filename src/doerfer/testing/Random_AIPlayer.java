package doerfer.testing;

import doerfer.preset.*;
import doerfer.testing.Points.*;
import java.util.*;


/** Class for a AI Player 
 * The Player chooses a completly random but valid position to place its card
 */

public class Random_AIPlayer implements Player {
    
    /** A reference to the used config*/
    private BasicConf config;
    /** A reference to the used userInterface*/
    private BasicGui userInterface;
    /** The ID the player holds*/
    private int id;
    /** The score the player has*/
    private int score;
    /** The TilePlacement the Player gets throug notifyTilePlacement*/
    private TilePlacement placing;
    /** The name of the player*/
    private String name;
    /** The random seed of the player */
    private RNG seed;
    /** The private scoreboard of this player */
    private int [] scoreboard = new int [4];
    /** Stores the turns made, used to check if it is indeed the players turn */
    private int turns;
    /** A reference to the board the player places all tiles */
    private BasicBoard gameBoard;
    /** A reference to the type the player has */
	private PlayerType type;
    /** A reference to the control */
	private BasicControl control;
    /** A HashMap storing all openEdges */
    private HashMap <Triple,Points> openEdges = new HashMap <Triple,Points>();
    /** A reference to the settings of the game */
    private Settings settings;
    /** A List of all cards placed during the game used for verifying the game */
    private List<BasicTile> hand;
    /** A boolean to check if this player has been notified of a new uncovered tile */
    private boolean notified;
    /** A boolean to add preplaced cards to the board */
    private boolean prePlacing;
    /** A boolean to check if the randomSeed only get's asked for once*/
    private boolean requested;
        

    /** Constructs a new Random_AIPlayer
     * @param w the width of the board
     * @param h the height of the board
     * @param ui    the ui to be used
     * @param cont  the controlreference
     * @param n     the name of the player
     * @param s     the settings to be used
     */
     
    public Random_AIPlayer(int w, int h,BasicGui ui, BasicControl cont, String n, Settings s) {
        type = PlayerType.RANDOM_AI;
        settings = s;
        userInterface = ui;
        turns = 0;
        id = -1;
		control = cont;                 
        score = 0;                      
        name = n;
        seed = new RNG();
        hand = new ArrayList<>();
        try {
            gameBoard = new BasicBoard(w,h,this.control.getOffSetX(),this.control.getOffSetY(),ui,control,null,settings);
        }
        catch (Exception e){
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
    public void init(GameConfiguration gameConfiguration, int playerid) throws Exception {
        if (config== null){                                            
            config = (BasicConf) gameConfiguration;
            this.id = playerid;
            this.gameBoard.setBoardID(id);
            this.gameBoard.setBoardforAI();
            prePlacing = true;
            try {
            placePreplacedTiles();
            }
            catch(Exception e) {                                       
                throw new Exception("Defective GameConfiguration");
            }
            
        }
        else {
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
	public String getName() throws Exception { 
        return name;
    }
    
    /** 
     * Returns the TilePlacement of this players turn.<br>
     * Checks if it's the players turn and if it's not called twice i.e. the game has incorrectly skipped a player <br>
     * As you are requesting a TilePlacement from an random AI you can not expect a great placement, but at least it's correct <br>
     * @return the TilePlacement this player "chose"
     * @throws Exception Requested two times if this player has been requested to place two times and it's not this players turn
     */
    @Override
	public TilePlacement requestTilePlacement() throws Exception {
        this.turns++;     
        prePlacing = false;                                             // PreplacingPhase ist abgeschlossen
        if(this.id != this.control.getActivePlayerID() && (this.turns%4 != this.id)) {
            throw new Exception ("Requested two times");
        }
        notified = false;
        ArrayList<Integer> possibilities = this.gameBoard.getPossibleFields();
        
        if (possibilities.size()==0) {
            return null;
        }
        BasicTile handCard = gameBoard.getLastHandCard();
        int choosen = possibilities.get((int) (Math.random() * (possibilities.size())));
        int rotation;
        BasicTile tempHand = handCard;
        rotation = (int) (Math.random() * 5);                           // Random rotation 
        tempHand = handCard.getRotated(rotation);                       
            if(this.gameBoard.isValidMove(tempHand,choosen)){           // checken ob das möglich ist
                    TilePlacement move = new TilePlacement(this.gameBoard.indexToY(choosen), this.gameBoard.indexToX(choosen),rotation);
                    readInNewEdges(move,tempHand);
                    this.gameBoard.insertTile(move,handCard,this.id);
                    return move;
            }
            else {                                                      // wenn nicht, suche so lange bis eine richtige Rotation gefunden worden ist; tritt erstaunlich selten auf
                for(int i = 0; i<6; i++){
                    tempHand = handCard.getRotated(i);
                    if(this.gameBoard.isValidMove(tempHand,choosen)){
                        TilePlacement move = new TilePlacement(this.gameBoard.indexToY(choosen), this.gameBoard.indexToX(choosen),i);
                        readInNewEdges(move,tempHand);
                        this.gameBoard.insertTile(move,handCard,this.id);
                        this.control.getGui().focus(move);
                    return move;
                    }
                }
            }
        return null;
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
        prePlacing = false;                                             // erste Karte wurde gelegt: Preplacing Phase ist abgeschlossen
        
        if(notified == true){                                        
            while(this.turns%this.control.getActivePlayerID() !=0){     // wenn geskipped wurde, steht notified auf true aber züge könnten nicht mehr stimmen
                turns++;
            }
        }

        if(this.turns%this.control.getActivePlayerID() !=0 &&this.control.getActivePlayerID()==this.id && notified == false){   // Darf nicht aufgerufen werden wenn Spieler selbst am Zug ist, und wenn nicht vorher eine neue Karte aufgedeckt worden ist 
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

    /** Notifies this player that a new card has been uncovered from the deck <br>
     * Adds it to the stack of cards this player holds for verifying the game and adds it to the boards open cards
     * @param tile the tile representing the new card that has been uncovered
     * @throws Exception if the tile is null
     */
    @Override
	public void notifyNewUncoveredTile(Tile tile) throws Exception {;               // wird von control nur aufgerufen so lange es noch Karten gibt die aufzudecken sind, ein Check würde sich also bei unser Konfiguration erübrigen?
        notified = true;
        BasicTile tmp = (BasicTile) tile;
        hand.add(tmp);                                              
        this.gameBoard.addNextOpenTile(tmp);
    }
    
    /** Returns this players gameboard
     * @return this players gameBoard
     */
     
    public BasicBoard getGameBoard(){
        return this.gameBoard;
    }
    
    /** Returns this players score
     * @return this players score
     */
    @Override
	public int getScore() {                     
        return this.score;
    }
    
    /** Sets this players score
     * @param s the score to be added to this players score
     */
     
    public void setScore(int s) {
        this.score +=s;
    }
    
    /** Returns the next random number
     * @return the next random number
     * @throws Exception if this player has not been notified of a new uncovered tile from the deck
     */
    @Override
	public long requestNextRandomNumber() throws Exception {
        if(this.turns != 0 &&notified == true){
            throw new Exception ("That should not have happend");
        }
        return seed.next();
    }
    
    /** Returns this players random number seed
     * @return this players random number seed
     * @throws Exception if this player has already been asked for his random number seed
     */
    @Override
	public long requestRandomNumberSeed() throws Exception {
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
	public void verifyGame(List<Long> seeds, List<Integer> scores) throws Exception {
        List<RNG> RNGList= new ArrayList<>();
        for(long s : seeds){
            RNGList.add(new RNG(s));
        }
        if( seeds.get(id-1) == seed.getSeed()){
                for (BasicTile t : this.hand) {
                    List<Long> random = new ArrayList<Long>();
                    long newNumber;
                    TileGenerator generator = new TileGenerator(this.config);
                    for (int j = 0; j < seeds.size(); j++) {
                        random.add(RNGList.get(j).next());
                    }
                    newNumber = random.get(0);
                    for (int z = 1; z < random.size(); z++) {

                        newNumber = newNumber ^ random.get(z);
                    }

                    List<Biome> biomesL = generator.generateTile(newNumber);
                    BasicTile tempTile = new BasicTile(biomesL);
                    if(!tempTile.toString().equals(t.toString())){
                        throw new Exception("Cheated");
                    }

                }
            }
        else{
            throw new Exception("Cheated");
        }
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

        HashMap <Triple,Points> edgesToCheck;                           // neue eingelese Kanten
        int idForPoints = this.control.getActivePlayerID();             // ID des Players der die Karte gelegt hat
        if (prePlacing == true){
            edgesToCheck = Points.startPoints(tp,t,t.getOwnerID());     // preplacing Karten - andere ID-Abfrage, Aufruf points methode um zusammenhängende Biome zu finden
        }
        else{
        edgesToCheck = Points.startPoints(tp,this.gameBoard.currentCard(),idForPoints); // nicht preplacing Karten, aktuelle Karte des Boards
        }
        ArrayList <Points> biomeValues = new ArrayList <Points>();      // Liste mit Points Objekten zur Weitergabe
        
            for(Triple key : edgesToCheck.keySet()){                    // für jede offene Kante (key) in der HashMap der neu eingelesenen Kanten
                Triple oppositeEdge = key.getOpposite();                // für diese Kante die (mögliche) gegenüberliegende Kante finden
                Points oldConnectedBiome = openEdges.get(oppositeEdge); // dazugehöriges Biome in der Liste offener Kanten finden
                Points newConnectedBiome = edgesToCheck.get(key);       // zum Key gehörenden Value (zusammenhängendes Biome auf neuem Tile)
                if (newConnectedBiome != null){                         
                    if(openEdges.containsKey(oppositeEdge)){            // wenn die offenen Kanten die gegenüberliegende Kante beinhalten
                        if(Points.checkBiome(newConnectedBiome,oldConnectedBiome)){ // teste ob die beiden Kanten sich mergen = true oder abschließen = false lassen
                            for(Triple replaceKey : openEdges.keySet()){            // ersetze das alte Biome (Points) durch das neue
                                openEdges.replace(replaceKey,oldConnectedBiome,newConnectedBiome); 
                            }
                            if(biomeValues.contains(oldConnectedBiome)){    // sollte das alte Biome schon in der Liste zur Weitergabe liegen, entferne es (kann passieren wenn eine Kante über Eck mehere vorher zusammenhängende Biome miteinander verbindet)
                                biomeValues.remove(oldConnectedBiome);
                            }
                            biomeValues.add(newConnectedBiome);             // füge das neue Biome hinzu
                            openEdges.remove(oppositeEdge);                 // entferne die abgeschlossene Kante aus der Liste offener Kanten
                        }
                        else {                                              // wenn nur abgeschlossen
                            edgesToCheck.put(key,null);                     // setze den wert auf null
                            if(!biomeValues.contains(oldConnectedBiome)){   // und füge das alte Objekt zur Weitergabeliste hinzu (ist abgeschlossen, aber nicht das gleiche biome)
                                biomeValues.add(oldConnectedBiome);
                            }
                            openEdges.remove(oppositeEdge);                 // und entferne die abgeschlossene Kante aus der Liste der offenen Kanten
                        }
                    }
                    else {                                                  // es gibt keine gegenüberliegende Kante? einfach neu zur Liste der offenen Kanten hinzufügen   
                        openEdges.put(key,edgesToCheck.get(key));
                        edgesToCheck.put(key,null);
                    }
                }
            }
        checkForPoints(biomeValues);                                        // Liste mit den abgeschlossenen/gemergten Objekten weitergeben
    }

    /**
     * Checks the merged biomes. Takes a specified list of Point objects to check wether or not those objects contain any points
     * For each value, i.e. connected and completed biome, it checks if the openEdges HashMap contains that value, if so the biome still hasn't been completed <br>
     * If not, the specified list is checked if this value exists more than once, so that no double calculation happens and given to calculatePoints for further processing
     * @param biomeValues   the merged biomes readInNewEdges found
     */

    public void checkForPoints(ArrayList<Points> biomeValues){
            
        ArrayList <Points> alreadyChecked = new ArrayList<Points>();    // Check-Liste
        
            for (Points value : biomeValues){                           // für jeden Eintrag in der übergebenen Liste
                if(alreadyChecked.size()==0)  {                         // erster Fall:
                    if(!openEdges.containsValue(value)){                // wenn der Value nicht mehr in der openEdges Liste auftaucht
                        calculatePoints(value);                         // übergib ihn an die Punkteberechnung
                        alreadyChecked.add(value);                      // und füge ihn zur Liste der schon abgearbeiteten Biome hinzu
                    }
                }
                else if (!alreadyChecked.contains(value)) {             // so lange der Wert noch nicht in der Check-Liste auftaucht
                     if(!openEdges.containsValue(value)){               // und auch nicht mehr in der openEdges Liste
                        calculatePoints(value);                         // übergib ihn an die Punkteberechnung
                        alreadyChecked.add(value);                      // und füge ihn zur Liste der schon abgearbeiteten Biome hinzu
                    }
                }
            }
    }
    
    /** 
     * Calculates points by using the specified completed Points object that holds information about how many inter-tile crossings the objects has <br>
     * Reads the IDs of the involved players while considering the ID of preplaced tiles<br>
     * Then calculates the points this object will give, that is: (inter-tile crossings*involved players)+floor(inter-tile crossings^1.5) <br>
     * At last adding this points to each of the involved players scoreboardentry and setting a new score of this player if he/she has been involved in this object
     * @param biomeToCheck the Points object that is completed i.e. has no more open edges in the openEdges HashMap as calculated through checkForPoints
     */
     
    public void calculatePoints(Points biomeToCheck){
    
    int possiblePoints = Points.getPoints(biomeToCheck);                // die intertile-überschritte des Objekts
        if(possiblePoints>0){                                           // wenn es 0 sind gibts eh keine Punkte
            ArrayList <Integer> playerList = Points.getId(biomeToCheck);    // ID-Liste des Objekts
            if(playerList.contains(0)){                                 // preplaced tiles
                playerList.remove(0);
            }
            int newScore = possiblePoints*playerList.size()+(int) Math.floor(Math.pow(possiblePoints,1.5));     // Punkteberechnung laut Anleitung
                for (int j = 0; j<playerList.size();j++){;
                    this.scoreboard[playerList.get(j)-1] += newScore;   // je nach beteiligtem Spieler wird der Scoreboard hochgesetzt
                        }
                    if(playerList.contains(this.id)){                   // wenn die eigene ID beteiligt ist setze die score hoch
                        this.setScore(newScore);
                        }
        }
    } 
        
        
    /** Returns this players scoreboard
     * @return this players scoreboard
     */
     
    public int [] getScoreBoard(){
        return this.scoreboard;
    }
    }
