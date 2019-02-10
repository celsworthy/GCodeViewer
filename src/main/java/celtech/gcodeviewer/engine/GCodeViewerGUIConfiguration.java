package celtech.gcodeviewer.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony
 */
public class GCodeViewerGUIConfiguration {
    @JsonIgnore
    private static final String GUI_CONFIG_FILE_NAME = "GCodeViewerGUI.json";
    
    @JsonIgnore
    private static final Stenographer STENO = StenographerFactory.getStenographer(GCodeViewerGUIConfiguration.class.getName());
    
    @JsonIgnore
    private boolean sliderPanelExpanded = false;
    @JsonIgnore
    private boolean gCodePanelExpanded = false;
    @JsonIgnore
    private boolean controlPanelExpanded = false;

    @JsonIgnore
    private int topLayerToRender = 0;
    @JsonIgnore
    private int bottomLayerToRender = 0;
    @JsonIgnore
    private int firstSelectedLine = 0;
    @JsonIgnore
    private int lastSelectedLine = 0;
    
    @JsonIgnore
    private boolean showMoves = false;
    @JsonIgnore
    private boolean showOnlySelected = false;
    @JsonIgnore
    private int showTools = 0xFFFF; 
    @JsonIgnore
    private int showTypes = 0xFFFF; 
    @JsonIgnore
    private RenderParameters.ColourMode colourMode = RenderParameters.ColourMode.COLOUR_AS_TOOL;

    GCodeViewerGUIConfiguration() {
    }

    @JsonIgnore
    public static GCodeViewerGUIConfiguration loadFromJSON(String projectDirectory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String configPath = projectDirectory + File.separator + GUI_CONFIG_FILE_NAME;
            
        GCodeViewerGUIConfiguration configuration = null;
        try {
            configuration = objectMapper.readValue(new File(configPath), GCodeViewerGUIConfiguration.class);
        }
        catch (IOException ex) {
            STENO.error("Couldn't load gui configuration");
            configuration = new GCodeViewerGUIConfiguration();
        }
        return configuration;
    }
    
    @JsonIgnore
    public void saveToJSON(String projectDirectory) {
        ObjectMapper objectMapper = new ObjectMapper();
        String configPath = projectDirectory + File.separator + GUI_CONFIG_FILE_NAME;
            
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(configPath), this);
        }
        catch (IOException ex) {
            STENO.error("Couldn't save gui configuration");
        }
    }

    @JsonProperty
    public boolean getControlPanelExpanded() {
        return controlPanelExpanded;
    }

    @JsonProperty
    public void setControlPanelExpanded(boolean controlPanelExpanded) {
        this.controlPanelExpanded = controlPanelExpanded;
    }

    @JsonProperty
    public boolean getGCodePanelExpanded() {
        return gCodePanelExpanded;
    }

    @JsonProperty
    public void setGCodePanelExpanded(boolean gCodePanelExpanded) {
        this.gCodePanelExpanded = gCodePanelExpanded;
    }

    @JsonProperty
    public boolean getSliderPanelExpanded() {
        return sliderPanelExpanded;
    }

    @JsonProperty
    public void setSliderPanelExpanded(boolean sliderPanelExpanded) {
        this.sliderPanelExpanded = sliderPanelExpanded;
    }

    @JsonProperty
    public int getTopLayerToRender() {
        return topLayerToRender;
    }

    @JsonProperty
    public void setTopLayerToRender(int topLayerToRender) {
        this.topLayerToRender = topLayerToRender;
    }

    @JsonProperty
    public int getBottomLayerToRender() {
        return bottomLayerToRender;
    }

    @JsonProperty
    public void setBottomLayerToRender(int bottomLayerToRender) {
        this.bottomLayerToRender = bottomLayerToRender;
    }

    @JsonProperty
    public int getFirstSelectedLine() {
        return firstSelectedLine;
    }

    @JsonProperty
    public void setFirstSelectedLine(int firstSelectedLine) {
        this.firstSelectedLine = firstSelectedLine;
    }

    @JsonProperty
    public int getLastSelectedLine() {
        return lastSelectedLine;
    }

    @JsonProperty
    public void setLastSelectedLine(int lastSelectedLine) {
        this.lastSelectedLine = lastSelectedLine;
    }

    @JsonProperty
    public boolean getShowMoves() {
        return showMoves;
    }

    @JsonProperty
    public void setShowMoves(boolean showMoves) {
        this.showMoves = showMoves;
    }

    @JsonProperty
    public boolean getShowOnlySelected() {
        return showOnlySelected;
    }

    @JsonProperty
    public void setShowOnlySelected(boolean showOnlySelected) {
        this.showOnlySelected = showOnlySelected;
    }
    
    @JsonProperty
    public int getShowTools() {
        return showTools;
    }

    @JsonProperty
    public void setShowTools(int showTools) {
        this.showTools = showTools;
    }
    
    @JsonProperty
    public int getShowTypes() {
        return showTypes;
    }

    @JsonProperty
    public void setShowTypes(int showTypes) {
        this.showTypes = showTypes;
    }

    @JsonProperty
    public RenderParameters.ColourMode getColourMode() {
        return colourMode;
    }

    @JsonProperty
    public void setColourMode(RenderParameters.ColourMode colourMode) {
        this.colourMode = colourMode;
    }
}
