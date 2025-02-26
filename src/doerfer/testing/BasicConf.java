package doerfer.testing;

import doerfer.preset.*;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

import java.util.*;

/**
 * Reads GameConfiguration file
 * 
 */
public class BasicConf implements doerfer.preset.GameConfiguration{

    public static final String MAGICNUMBER = "DoerferGameConfigurationv1";
    /**
     * Description
     */
    private String desc;
    /**
     * Number of Players
     */
    private int anzahlSpieler;
    /**
     * Number of Cards in Stack
     */
    private int anzahlKarten;
    /**
     * List of probability's that specific Bioms are drawn
     */
    private Map<Biome, Integer> bioWs;
    /**
     * List of Weights for specific Bioms
     */
    private Map<Biome, Integer> bioGw;
    /**
     * Number of preplaced Tiles
     */
    private int anzahlpreplacedTile;

    /**
     * All placements of all preplaced Tiles
     */
    private List<TilePlacement> preplacedTilePlacement;
    /**
     * all Tiles of all preplaced Tiles
     */
    private List<Tile> preplacedTile;
    /**
     * ID's of all owners of preplaced Tiles
     */
    private List<Integer> IDs;
    /**
     * Reference of Settings
     */
    private Settings settings;
    /**
     * Reads Configuration file
     * @param settings1 Settings of the game
     */
    BasicConf(Settings settings1){
        try {
            // initialisiert Listen
            settings = settings1;
            bioWs = new HashMap<>();
            bioGw = new HashMap<>();
            IDs = new ArrayList<>();
            preplacedTile = new ArrayList<>();
            preplacedTilePlacement = new ArrayList<>();
            //wählt File aus, in dem gescannt wird
            Scanner scan = new Scanner(this.settings.gameConfigurationFile);
            String bio = "";
            try{
                //liest Beschreibung ein
                scan.nextLine();
                desc = scan.nextLine();
                //liest die Anzahl der Spieler ein
                anzahlSpieler = scan.nextInt();
                //liest die Anzahl der Karten auf dem Stack ein und falls zu wenig Error
                anzahlKarten = scan.nextInt();
                if(anzahlKarten<=anzahlSpieler+1){
                    throw new Error("zu wenig Karten");
                }
                //Array, um zu erkennen, ob alle Biome angesprochen wurden
                boolean[] alleBiome = new boolean[6];
                //Wahrscheinlichkeit und Gewichtung für Biome werden verteilt
                for(int i = 0; i < 6; i++) {
                    //Liest nächstes Biom ein
                    bio = scan.next();
                    //Liest Gewichte und Wahrscheinlichkeit der Biome ein
                    if (bio.equals("PLAINS")) {
                        bioWs.put(Biome.PLAINS, scan.nextInt());
                        bioGw.put(Biome.PLAINS, scan.nextInt());
                        alleBiome[0] = true;
                    } else if (bio.equals("HOUSES")) {
                        bioWs.put(Biome.HOUSES, scan.nextInt());
                        bioGw.put(Biome.HOUSES, scan.nextInt());
                        alleBiome[1] = true;
                    } else if (bio.equals("FOREST")) {
                        bioWs.put(Biome.FOREST, scan.nextInt());
                        bioGw.put(Biome.FOREST, scan.nextInt());
                        alleBiome[2] = true;
                    } else if (bio.equals("FIELDS")) {
                        bioWs.put(Biome.FIELDS, scan.nextInt());
                        bioGw.put(Biome.FIELDS, scan.nextInt());
                        alleBiome[3] = true;
                    } else if (bio.equals("WATER")) {
                        bioWs.put(Biome.WATER, scan.nextInt());
                        bioGw.put(Biome.WATER, scan.nextInt());
                        alleBiome[4] = true;
                    } else if (bio.equals("TRAINTRACKS")) {
                        bioWs.put(Biome.TRAINTRACKS, scan.nextInt());
                        bioGw.put(Biome.TRAINTRACKS, scan.nextInt());
                        alleBiome[5] = true;
                    }else {
                        //Error falls ein nicht bekanntes Biom eingelesen wurde
                        throw new Error("Falsche Eingabe der Biome");
                    }
                }
                //falls nicht alle Biome initialisiert wurden, Error ausgeben
                for (boolean b : alleBiome) {
                    if (b == false) {
                        throw new Error("nicht alle Biome");
                    }
                }
                //immer das neu eingelesene Biom
                Biome neuesBiom;
                //Spieler ID
                int SpielerID;
                //Anzahl der PreplacedTiles einlesen und falls weniger als Spieleranzahl Error ausgeben
                anzahlpreplacedTile = scan.nextInt();
                if(anzahlpreplacedTile < anzahlSpieler){
                    throw new Error("zu wenig preplacedTiles");
                }

                //geht alle preplacedTiles durch
                for(int z = 0; z < anzahlpreplacedTile; z++){
                    try {
                        List<Biome> neuesABiom = new ArrayList<>();
                        if (scan.hasNext()) {
                            SpielerID = scan.nextInt();
                        } else {
                            throw new Error("zu wenig preplaced Tiles");
                        }

                        //falls nicht vorhandene ID eingelesen, dann Error ausgeben
                        if (SpielerID >= 0 && SpielerID <= anzahlSpieler) {
                            IDs.add(SpielerID);
                            //lese TilePlacement von neuen preplacedTile ein
                            preplacedTilePlacement.add(new TilePlacement(scan.nextInt(), scan.nextInt(), scan.nextInt()));
                            //lese die Biome der neuen Karte ein
                            Scanner tmp = new Scanner(scan.next());
                            tmp.useDelimiter(",");
                            for (int n = 0; n < 6; n++) {
                                neuesBiom = Biome.valueOf(tmp.next());
                                neuesABiom.add(neuesBiom);
                            }
                            BasicTile neu = new BasicTile(neuesABiom);
                            neu.setOwnerID(SpielerID);
                            //füge neues preplacedTile hinzu
                            preplacedTile.add(neu);

                        } else {
                            throw new Error("Spieler gibts nicht");
                        }
                    }catch (Exception e){
                        throw new Error ("Falsche Eingabe der preplaced Tiles");
                    }
                }
                for(int t = 0; t < preplacedTilePlacement.size();t++){
                    for(int n = t+1; n < preplacedTilePlacement.size();n++){
                        if(preplacedTilePlacement.get(t).getColumn() == preplacedTilePlacement.get(n).getColumn()){
                            if(preplacedTilePlacement.get(t).getRow() == preplacedTilePlacement.get(n).getRow()){
                                throw new Error("preplaced Tile an gleicher Stelle");
                            }
                        }
                    }
                }

            }catch(Exception e){// Error falls irgendwann falsche Eingabe eingegeben
                throw new Error("falsche Eingabe");
            }

        }catch (Exception e){
            throw new Error("File not Found");
        }
    }

