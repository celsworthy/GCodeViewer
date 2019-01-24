package celtech.gcodeviewer.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.joml.Vector3f;

/**
 *
 * @author Tony
 */
public class GCodeViewerConfiguration {

    // Jackson deserializer for Vector3f.
    public static class Vector3fDeserializer extends StdDeserializer<Vector3f> {
     
        public Vector3fDeserializer() {
            this(null);
        }

        public Vector3fDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Vector3f deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException
        {
            ObjectCodec codec = jp.getCodec();
            JsonNode node = codec.readTree(jp);
            Vector3f v3f = new Vector3f(node.get(0).floatValue(),
                                        node.get(1).floatValue(),
                                        node.get(2).floatValue());
            return v3f;
        }
    }

    // Jackson deserializer for type colour map.
    public static class TypeColourMapDeserializer extends StdDeserializer<Map<String, Vector3f>> {
     
        public TypeColourMapDeserializer() {
            this(null);
        }

        public TypeColourMapDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Map<String, Vector3f> deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException
        {
            ObjectCodec codec = jp.getCodec();
            JsonNode node = codec.readTree(jp);
            Map<String, Vector3f> typeColourMap = new HashMap<>();
            for (final JsonNode entryNode : node) {
                String typeName = entryNode.get("type").asText();
                JsonNode v3fArray = entryNode.get("colour");
                Vector3f typeColour = new Vector3f(v3fArray.get(0).floatValue(),
                                                   v3fArray.get(1).floatValue(),
                                                   v3fArray.get(2).floatValue());
                typeColourMap.put(typeName, typeColour);                
            }           
            return typeColourMap;
        }
    }
    
    public static class PrintVolumeDetails {
        
        @JsonIgnore
        private Vector3f dimensions;
        @JsonIgnore
        private Vector3f offset;
        
        public PrintVolumeDetails() {
            dimensions = null;
            offset = null;
        }
    
        public PrintVolumeDetails(Vector3f d, Vector3f o) {
            dimensions = d;
            offset = o;
        }

        @JsonProperty
        public Vector3f getDimensions() {
            return dimensions;
        }
                
        @JsonProperty
        public void setDimensions(Vector3f d) {
            dimensions = d;
        }
        
        @JsonProperty
        public Vector3f getOffset() {
            return offset;
        }
                
        @JsonProperty
        public void setOffset(Vector3f o) {
            offset = o;
        }
    }
    
    // Jackson deserializer for PrintVolumeDetails map.
    public static class PrintVolumeDetailsMapDeserializer extends StdDeserializer<Map<String, PrintVolumeDetails>> {
     
        public PrintVolumeDetailsMapDeserializer() {
            this(null);
        }

        public PrintVolumeDetailsMapDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Map<String, PrintVolumeDetails> deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException
        {
            ObjectCodec codec = jp.getCodec();
            JsonNode node = codec.readTree(jp);
            Map<String, PrintVolumeDetails> pvdMap = new HashMap<>();
            for (final JsonNode entryNode : node) {
                JsonNode v3fArray;
                String typeName = entryNode.get("type").asText();
                v3fArray = entryNode.get("dimensions");
                Vector3f dimensions = new Vector3f(v3fArray.get(0).floatValue(),
                                                   v3fArray.get(1).floatValue(),
                                                   v3fArray.get(2).floatValue());
                v3fArray = entryNode.get("offset");
                Vector3f offset = new Vector3f(v3fArray.get(0).floatValue(),
                                               v3fArray.get(1).floatValue(),
                                               v3fArray.get(2).floatValue());
                PrintVolumeDetails pvd = new PrintVolumeDetails();
                pvd.setDimensions(dimensions);
                pvd.setOffset(offset);
                pvdMap.put(typeName, pvd);                
            }           
            return pvdMap;
        }
    }
    
    @JsonIgnore
    private static final Stenographer STENO = StenographerFactory.getStenographer(GCodeViewerConfiguration.class.getName());
    
