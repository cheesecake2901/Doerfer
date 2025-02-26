package doerfer.testing;
import doerfer.preset.Biome;
import doerfer.preset.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * BasicTiles repraesentieren die Spielkarte(Hexes)
 *
 * Jede Ecke hat ein Biom
 * Das Biom in der Mitte wird nach gewissen Regeln festgelegt.
 *
 *
 */
public class BasicTile extends Tile {

    /**
     * Saves ID of the owner of the Tile
     */
    private int ownerID;

    /**
     * Constructor
     *
     * @param biomes Eine Liste mit Biomen die den Kanten des Hexes zugewiesen werdne.
     */
    public BasicTile(List<Biome> biomes) {
        super(biomes);
    }


    /**
     * Identify central Biom of a Tile
     * @return central Biom of Tile
     */
    @Override
    public Biome getCenter() {

        Biome center = Biome.FIELDS;
        int max = 0;

        if(contains(Biome.TRAINTRACKS))
            return Biome.TRAINTRACKS;

        max = getCount(center);
        if(getCount(Biome.HOUSES) >= max){
            max = getCount(Biome.HOUSES);
            center = Biome.HOUSES;
        }
        if(getCount(Biome.FOREST) >= max){
            max = getCount(Biome.FOREST);
            center = Biome.FOREST;
        }
        if(getCount(Biome.PLAINS) >= max){
            max = getCount(Biome.PLAINS);
            center = Biome.PLAINS;
        }
        if(getCount(Biome.WATER) >= max){
            max = getCount(Biome.WATER);
            center = Biome.WATER;
        }
        return center;
    }


    /**
     * Identifies if a Tile contains a specific Biom
     * @param b wanted Biom
     * @return true if Tile contains Biom
     */
    private boolean contains(Biome b){
        for(int i = 0; i < 6; i++) {
            if (this.getEdge(i).equals(b))
            return true;
        }
        return false;
    }


    /**
     * Returns Tile, which got rotated r times
     * @param r Number of Rotations
     * @return rotated Tile
     */
    public BasicTile getRotated(int r){

        List<Biome> rotated = new ArrayList<Biome>();

        if(r < 0)
            r = 6 + r;

        r = r % 6;


        for(int i = 0; i <  6; i++){
            rotated.add(this.getEdge((i+r)%6));
        }

        return new BasicTile(rotated);

    }

    /**
     * Returns Tile information as a String
     * @return information of Tile
     */
    public String toString(){
        String s = "Tile owner: " + this.getOwnerID();
        s = s + " "  + getEdge(0).name();
        s = s + " "  + getEdge(1).name();
        s = s + " "  + getEdge(2).name();
        s = s + " "  + getEdge(3).name();
        s = s + " "  + getEdge(4).name();
        s = s + " "  + getEdge(5).name();
        return s;
    }

    /**
     * Counts how many times a Tile contains a specific Biom
     * @param b Biom which frequency is queried
     * @return Number of frequency
     */
    private int getCount(Biome b){
        int count = 0;
        for(int i = 0; i < 6; i++) {
            if (this.getEdge(i).equals(b))
                count++;
        }
        return count;
    }

    /**
     * Returns ID of Owner
     * @return Player ID
     */
    public int getOwnerID(){
        return this.ownerID;
    }

    /**
     * Sets owner of Tile
     * @param x the ID to be set
     */
    public void setOwnerID(int x){
        this.ownerID = x;
    }




}