    /**
     * Returns description of the Configuration from the Configuration file <br>
     * @return Description
     */
    @Override
    public String getDescription(){
        return this.desc;
    }

    /**
     * Returns the Number of Players from the Configuration file
     * @return Number of Players
     */
    @Override
    public int getNumPlayers(){
        return this.anzahlSpieler;
    }

    /**
     * Returns the number of preplaced Tiles from the Configuration file
     * @return number of preplaced Tiles
     */
    @Override
    public int getNumTiles(){
        return this.anzahlKarten;
    }

    /**
     * Returns list of probability's that specific Bioms are drawn from the Configuration file
     * @return list of probability's that specific Bioms are drawn
     */
    @Override
    public Map<Biome, Integer> getBiomeChances(){
        return bioWs;
    }

    /**
     * Returns Weight of the Bioms from the Configuration file
     * @return Weight of the Bioms
     */
    @Override
    public Map<Biome, Integer> getBiomeWeights(){
        return this.bioGw;
    }

    /**
     * Returns list of ID's, which are the owner of the preplaced Tiles from the Configuration file
     * @return list of ID's, which are the owner of the preplaced Tiles
     */
    @Override
    public List<Integer> getPreplacedTilesPlayerIDs(){
        return this.IDs;
    }

    /**
     * Returns list of preplaced Tile placements from the Configuration file
     * @return list of preplaced Tile placements
     */
    @Override
    public List<TilePlacement> getPreplacedTilesPlacements(){
        return this.preplacedTilePlacement;
    }

    /**
     * Returns list of preplaced Tiles from the Configuration file
     * @return list of preplaced Tiles
     */
    @Override
    public List<Tile> getPreplacedTiles(){
        return this.preplacedTile;
    }



}