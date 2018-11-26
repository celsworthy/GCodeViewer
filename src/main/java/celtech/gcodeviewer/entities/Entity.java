package celtech.gcodeviewer.entities;

import celtech.gcodeviewer.engine.RawModel;
import org.lwjgl.util.vector.Vector3f;

public class Entity {

    public final static int NULL_LAYER = -9999;
    
    private RawModel model;
    private Vector3f position;
    private Vector3f direction;
    private Vector3f normal;
    private float length;
    private float width;
    
    private Vector3f colour = new Vector3f(1, 1, 1);

    private int layer;
    private int lineNumber;
    private int toolNumber;
    private boolean isMoveFlag;

    public Entity(RawModel model, Vector3f position, Vector3f direction, Vector3f normal,
            float length, float width,
            int layer, int lineNumber, int toolNumber, boolean isMoveFlag) {
        this.model = model;
        this.position = position;
        this.direction = direction;
        this.normal = normal;
        this.length = length;
        this.width = width;
        this.layer = layer;
        this.lineNumber = lineNumber;
        this.toolNumber = toolNumber;
        this.isMoveFlag = isMoveFlag;
    }
    
    public RawModel getModel() {
        return model;
    }

    public void setModel(RawModel model) {
        this.model = model;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f direction) {
        this.direction = direction;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public void setNormal(Vector3f normal) {
        this.normal = normal;
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public Vector3f getColour() {
        return colour;
    }

    public void setColour(Vector3f colour) {
        this.colour = colour;
    }
    
    public int getLayer() {
        return (layer > NULL_LAYER ? layer : lineNumber);
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getToolNumber() {
        return toolNumber;
    }

    public void setToolNumber(int toolNumber) {
        this.toolNumber = toolNumber;
    }

    public boolean isMove() {
        return isMoveFlag;
    }

    public void setIsMove(boolean isMoveFlag) {
        this.isMoveFlag = isMoveFlag;
    }
}
