package doerfer.testing;

import java.util.*;
import doerfer.preset.Biome;
import doerfer.preset.TilePlacement;
import doerfer.preset.Tile;

/**
 * Class to administrate connected biomes as points-object.
 * Player classes uses Points methods to merge biomes if possible
 * 
 */

public class Points {
    
    /** Saves the type of biome of this connected biome */
    private Biome biome;                                          
    /** Saves the count of intertile connected biomes of this connected biome */
    private int points;                                                
    /** Saves the id of players involved in this connected biome */
    private ArrayList <Integer> id = new ArrayList <Integer>();
    /** Stack used as queue for bfs for connected biomes on one tile */
    private static Stack <Triple> bT;
    /** Array-list used to store temporarily the open edges of one connected biome on one tile */
    private static ArrayList <Triple> tTemp;
    
    
    
    /** Default constructor. Constructs a new empty Points object */
    
    public Points () {
    }
    
    /** Constructor: constructs a new Points object consisting of a biometyp, the owner ID(s) and the possible points it would give if it is completed
     * @param biome     the biome of the connected biome this object represents
     * @param idT       the ID of the player who owns this connected biome
     */
     
    public Points (Biome biome, int idT) {
        this.biome = biome;
        this.points = 0;
        this.id.add(idT);
    }
    
    /**Method to search for connected biomes on one tile using BFS<br>
     * Firstly uses the given information through BasicTile and TilePlacement to carry out a bfs on connected biomes on this specific tile, while adding each visited edge as a Triple to a temporary list. <br>
     * Secondly creates a points-object and connecting all Triple of the temporay list with this object via a hashMap, so that each connected edge refers to the same object. <br>
     * Finally returns the hashMap for further use
     * @param placement         the TilePlacement information of this tile
     * @param lastPlacedTile    the BasicTile information of this tile
     * @param idP               the id of the player who placed the tile
     * @return  a HashMap containing the open edges of this tile als triple objects and the connected biomes of this tile as points objects stored as a key-value pair so that multiple keys can reference to the same value i.e. one connected biome represents one points object and has multiple open edges represented through triple objects
     */
      
