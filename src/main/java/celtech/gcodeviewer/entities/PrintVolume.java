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
    
    public PrintVolume(RawModel lineModel, float printVolumeWidth, 
            float printVolumeHeight, float printVolumeDepth) {
        this.lineModel = lineModel;
        this.printVolumeWidth = printVolumeWidth;
        this.printVolumeHeight = printVolumeHeight;
        this.printVolumeDepth = printVolumeDepth;
        createPrintVolumeLines();
    }
    
    private void createPrintVolumeLines() {
        LineEntity frontBottom = new LineEntity(lineModel, new Vector3f(0, 0, 0), new Vector3f(-printVolumeWidth, 0, 0));
        LineEntity backBottom = new LineEntity(lineModel, new Vector3f(0, 0, printVolumeDepth), new Vector3f(-printVolumeWidth, 0, printVolumeDepth));
        LineEntity leftBottom = new LineEntity(lineModel, new Vector3f(0, 0, 0), new Vector3f(0, 0, printVolumeDepth));
        LineEntity rightBottom = new LineEntity(lineModel, new Vector3f(-printVolumeWidth, 0, 0), new Vector3f(-printVolumeWidth, 0, printVolumeDepth));
        
        LineEntity frontTop = new LineEntity(lineModel, new Vector3f(0, printVolumeHeight, 0), new Vector3f(-printVolumeWidth, printVolumeHeight, 0));
        LineEntity backTop = new LineEntity(lineModel, new Vector3f(0, printVolumeHeight, printVolumeDepth), new Vector3f(-printVolumeWidth, printVolumeHeight, printVolumeDepth));
        LineEntity leftTop = new LineEntity(lineModel, new Vector3f(0, printVolumeHeight, 0), new Vector3f(0, printVolumeHeight, printVolumeDepth));
        LineEntity rightTop = new LineEntity(lineModel, new Vector3f(-printVolumeWidth, printVolumeHeight, 0), new Vector3f(-printVolumeWidth, printVolumeHeight, printVolumeDepth));
        
        LineEntity frontLeft = new LineEntity(lineModel, new Vector3f(0, 0, 0), new Vector3f(0, printVolumeHeight, 0));
        LineEntity backLeft = new LineEntity(lineModel, new Vector3f(0, 0, printVolumeDepth), new Vector3f(0, printVolumeHeight, printVolumeDepth));
        LineEntity frontRight = new LineEntity(lineModel, new Vector3f(-printVolumeWidth, 0, 0), new Vector3f(-printVolumeWidth, printVolumeHeight, 0));
        LineEntity backRight = new LineEntity(lineModel, new Vector3f(-printVolumeWidth, 0, printVolumeDepth), new Vector3f(-printVolumeWidth, printVolumeHeight, printVolumeDepth));
        
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
