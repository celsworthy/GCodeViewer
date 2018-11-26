package celtech.gcodeviewer.engine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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
    Vector3f moveColour = new Vector3f(0.0f, 0.0f, 1.0f);
    Vector3f defaultTypeColour = new Vector3f(0.9882f, 0.3608f, 0.0471f);
    Vector3f printVolume = new Vector3f(210.0f, 150.0f, 100.0f);
    Vector3f lightColour = new Vector3f(1.0f, 1.0f, 1.0f);
    Vector3f lightPosition = new Vector3f(105.0f, 37.5f, 200.0f);
    Vector3f tool0Colour = new Vector3f(1.0f, 1.0f, 0.0f);
    Vector3f tool1Colour = new Vector3f(0.0f, 1.0f, 1.0f);
    double filamentFactor = 1.0;
    boolean relativeExtrusionAsDefault = true;
    
    GCodeViewerConfiguration() {
    }
    
    public static GCodeViewerConfiguration loadFromFile(String fileName) {
        GCodeViewerConfiguration configuration = new GCodeViewerConfiguration();

        try {
            String text = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);

            JSONObject configObj = new JSONObject(text);
            JSONArray typeColourArray = configObj.getJSONArray("typeColourMap");
            JSONArray vectorArray = null;

            for(int i = 0; i < typeColourArray.length(); i++) {
                JSONObject entry = typeColourArray.getJSONObject(i);
                String typeName = entry.getString("type");
                vectorArray = entry.getJSONArray("colour");
                Vector3f typeColour = new Vector3f(vectorArray.getFloat(0),
                                                   vectorArray.getFloat(1),
                                                   vectorArray.getFloat(2));
                configuration.typeColourMap.put(typeName, typeColour);                
            }           

            if (configObj.has("defaultTypeColour"))
            {
                vectorArray = configObj.getJSONArray("defaultTypeColour");
                configuration.defaultTypeColour = new Vector3f(vectorArray.getFloat(0),
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

            if (configObj.has("tool0Colour"))
            {
                vectorArray  = configObj.getJSONArray("tool0Colour");
                configuration.tool0Colour = new Vector3f(vectorArray.getFloat(0),
                                                         vectorArray.getFloat(1),
                                                         vectorArray.getFloat(2));
            }

            if (configObj.has("tool1Colour"))
            {
                vectorArray  = configObj.getJSONArray("tool1Colour");
                configuration.tool1Colour = new Vector3f(vectorArray.getFloat(0),
                                                         vectorArray.getFloat(1),
                                                         vectorArray.getFloat(2));
            }
        }
        catch(Exception ex){
            System.out.println(ex.toString());
        }
        
        return configuration;
    }

    Vector3f getColourForType(String type) {
        return typeColourMap.getOrDefault(type, defaultTypeColour);
    }

    Vector3f getDefaultColourForType() {
        return defaultTypeColour;
    }

    Vector3f getColourForMove() {
        return moveColour;
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

    Vector3f getTool0Colour() {
        return tool0Colour;
    }
    
    Vector3f getTool1Colour() {
        return tool1Colour;
    }
}
