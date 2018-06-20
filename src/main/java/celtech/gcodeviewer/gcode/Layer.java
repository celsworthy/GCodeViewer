package celtech.gcodeviewer.gcode;

import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.LineEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a layer as described by G-Code.
 * Holds the {@link Entity} objects that will allow the layer to be displayed.
 *
 * @author George Salter
 */
public class Layer {
    
    private final List<Entity> entities = new ArrayList<>();
    private final List<LineEntity> lineEntities = new ArrayList<>();
    
    private final int layerNo;
    
    private boolean rendered = true;
    
    public Layer(int layerNo) {
        this.layerNo = layerNo;
    }
    
    public void addEntity(Entity entity) {
        entities.add(entity);
    }
    
    public List<Entity> getEntities() {
        return entities;
    }
    
    public void addLineEntity(LineEntity lineEntity) {
        lineEntities.add(lineEntity);
    }
    
    public List<LineEntity> getLineEntitys() {
        return lineEntities;
    }
    
    public int getLayerNo() {
        return this.layerNo;
    }
    
    public boolean isRendered() {
        return this.rendered;
    }
    
    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }
}
