/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodeviewer.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

/**
 *
 * @author Tony
 */
public class RenderParameters {

    public static enum ColourMode {
        COLOUR_AS_TYPE,
        COLOUR_AS_TOOL;
    }
      
    private int indexOfTopLayer = 0;
    private int indexOfBottomLayer = 0;
    private int topLayerToRender = 0;
    private int bottomLayerToRender = 0;
    
    private int numberOfLines = 0;
    private int firstSelectedLine = 0;
    private int lastSelectedLine = 0;
    
    private boolean showMoves = false;
    private boolean showOnlySelected = false;
    private int showTools = 0xFFFF; 
    private ColourMode colourMode = ColourMode.COLOUR_AS_TOOL;
    private Vector3f moveColour = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f selectColour = new Vector3f(0.0f, 0.0f, 0.0f);
    private List<Vector3f> toolColours = new ArrayList<>();
    private List<Vector3f> dataColourPalette = new ArrayList<>();
    private List<Vector3f> typeColours = new ArrayList<>();
    private Map<String, Vector3f> typeColourMap = new HashMap<>();
    private Vector3f defaultColour = new Vector3f(0.0f, 0.0f, 0.0f);
    private List<Double> toolFilamentFactors = new ArrayList<>();
    private double defaultFilamentFactor = 0.0;
    private int displayWidth = 0;
    private int displayHeight = 0;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private int windowXPos = 0;
    private int windowYPos = 0;
    private double frameTime = 0.0;

    // Nuklear GUI requires 2 renders to update properly - the first updates the state
    // the second updates the GUI. Easy way to do this is it always set the render flag
    // to 2, and decrement by one when cleared.
    private int renderRequired = 2;

    RenderParameters(){
    }
    
    public void setFromConfiguration(GCodeViewerConfiguration configuration) {
        moveColour = configuration.getMoveColour();
        selectColour = configuration.getSelectColour();
        toolColours = configuration.getToolColours();
        typeColourMap = configuration.getTypeColourMap();
        dataColourPalette = configuration.getDataColourPalette();
        defaultColour = configuration.getDefaultColour();
        toolFilamentFactors = configuration.getToolFilamentFactors();
        defaultFilamentFactor = configuration.getDefaultFilamentFactor();
    }
            
    public void setFromGUIConfiguration(GCodeViewerGUIConfiguration guiConfiguration) {
        this.topLayerToRender = guiConfiguration.getTopLayerToRender();
        this.bottomLayerToRender = guiConfiguration.getBottomLayerToRender();
        this.firstSelectedLine = guiConfiguration.getFirstSelectedLine();
        this.lastSelectedLine = guiConfiguration.getLastSelectedLine();
        this.showMoves = guiConfiguration.getShowMoves();
        this.showOnlySelected = guiConfiguration.getShowOnlySelected();
        this.showTools = guiConfiguration.getShowTools(); 
        this.colourMode = guiConfiguration.getColourMode();
    }

    public void saveToGUIConfiguration(GCodeViewerGUIConfiguration guiConfiguration) {
        guiConfiguration.setTopLayerToRender(this.topLayerToRender);
        guiConfiguration.setBottomLayerToRender(this.bottomLayerToRender);
        guiConfiguration.setFirstSelectedLine(this.firstSelectedLine);
        guiConfiguration.setLastSelectedLine(this.lastSelectedLine);
        guiConfiguration.setShowMoves(this.showMoves);
        guiConfiguration.setShowOnlySelected(this.showOnlySelected);
        guiConfiguration.setShowTools(this.showTools); 
        guiConfiguration.setColourMode(this.colourMode);
    }

    public void clearLinesAndLayer() {
        numberOfLines = 0;
        firstSelectedLine = 0;
        lastSelectedLine = 0;

        indexOfTopLayer = 0;
        indexOfBottomLayer = 0;
        topLayerToRender = 0;
        bottomLayerToRender = 0;
        frameTime = 0.0;
        renderRequired = 2;
    }

    public int getIndexOfTopLayer() {
        return indexOfTopLayer;
    }

    public void setIndexOfTopLayer(int indexOfTopLayer) {
        this.indexOfTopLayer = indexOfTopLayer;
        renderRequired = 2;
    }