    @JsonIgnore
    private Map<String, Vector3f> typeColourMap = new HashMap<>();
    @JsonIgnore
    private List<Vector3f> dataColourPalette = new ArrayList<>();
    @JsonIgnore
    private List<Vector3f> toolColours = new ArrayList<>();
    @JsonIgnore
    private List<Double> toolFilamentFactors = new ArrayList<>();
    @JsonIgnore
    private Vector3f moveColour = new Vector3f(0.0f, 0.0f, 1.0f);
    @JsonIgnore
    private Vector3f selectColour = new Vector3f(1.0f, 1.0f, 0.0f);
    @JsonIgnore
    private Vector3f defaultColour = new Vector3f(0.9882f, 0.3608f, 0.0471f);
    @JsonIgnore
    private PrintVolumeDetails defaultPrintVolumeDetails = new PrintVolumeDetails(new Vector3f(210.0f, 150.0f, 100.0f),
                                                                                  new Vector3f(0.0f, 0.0f, 0.0f));
    @JsonIgnore
    private Map<String, PrintVolumeDetails> printVolumeDetailsMap = new HashMap<>();
    @JsonIgnore
    private Vector3f lightColour = new Vector3f(1.0f, 1.0f, 1.0f);
    @JsonIgnore
    private Vector3f lightPosition = new Vector3f(105.0f, 37.5f, 200.0f);
    @JsonIgnore
    private double defaultFilamentFactor = 1.0;
    @JsonIgnore
    private boolean relativeExtrusionAsDefault = true;
    
    GCodeViewerConfiguration() {
    }

    @JsonIgnore
    public static GCodeViewerConfiguration loadFromJSON() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule v3fModule = new SimpleModule("Vector3fDeserializer", new Version(1, 0, 0, null, null, null));
        v3fModule.addDeserializer(Vector3f.class, new Vector3fDeserializer());
        objectMapper.registerModule(v3fModule);
        String configPath = getApplicationInstallDirectory() + "GCodeViewer.json";
            
