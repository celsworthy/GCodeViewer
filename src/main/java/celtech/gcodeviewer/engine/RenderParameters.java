/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.util.vector.Vector3f;

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
    private Vector3f defaultColour = new Vector3f(0.0f, 0.0f, 0.0f);
    private List<Double> toolFilamentFactors = new ArrayList<>();
    private double defaultFilamentFactor = 0.0;
    private int displayWidth = 0;
    private int displayHeight = 0;
    private int windowWidth = 0;
    private int windowHeight = 0;

    RenderParameters(){
    }
    
    public void setFromConfiguration(GCodeViewerConfiguration configuration) {
        moveColour = configuration.getMoveColour();
        selectColour = configuration.getSelectColour();
        toolColours = configuration.getToolColours();
        dataColourPalette = configuration.getDataColourPalette();
        defaultColour = configuration.getDefaultColour();
        toolFilamentFactors = configuration.getToolFilamentFactors();
        defaultFilamentFactor = configuration.getDefaultFilamentFactor();
    }
            
    public void clearLinesAndLayer() {
        numberOfLines = 0;
        firstSelectedLine = 0;
        lastSelectedLine = 0;

        indexOfTopLayer = 0;
        indexOfBottomLayer = 0;
        topLayerToRender = 0;
        bottomLayerToRender = 0;
    }

    public int getIndexOfTopLayer() {
        return indexOfTopLayer;
    }

    public void setIndexOfTopLayer(int indexOfTopLayer) {
        this.indexOfTopLayer = indexOfTopLayer;
    }

    public int getIndexOfBottomLayer() {
        return indexOfBottomLayer;
    }

    public void setIndexOfBottomLayer(int indexOfBottomLayer) {
        this.indexOfBottomLayer = indexOfBottomLayer;
    }

    public int getTopLayerToRender() {
        return topLayerToRender;
    }

    public void setTopLayerToRender(int topLayerToRender) {
        this.topLayerToRender = topLayerToRender;
    }

    public int getBottomLayerToRender() {
        return bottomLayerToRender;
    }

    public void setBottomLayerToRender(int bottomLayerToRender) {
        this.bottomLayerToRender = bottomLayerToRender;
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public void setNumberOfLines(int numberOfLines) {
        this.numberOfLines = numberOfLines;
    }

    public int getFirstSelectedLine() {
        return firstSelectedLine;
    }

    public void setFirstSelectedLine(int firstSelectedLine) {
        this.firstSelectedLine = firstSelectedLine;
    }

    public int getLastSelectedLine() {
        return lastSelectedLine;
    }

    public void setLastSelectedLine(int lastSelectedLine) {
        this.lastSelectedLine = lastSelectedLine;
    }

    public boolean getShowMoves() {
        return showMoves;
    }

    public void setShowMoves(boolean showMoves) {
        this.showMoves = showMoves;
    }

    public boolean getShowOnlySelected() {
        return showOnlySelected;
    }

    public void setShowOnlySelected(boolean showOnlySelected) {
        this.showOnlySelected = showOnlySelected;
    }

    public ColourMode getColourMode() {
        return colourMode;
    }

    public void setColourMode(ColourMode colourMode) {
        this.colourMode = colourMode;
    }
    
    public Vector3f getMoveColour() {
        return moveColour;
    }

    public void setMoveColour(Vector3f moveColour) {
        this.moveColour = moveColour;
    }
    
    public Vector3f getSelectColour() {
        return selectColour;
    }

    public void getSelectColour(Vector3f selectColour) {
        this.selectColour = selectColour;
    }
    
    public List<Vector3f> getToolColours() {
        return toolColours;
    }

    public void setToolColours(List<Vector3f> toolColours) {
        this.toolColours = toolColours;
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
    }

    public List<Double> getToolFilamentFactors() {
        return toolFilamentFactors;
    }

    public void setToolFilamentFactors(List<Double> toolFilamentFactors) {
        this.toolFilamentFactors = toolFilamentFactors;
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
    }

    public List<Vector3f> getDataColourPalette() {
        return dataColourPalette;
    }

    public void setDataColourPalette(List<Vector3f> dataColourPalette) {
        this.dataColourPalette = dataColourPalette;
    }

    public Vector3f getDefaultColour() {
        return defaultColour;
    }

    public void setDefaultColour(Vector3f defaultColour) {
        this.defaultColour = defaultColour;
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
        this.showTools = showTools;
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
            if (showFlag)
                showTools |= toolFlag;
            else
                showTools &= ~toolFlag;
        }
    }

    public void checkLimits() {
        if (bottomLayerToRender < indexOfBottomLayer)
            bottomLayerToRender = indexOfBottomLayer;
        if (topLayerToRender > indexOfTopLayer)
            topLayerToRender = indexOfTopLayer;
        if (topLayerToRender < bottomLayerToRender)
            topLayerToRender = bottomLayerToRender;

        if (firstSelectedLine < 0)
            firstSelectedLine = 0;
        if (lastSelectedLine > numberOfLines)
            lastSelectedLine = numberOfLines;
        if (lastSelectedLine < firstSelectedLine)
            lastSelectedLine = firstSelectedLine;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayWidth(int displayWidth) {
        this.displayWidth = displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public void setDisplayHeight(int displayHeight) {
        this.displayHeight = displayHeight;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }
}