    public int getIndexOfBottomLayer() {
        return indexOfBottomLayer;
    }

    public void setIndexOfBottomLayer(int indexOfBottomLayer) {
        this.indexOfBottomLayer = indexOfBottomLayer;
        renderRequired = 2;
    }

    public int getTopLayerToRender() {
        return topLayerToRender;
    }

    public void setTopLayerToRender(int topLayerToRender) {
        if (this.topLayerToRender != topLayerToRender) {
            if (topLayerToRender > this.bottomLayerToRender)
                this.topLayerToRender = topLayerToRender;
            else
                this.topLayerToRender = this.bottomLayerToRender;
            renderRequired = 2;
        }
    }

    public int getBottomLayerToRender() {
        return bottomLayerToRender;
    }

    public void setBottomLayerToRender(int bottomLayerToRender) {
        if (this.bottomLayerToRender != bottomLayerToRender) {
            if (bottomLayerToRender < this.topLayerToRender)
                this.bottomLayerToRender = bottomLayerToRender;
            else
                this.bottomLayerToRender = this.topLayerToRender;
            renderRequired = 2;
        }
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
        renderRequired = 2;
    }

    public int getFirstSelectedLine() {
        return firstSelectedLine;
    }

    public void setFirstSelectedLine(int firstSelectedLine) {
        if (this.firstSelectedLine != firstSelectedLine) {
            this.firstSelectedLine = firstSelectedLine;
            renderRequired = 2;
        }
    }

    public int getLastSelectedLine() {
        return lastSelectedLine;
    }

    public void setLastSelectedLine(int lastSelectedLine) {
        if (this.lastSelectedLine != lastSelectedLine) {
            this.lastSelectedLine = lastSelectedLine;
            renderRequired = 2;
        }
    }

    public boolean getShowMoves() {
        return showMoves;
    }

    public void setShowMoves(boolean showMoves) {
        if (this.showMoves != showMoves) {
            this.showMoves = showMoves;
            renderRequired = 2;
        }
    }

    public boolean getShowOnlySelected() {
        return showOnlySelected;
    }

    public void setShowOnlySelected(boolean showOnlySelected) {
        if (this.showOnlySelected != showOnlySelected) {
            this.showOnlySelected = showOnlySelected;
            renderRequired = 2;
        }
    }

    public ColourMode getColourMode() {
        return colourMode;
    }

    public void setColourMode(ColourMode colourMode) {
        if (this.colourMode != colourMode) {
            this.colourMode = colourMode;
            renderRequired = 2;
        }
    }
    
    public Vector3f getMoveColour() {
        return moveColour;
    }

    public void setMoveColour(Vector3f moveColour) {
        this.moveColour = moveColour;
        renderRequired = 2;
    }
    
    public Vector3f getSelectColour() {
        return selectColour;
    }

    public void getSelectColour(Vector3f selectColour) {
        this.selectColour = selectColour;
        renderRequired = 2;
    }
    
    public List<Vector3f> getToolColours() {
        return toolColours;
    }

    public void setToolColours(List<Vector3f> toolColours) {
        this.toolColours = toolColours;
        renderRequired = 2;
    }
    