        GCodeViewerConfiguration configuration = null;
        try {
            configuration = objectMapper.readValue(new File(configPath), GCodeViewerConfiguration.class);
        }
        catch (IOException ex) {
            STENO.error("Couldn't load configuration - using defaults");
            configuration = new GCodeViewerConfiguration();
        }
        return configuration;
    }
    
    @JsonIgnore
    public static String getApplicationInstallDirectory() {
        String installDirectory = "." + File.separator;
        
        try
        {
            installDirectory = Configuration.getInstance().getFilenameString("ApplicationConfiguration",
                                                              "FakeInstallDirectory",
                                                              null);
        } catch (ConfigNotLoadedException ex)
        {
            STENO.error("Couldn't load configuration - the application cannot derive the install directory");
        }
        
        if (installDirectory == null)
        {
            try
            {
                String path = GCodeViewerConfiguration.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                URI uri = new URI(path);
                File file = new File(uri.getSchemeSpecificPart());
                String actualPath = file.getCanonicalPath();
                actualPath = actualPath.replaceFirst("[a-zA-Z0-9]*\\.jar", "");
                installDirectory = actualPath;
            } catch (URISyntaxException ex)
            {
                STENO.error("URI Syntax Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
            } catch (IOException ex)
            {
                STENO.error("IO Exception whilst attempting to determine the application path - the application is unlikely to run correctly.");
            }
        }
        
        return installDirectory;
    }
    
    @JsonIgnore
    public Vector3f getColourForType(String type) {
        return typeColourMap.getOrDefault(type, defaultColour);
    }

    @JsonIgnore
    public Vector3f getColourForTool(int toolNumber) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            return toolColours.get(toolNumber);
        else
            return defaultColour;
    }

    @JsonIgnore
    public PrintVolumeDetails getPrintVolumeDetailsForType(String type) {
        return printVolumeDetailsMap.getOrDefault(type, defaultPrintVolumeDetails);
    }

    @JsonProperty
    public Vector3f getDefaultColour() {
        return defaultColour;
    }

    @JsonProperty
    public void setDefaultColour(Vector3f defaultColour) {
        this.defaultColour = defaultColour;
    }

    @JsonProperty
    public List<Vector3f> getToolColours() {
        return toolColours;
    }

    @JsonProperty
    public void setToolColours(List<Vector3f> toolColours) {
        this.toolColours = toolColours;
    }
    
    @JsonProperty
    public Map<String, Vector3f> getTypeColourMap() {
        return typeColourMap;
    }

    @JsonProperty
    @JsonDeserialize(using = TypeColourMapDeserializer.class,
                     keyAs = String.class, contentAs = Vector3f.class)
    public void setTypeColourMap(Map<String, Vector3f> typeColourMap) {
        this.typeColourMap = typeColourMap;
    }

    @JsonProperty
    public List<Vector3f> getDataColourPalette() {
        return dataColourPalette;
    }

    @JsonProperty
    public void setDataColourPalette(List<Vector3f> dataColourPalette) {
        this.dataColourPalette = dataColourPalette;
    }

    @JsonProperty
    public List<Double> getToolFilamentFactors() {
        return toolFilamentFactors;
    }

    @JsonProperty
    public void setToolFilamentFactors(List<Double> toolFilamentFactors) {
        this.toolFilamentFactors = toolFilamentFactors;
    }

    @JsonIgnore
    double getFilamentFactorForTool(int toolNumber) {
        if (toolNumber >= 0 && toolNumber < toolFilamentFactors.size())
            return toolFilamentFactors.get(toolNumber);
        else
            return defaultFilamentFactor;
    }

    @JsonProperty
    public double getDefaultFilamentFactor() {
        return defaultFilamentFactor;
    }

    @JsonProperty
    public void setDefaultFilamentFactor(double defaultFilamentFactor) {
        this.defaultFilamentFactor = defaultFilamentFactor;
    }

    @JsonProperty
    public PrintVolumeDetails getDefaultPrintVolumeDetails() {
        return defaultPrintVolumeDetails;
    }

    @JsonProperty
    public void setDefaultPrintVolumeDetails(PrintVolumeDetails printVolumeDetails) {
        this.defaultPrintVolumeDetails = printVolumeDetails;
    }

    @JsonProperty
    public Map<String, PrintVolumeDetails> getPrintVolumeDetailsMap() {
        return printVolumeDetailsMap;
    }

    @JsonProperty
    @JsonDeserialize(using = PrintVolumeDetailsMapDeserializer.class,
                     keyAs = String.class, contentAs = PrintVolumeDetails.class)
    public void setPrintVolumeDetailsMap(Map<String, PrintVolumeDetails> printVolumeDetailsMap) {
        this.printVolumeDetailsMap = printVolumeDetailsMap;
    }

    @JsonProperty
    public Vector3f getLightPosition() {
        return lightPosition;
    }

    @JsonProperty
    public void setLightPosition(Vector3f lightPosition) {
        this.lightPosition = lightPosition;
    }

    @JsonProperty
    public Vector3f getLightColour() {
        return lightColour;
    }
    
    @JsonProperty
    public void setLightColour(Vector3f lightColour) {
        this.lightColour = lightColour;
    }

    @JsonProperty
    public boolean getRelativeExtrusionAsDefault() {
        return relativeExtrusionAsDefault;
    }

    @JsonProperty
    public void setRelativeExtrusionAsDefault(boolean relativeExtrusionAsDefault) {
        this.relativeExtrusionAsDefault = relativeExtrusionAsDefault;
    }

    @JsonProperty
    public Vector3f getMoveColour() {
        return moveColour;
    }

    @JsonProperty
    public void setMoveColour(Vector3f moveColour) {
        this.moveColour = moveColour;
    }

    @JsonProperty
    public Vector3f getSelectColour() {
        return selectColour;
    }

    @JsonProperty
    public void setSelectColour(Vector3f selectColour) {
        this.selectColour = selectColour;
    }
}
