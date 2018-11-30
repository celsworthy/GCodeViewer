package celtech.gcodeviewer.engine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Tony
 */
public class GCodeViewerConfiguration {
    private Map<String, Vector3f> typeColourMap = new HashMap<>();
    private List<Vector3f> dataColourPallet = new ArrayList<>();
    private List<Vector3f> toolColours = new ArrayList<>();
    private Vector3f moveColour = new Vector3f(0.0f, 0.0f, 1.0f);
    private Vector3f defaultColour = new Vector3f(0.9882f, 0.3608f, 0.0471f);
    private Vector3f printVolume = new Vector3f(210.0f, 150.0f, 100.0f);
    private Vector3f lightColour = new Vector3f(1.0f, 1.0f, 1.0f);
    private Vector3f lightPosition = new Vector3f(105.0f, 37.5f, 200.0f);
    private double filamentFactor = 1.0;
    private boolean relativeExtrusionAsDefault = true;
    
    GCodeViewerConfiguration() {
    }
    
    public static GCodeViewerConfiguration loadFromFile(String fileName) {
        GCodeViewerConfiguration configuration = new GCodeViewerConfiguration();

        try {
            String text = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);

            JSONObject configObj = new JSONObject(text);
            JSONArray vectorArray = null;

            if (configObj.has("typeColourMap"))
            {
                JSONArray typeColourArray = configObj.getJSONArray("typeColourMap");

                for(int i = 0; i < typeColourArray.length(); i++) {
                    JSONObject entry = typeColourArray.getJSONObject(i);
                    String typeName = entry.getString("type");
                    vectorArray = entry.getJSONArray("colour");
                    Vector3f typeColour = new Vector3f(vectorArray.getFloat(0),
                                                       vectorArray.getFloat(1),
                                                       vectorArray.getFloat(2));
                    configuration.typeColourMap.put(typeName, typeColour);                
                }           
            }
            
            if (configObj.has("dataColourPallet"))
            {
                loadColourArray(configObj.getJSONArray("dataColourPallet"), configuration.dataColourPallet);
            }
            
            if (configObj.has("toolColours"))
            {
                loadColourArray(configObj.getJSONArray("toolColours"), configuration.toolColours);
            }

            if (configObj.has("defaultColour"))
            {
                vectorArray = configObj.getJSONArray("defaultColour");
                configuration.defaultColour = new Vector3f(vectorArray.getFloat(0),
                                                           vectorArray.getFloat(1),
                                                           vectorArray.getFloat(2));
            }

            if (configObj.has("moveColour"))
            {
                vectorArray = configObj.getJSONArray("moveColour");
                configuration.moveColour = new Vector3f(vectorArray.getFloat(0),
                                                        vectorArray.getFloat(1),
                                                        vectorArray.getFloat(2));
            }

            if (configObj.has("printVolume"))
            {
                vectorArray = configObj.getJSONArray("printVolume");
                configuration.printVolume = new Vector3f(vectorArray.getFloat(0),
                                                        vectorArray.getFloat(1),
                                                        vectorArray.getFloat(2));
            }

            if (configObj.has("lightPosition"))
            {
                vectorArray  = configObj.getJSONArray("lightPosition");
                configuration.lightPosition = new Vector3f(vectorArray.getFloat(0),
                                                           vectorArray.getFloat(1),
                                                           vectorArray.getFloat(2));
            }

            if (configObj.has("lightColour"))
            {
                vectorArray  = configObj.getJSONArray("lightColour");
                configuration.lightColour = new Vector3f(vectorArray.getFloat(0),
                                                         vectorArray.getFloat(1),
                                                         vectorArray.getFloat(2));
            }

            if (configObj.has("relativeExtrusionAsDefault"))
            {
                configuration.relativeExtrusionAsDefault = configObj.getBoolean("relativeExtrusionAsDefault");
            }
        }
        catch(Exception ex){
            System.out.println(ex.toString());
        }
        
        return configuration;
    }

    private static void loadColourArray(JSONArray colourArray, List<Vector3f> colourList ) {
        for(int i = 0; i < colourArray.length(); i++) {
            JSONArray vectorArray = colourArray.getJSONArray(i);
            Vector3f pallettColour = new Vector3f(vectorArray.getFloat(0),
                                                  vectorArray.getFloat(1),
                                                  vectorArray.getFloat(2));
            colourList.add(pallettColour);                
        }           
    }

    Vector3f getColourForType(String type) {
        return typeColourMap.getOrDefault(type, defaultColour);
    }

    Vector3f getColourForTool(int toolNumber) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            return toolColours.get(toolNumber);
        else
            return defaultColour;
    }

    Vector3f getDefaultColour() {
        return defaultColour;
    }

    List<Vector3f> getToolColours() {
        return toolColours;
    }

    List<Vector3f> getDataColourPalette() {
        return dataColourPallet;
    }

    double getFilamentFactor() {
        return filamentFactor;
    }

    Vector3f getPrintVolume() {
        return printVolume;
    }

    Vector3f getLightPosition() {
        return lightPosition;
    }

    Vector3f getLightColour() {
        return lightColour;
    }
    
    boolean getRelativeExtrusionAsDefault() {
        return relativeExtrusionAsDefault;
    }

    Vector3f getMoveColour() {
        return moveColour;
    }
}