    public static HashMap<Triple,Points> startPoints (TilePlacement placement, BasicTile lastPlacedTile, int idP) {
        
        int rotationOfTile = placement.getRotation();                   // rotation des tiles abfragen
        int column = placement.getColumn();                             // column
        int row = placement.getRow();                                   // row
        BasicTile tile = lastPlacedTile;                
        int id = idP;                                                   // id davon       
       tile = tile.getRotated(rotationOfTile);                          // drehen

        bT = new Stack <Triple>();                                      // queue als Stack    
        tTemp = new ArrayList<Triple>();                                // temporäre Liste der Kanten eines zusammenhängenden Gebiets auf einem Tile
        boolean [] arr = new boolean [7];                               // indexliste wird auf true gesetzt wenn Gebiet abgearbeitet wurde
        HashMap <Triple,Points> tempHM = new HashMap <Triple,Points>(); // HashMap zum Zurückgeben
        
        // BFS
        Biome temp;
    
        for(int i = 6; i>=0; i--){                                      // iteriere über jede Kante (0-5) und den Center (6)
            if(!arr[i]){                                                // wenn noch nicht besucht
                if (i==6){                                              // Fall 1: center
                    temp = tile.getCenter();                            // biome das gesucht wird setzen
                    bT.push(new Triple(column,row,6));                  // In Warteschlange pushen; Center wird nicht in die Liste der Kanten aufgenommen
                }
            else{                                                       // Fall 2: edge
                temp = tile.getEdge(i);                                 // biome setzen
                bT.push(new Triple(column,row,i));                      // in queue pushen
                tTemp.add(new Triple(column,row,i));                    // in temporäre Liste der offenen Kanten dieses biomes packen
            }
            arr[i] = true;                                              // als besucht markieren
            
            while(!bT.empty()){                                         // solange Warteschlange nicht leer
                Triple current = bT.pop();                              // oberstes Element entfernen
                int edgePoint = current.getE();                         // Position der Ecke bestimmen
                switch(edgePoint){                                      // switch je nach Ecke/Center
                        case(0):                                        // wenn dieser Fall eintritt sind alle anderen Kanten schon betrachet worden
                        break;
                    case(5):                                            // Edge 5; also Ecke 0 und 4 betrachten
                        if (temp == tile.getEdge(0)&& !arr[0]) {        // ist das Biome auf temp das gleiche wie Ecke 0? und ist Eintrag 0 im boolean Array noch false==unbesucht? 
                            bT.push(new Triple (column,row,0));         // dann adde es in die queue
                            tTemp.add(new Triple (column,row,0));       // und füge es als offene Kante dieses zusammenhängenden Gebietes zur temporären Liste hinzu
                            arr[0] = true;                              // und ändere boolean Eintrag auf true==besucht
                        }
                        if (temp == tile.getEdge(4)&& !arr[4]) {
                            bT.push(new Triple (column,row,4));
                            tTemp.add(new Triple (column,row,4));
                            arr[4] = true;
                        }
                        break;

                case(6):                                                // Center Fall: Gehe alle edges durch
                        for(int j = 0; j<6; j++){
                            if (temp == tile.getEdge(j) && !arr[j]){
                                bT.push(new Triple (column,row,j));                                
                                tTemp.add(new Triple (column,row,j));
                                arr[j] = true;
                            }
                        }
                        break;
                case(1): case(2): case(3):case(4):                      // default case: edges links und rechts abfragen
                        if (temp == tile.getEdge(edgePoint+1) && !arr[edgePoint+1]){
                            bT.push(new Triple (column,row,edgePoint+1));                                
                            tTemp.add(new Triple (column,row,edgePoint+1));
                            arr[edgePoint+1] = true;
                        }
                        if (temp == tile.getEdge(edgePoint-1) && !arr[edgePoint-1]){
                            bT.push(new Triple (column,row,edgePoint-1));
                            tTemp.add(new Triple (column,row,edgePoint-1));
                            arr[edgePoint-1] = true;
                        }

                        break;
                        }   
                    }
                Points tempP = new Points (temp,id);                    // erstelle neues Points Objekt mit dem Biometyp und der ID des Spielers
                for (int j = 0; j<tTemp.size(); j++){                   // lese die Liste der Kanten die zusammenhängen aus
                    tempHM.put(tTemp.get(j),tempP);                     // füge in Hashmap mit Key=Triple und Value = Points Objekt ein
                }
            tTemp.clear();
            }
        }
        return tempHM;
    }
    
    /**Method to check if two points objects have the same biome type.<br>
     * Returns true if both objects have the same biome type after it forwarded it to the mergeBiome method
     * @param n the new object to compare to the old one
     * @param o the old object to be compared to
     * @return true if both points objects have the same biome
     */
    public static boolean checkBiome (Points n, Points o)   {
        Biome temp1 = getBiome(o);
        Biome temp2 = getBiome(n);
        if (temp1 == temp2){
            mergeBiome(n,o);                                            // ruft merge-Methode auf wenn Biome der Objekte gleich sind
            return true;
        }
        else {
            return false;                                                 
        }
    }

    /** Method to merge two points objects.<br>
     * It takes two objects and merging them, getting the ID(s) and points of the old one and transferring them to the new one through setID and setPoints 
     * @param n the new object to be merged into
     * @param o the old object merged into the new one
     */
    
    public static void mergeBiome(Points n, Points o){
        ArrayList <Integer> idN = getId(n);                             // frage die ID Liste des zu mergenden Objekts ab
        ArrayList <Integer> idO = getId(o);                             // frage die ID Liste des zu mergenden Objekts ab
        for(int i = 0; i<idO.size(); i++){                              // adde zu bestehendem Objekt wenn noch nicht in Liste
            if(!idN.contains(idO.get(i))){
                setId(n, idO.get(i));
            }
        }
        int point = getPoints(o);                                       // frage den Punktestand des zu mergenden Objekts ab
        setPoints(n,point+1);                                             // setze Punkte bestehenden Objekts neu
    }
    
    // SETTER und GETTER
    
    /** Returns the biome of this object
     * @param a the object of which to get the biome from
     * @return  the biome of the inputted object
     */
    public static Biome getBiome (Points a){
        return a.biome;
    }    
    /** Adds a new ID to the existing ID-list of this object
     * @param a      the object to which the ID should be added
     * @param idN    the new ID to be added to the ID-list of this object
     */
    public static void setId (Points a, int idN) {
        a.id.add(idN);
    }
    /** Returns a list of the Player IDs currently involved in this connected biome
     * @param a     the object from which to get the ID-list from
     * @return  a ArrayList in which all Player IDs are stored
     */
    public static ArrayList <Integer> getId (Points a) {
        return a.id;
    }
    