    public Vector3f getColourForTool(int toolNumber) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            return toolColours.get(toolNumber);
        else
            return defaultColour;
    }

    public void setColourForTool(int toolNumber, Vector3f c) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            toolColours.set(toolNumber, c);
        renderRequired = 2;
    }

    public Map<String, Vector3f> getTypeColourMap() {
        return typeColourMap;
    }

    public void setTypeColourMap(Map<String, Vector3f> typeColourMap) {
        this.typeColourMap = typeColourMap;
        renderRequired = 2;
    }
    
    public Vector3f getColourForType(String type) {
        return typeColourMap.getOrDefault(type, defaultColour);
    }

    public void setColourForType(String type, Vector3f c) {
        typeColourMap.put(type, c);
        renderRequired = 2;
    }

    public List<Double> getToolFilamentFactors() {
        return toolFilamentFactors;
    }

    public void setToolFilamentFactors(List<Double> toolFilamentFactors) {
        this.toolFilamentFactors = toolFilamentFactors;
        renderRequired = 2;
    }
    
    public double getFilamentFactorForTool(int toolNumber) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            return toolFilamentFactors.get(toolNumber);
        else
            return defaultFilamentFactor;
    }

    public void setFilamentFactorForTool(int toolNumber, double filamentFactor) {
        if (toolNumber >= 0 && toolNumber < toolColours.size())
            toolFilamentFactors.set(toolNumber, filamentFactor);
        renderRequired = 2;
    }

    public List<Vector3f> getDataColourPalette() {
        return dataColourPalette;
    }

    public void setDataColourPalette(List<Vector3f> dataColourPalette) {
        this.dataColourPalette = dataColourPalette;
        renderRequired = 2;
    }

    public Vector3f getDefaultColour() {
        return defaultColour;
    }

    public void setDefaultColour(Vector3f defaultColour) {
        this.defaultColour = defaultColour;
        renderRequired = 2;
    }
    
    public int getShowFlags() {
        int showFlags = (showMoves ? 1 : 0);
        if (colourMode == ColourMode.COLOUR_AS_TYPE)
            showFlags += 2;
        if (showOnlySelected)
            showFlags += 4;
        return showFlags;
    }

    public int getShowTools() {
        return showTools;
    }

    public void setShowTools(int showTools) {
        if (this.showTools != showTools) {
            this.showTools = showTools;
            renderRequired = 2;
        }
    }

    public boolean getShowFlagForTool(int toolIndex) {
        boolean showTool = false;
        if (toolIndex >= 0 && toolIndex < 16)
        {
            int toolFlag = 1 << toolIndex;
            showTool = ((showTools & toolFlag) == toolFlag);
        }
        return showTool;
    }

    public void setShowFlagForTool(int toolIndex, boolean showFlag) {
        if (toolIndex >= 0 && toolIndex < 16)
        {
            int toolFlag = 1 << toolIndex;
            if (showFlag != ((showTools & toolFlag) == toolFlag)) {
                if (showFlag)
                    showTools |= toolFlag;
                else
                    showTools &= ~toolFlag;
                renderRequired = 2;
            }
        }
    }

    public void checkLimits() {
        if (bottomLayerToRender < indexOfBottomLayer) {
            bottomLayerToRender = indexOfBottomLayer;
            renderRequired = 2;
        }

        if (topLayerToRender > indexOfTopLayer) {
            topLayerToRender = indexOfTopLayer;
            renderRequired = 2;
        }
        if (topLayerToRender < bottomLayerToRender) {
            topLayerToRender = bottomLayerToRender;
            renderRequired = 2;
        }

        if (firstSelectedLine < 0) {
            firstSelectedLine = 0;
            renderRequired = 2;
        }
        if (lastSelectedLine > numberOfLines) {
            lastSelectedLine = numberOfLines;
            renderRequired = 2;
        }
        if (lastSelectedLine < firstSelectedLine) {
            lastSelectedLine = firstSelectedLine;
            renderRequired = 2;
        }
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(int displayWidth) {
        if (this.displayWidth != displayWidth) {
            this.displayWidth = displayWidth;
            renderRequired = 2;
        }
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight;
        if (this.displayHeight != displayHeight) {
            this.displayHeight = displayHeight;
            renderRequired = 2;
        }
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        
        if (this.windowWidth != windowWidth) {
            this.windowWidth = windowWidth;
            renderRequired = 2;
        }
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        if (this.windowHeight != windowHeight) {
            this.windowHeight = windowHeight;
            renderRequired = 2;
        }
    }
    
    public int getWindowXPos() {
        return windowXPos;
    }

    public void setWindowXPos(int windowXPos) {
        
        if (this.windowXPos != windowXPos) {
            this.windowXPos = windowXPos;
        }
    }

    public int setWindowYPos() {
        return windowYPos;
    }

    public void setWindowYPos(int windowYPos) {
        if (this.windowYPos != windowYPos) {
            this.windowYPos = windowYPos;
        }
    }

    public double getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(double frameTime) {
        if (this.frameTime != frameTime) {
            this.frameTime = frameTime;
            renderRequired = 2;
        }
    }

    public void setRenderRequired() {
        renderRequired = 2;
    }

    public void clearRenderRequired() {
        if (renderRequired > 0)
            --renderRequired;
    }

    public boolean getRenderRequired() {
        return renderRequired > 0;
    }
}
