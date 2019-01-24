package celtech.gcodeviewer.entities;

import celtech.gcodeviewer.engine.RawModel;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

/**
 *
 * @author George Salter
 */
public class PrintVolume {
    
    private final List<LineEntity> lineEntities = new ArrayList<>();
    
    private final RawModel lineModel;
    
    private final float printVolumeWidth;
    private final float printVolumeHeight;
    private final float printVolumeDepth;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    
    public PrintVolume(RawModel lineModel, float printVolumeWidth, 
            float printVolumeHeight, float printVolumeDepth,
            float xOffset, float yOffset, float zOffset) {
        this.lineModel = lineModel;
        this.printVolumeWidth = printVolumeWidth;
        this.printVolumeHeight = printVolumeHeight;
        this.printVolumeDepth = printVolumeDepth;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;

        createPrintVolumeLines();
    }
    
    private void createPrintVolumeLines() {
        LineEntity frontBottom = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset, zOffset),
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset));
        LineEntity backBottom = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset, zOffset + printVolumeDepth),
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset + printVolumeDepth));
        LineEntity leftBottom = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset, zOffset),
                new Vector3f(xOffset, yOffset, zOffset + printVolumeDepth));
        LineEntity rightBottom = new LineEntity(lineModel,
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset),
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset + printVolumeDepth));
        
        LineEntity frontTop = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset),
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset));
        LineEntity backTop = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset + printVolumeDepth),
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset + printVolumeDepth));
        LineEntity leftTop = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset),
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset + printVolumeDepth));
        LineEntity rightTop = new LineEntity(lineModel,
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset),
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset + printVolumeDepth));
        
        LineEntity frontLeft = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset, zOffset),
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset));
        LineEntity backLeft = new LineEntity(lineModel,
                new Vector3f(xOffset, yOffset, zOffset + printVolumeDepth),
                new Vector3f(xOffset, yOffset + printVolumeHeight, zOffset + printVolumeDepth));
        LineEntity frontRight = new LineEntity(lineModel,
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset),
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset));
        LineEntity backRight = new LineEntity(lineModel,
                new Vector3f(xOffset - printVolumeWidth, yOffset, zOffset + printVolumeDepth),
                new Vector3f(xOffset - printVolumeWidth, yOffset + printVolumeHeight, zOffset + printVolumeDepth));
        
        lineEntities.add(frontBottom);
        lineEntities.add(backBottom);
        lineEntities.add(leftBottom);
        lineEntities.add(rightBottom);
        lineEntities.add(frontTop);
        lineEntities.add(backTop);
        lineEntities.add(leftTop);
        lineEntities.add(rightTop);
        lineEntities.add(frontLeft);
        lineEntities.add(backLeft);
        lineEntities.add(frontRight);
        lineEntities.add(backRight);
        
        lineEntities.forEach(lineEntity -> lineEntity.setColour(new Vector3f(0, 0, 0)));
    }
    
    public List<LineEntity> getLineEntities() {
        return lineEntities;
    }
}