    /** Sets new points for this object
     * @param a         the object to which the points should be added
     * @param point     the amount of points that should be added to this object
     */
    public static void setPoints (Points a, int point){
        a.points+=point;
    }
    /** Returns the points this object stores
     * @param a the object from which the points should be returned
     * @return the points stored in this object
     */
    public static int getPoints(Points a){
        return a.points;
    }
    
    
    
/**
 * Class to create and use triples containing the row and column and position of an edge on a tile <br>
 * Used by points class and player classes to manage open edges and find the right open edge for merge or complete actions
 * 
 */
 
static class Triple{
    /** the x-coordinate of the tile */
    int x;
    /** the y-coordinate of the tile */
    int y;
    /** the position of the edge on this tile. from top = 0 clockwise to 5  */
    int e;
    /** Constructor for a triple object
     * @param x     the x-coordinate of the tile
     * @param y     the y-coordinate of the tile
     * @param e     the edge position on the tile
     */  
    public Triple (int x, int y, int e) {
        this.x = x;
        this.y = y;
        this.e = e;
    }
    /** Returns the x-coordinate of this triple
     * @return the x-coordinate of this triple
     */
    public int getX(){
        return this.x;
    }
    /** Returns the y-coordinate of this triple
     * @return the y-coordinate of this triple
     */
    public int getY(){
        return this.y;
    }
    /** Returns the edge position
     * @return the edge position of this triple
     */
    public int getE(){
        return this.e;
    }
    // findet das Triple, dass beim Anlegen gegenüber liegt
    // bestimmt ecke des übergebenen Triples
    // bestimmt damit position des gegenüberliegenden Triples durch odd-q und edge+3
    
    /** Method to get the opposite Triple of this triple i.e. the method returns the triple that lays next to this edge on the odd-q grid layout.
     * @return the opposite Triple to this Triple
     */
    public Triple getOpposite(){
        int edge = this.getE();
        int newX = this.getX();
        int newY = this.getY();
        switch(edge){                                                   // sucht gegenüberliegende Kante, also bestimmt Kantenlage nach welchen x/y Koordinaten gesucht werden muss
            case(0):                                                    // stimmt für alle fälle
                newY = newY-1;
                break;
            case(1):                                                    // wegen odd-q layout unterschiede bei geraden und ungeraden x-koordinaten
                if(newX%2==0) {
                    newX = newX+1;
                    newY = newY-1;
                }
                else {
                    newX = newX+1;
                }
                break;
            case(2):
                if(newX%2==0){
                    newX = newX+1;
                }
                else {
                    newX = newX+1;
                    newY = newY+1;
                }
                break;
            case(3):                                                    // stimmt für alle fälle
                newY = newY+1;
                break;
            case(4):
                if(newX%2==0){
                    newX=newX-1;
                }
                else{
                    newX = newX-1;
                    newY = newY+1;
                }
                break;
            case(5):
                if(newX%2==0){
                    newX = newX-1;
                    newY = newY-1;
                }
                else {
                    newX = newX-1;
                }
                break;
            }
        edge = edge+3;                                                  // gegenüberliegende Kante ist diese Kante+3
        if(edge>5){                                                     // und wenn das Ergebnis über 5 liegt
            edge = edge-6;                                              // wird einfach 6 davon abgezogen
        }
        return new Triple(newX,newY,edge);
    }
    /** Compares the specified object to this object. Returns true if both objects are of type Triple and having the same x/y-coordinates and the same e-entry.
     * @param other the object to be compared with equality with this object
     * @return true if the specified object is the same as this object
     */
    public boolean equals(Object other) {
    if (!(other instanceof Triple)) {
        return false;
    }
    Triple otherPoint = (Triple)other;
    return otherPoint.getX() == this.getX() && otherPoint.getY() == this.getY() && otherPoint.getE() == this.getE();
    }
    /** Returns the hash code value of this object.
     * @return the hash code value of this object
     */
    public int hashCode () {
        int x = getX();
        int y = getY();
        int hash = (y+((x+1)/2));

       
            return getX() +( hash*hash)*(int)(Math.pow(13,getE()));
        
    }
}
}
