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
    private int firstLineToRender = 0;
    private int lastLineToRender = 0;
    
    private boolean showMoves = false;
    private boolean showTool0 = true;
    private boolean showTool1 = true;
    private ColourMode colourMode = ColourMode.COLOUR_AS_TOOL;
    private Vector3f moveColour = new Vector3f(0.0f, 0.0f, 1.0f);
    private List<Vector3f> toolColours = new ArrayList<>();
    private List<Vector3f> dataColourPalette = new ArrayList<>();
    private Vector3f defaultColour = new Vector3f(0.0f, 0.0f, 0.0f);

    RenderParameters(){
    }
    
    public void clearLinesAndLayer() {
        numberOfLines = 0;
        firstLineToRender = 0;
        lastLineToRender = 0;

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

    public int getFirstLineToRender() {
        return firstLineToRender;
    }

    public void setFirstLineToRender(int firstLineToRender) {
        this.firstLineToRender = firstLineToRender;
    }

    public int getLastLineToRender() {
        return lastLineToRender;
    }

    public void setLastLineToRender(int lastLineToRender) {
        this.lastLineToRender = lastLineToRender;
    }

    public boolean getShowMoves() {
        return showMoves;
    }

    public void setShowMoves(boolean showMoves) {
        this.showMoves = showMoves;
    }

    public boolean getShowTool0() {
        return showTool0;
    }

    public void setShowTool0(boolean showTool0) {
        this.showTool0 = showTool0;
    }

    public boolean getShowTool1() {
        return showTool1;
    }

    public void setShowTool1(boolean showTool1) {
        this.showTool1 = showTool1;
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
        int showFlags = (showMoves ? 1 : 0) 
                        + (showTool0 ? 2 : 0)
                        + (showTool1 ? 4 : 0);
        if (colourMode == ColourMode.COLOUR_AS_TYPE)
            showFlags += 8;
        return showFlags;
    }

    public void checkLimits() {
        if (bottomLayerToRender < indexOfBottomLayer)
            bottomLayerToRender = indexOfBottomLayer;
        if (topLayerToRender > indexOfTopLayer)
            topLayerToRender = indexOfTopLayer;
        if (topLayerToRender < bottomLayerToRender)
            topLayerToRender = bottomLayerToRender;

        if (firstLineToRender < 0)
            firstLineToRender = 0;
        if (lastLineToRender > numberOfLines)
            lastLineToRender = numberOfLines;
        if (lastLineToRender < firstLineToRender)
            lastLineToRender = firstLineToRender;
    }
}
