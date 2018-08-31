package celtech.gcodeviewer.entities;

import celtech.gcodeviewer.engine.RawModel;
import org.lwjgl.util.vector.Vector3f;

public class Entity {
    
    private RawModel model;
    private Vector3f position;
    
    private float rotationX;
    private float rotationY;
    private float rotationZ;
    
    private float scaleX;
    private float scaleY;
    private float scaleZ;
    
    private Vector3f colour = new Vector3f(1, 1, 1);

    public Entity(RawModel model, Vector3f position, float roatationX, float roatationY, float roatationZ, 
            float scaleX, float scaleY, float scaleZ) {
        this.model = model;
        this.position = position;
        this.rotationX = roatationX;
        this.rotationY = roatationY;
        this.rotationZ = roatationZ;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }
    
    public void increasePosition(float dx, float dy, float dz) {
        this.position = new Vector3f(position.getX() + dx, position.getY() + dy, position.getZ() + dz);
    }
    
    public void increaseRotation(float dx, float dy, float dz) {
        this.rotationX += dx;
        this.rotationY += dy;
        this.rotationZ += dz;
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

    public float getRotationX() {
        return rotationX;
    }

    public void setRotationX(float roatationX) {
        this.rotationX = roatationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public void setRotationY(float roatationY) {
        this.rotationY = roatationY;
    }

    public float getRotationZ() {
        return rotationZ;
    }

    public void setRotationZ(float roatationZ) {
        this.rotationZ = roatationZ;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }
    
    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }
    
    public float getScaleZ() {
        return scaleZ;
    }

    public void setScaleZ(float scaleZ) {
        this.scaleZ = scaleZ;
    }

    public Vector3f getColour() {
        return colour;
    }

    public void setColour(Vector3f colour) {
        this.colour = colour;
    }
}
