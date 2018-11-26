package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.utils.VectorUtils;
import celtech.gcodeviewer.gcode.GCodeLine;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Tony Aldhous
 */
public class GCodeVisualiser implements GCodeConsumer
{
    private final Stenographer steno = StenographerFactory.getStenographer(GCodeVisualiser.class.getName());
    private final double MINIMUM_STEP = 0.1;
    private final double MINIMUM_EXTRUSION = 0.0001;
    
    private class ToolState
    {
        public int toolNumber;
        public double currentB = 0.0;
        public double currentF = 0.0;
        
        public ToolState(int toolNumber)
        {
            this.toolNumber = toolNumber;
        }

        public void reset()
        {
            currentB = 0.0;
            currentF = 0.0;
        }
    }
    
    private final GCodeViewerConfiguration configuration;

    private double previousX = 0.0;
    private double previousY = 0.0;
    private double previousZ = 0.0;
    private double previousD = 0.0;
    private double previousE = 0.0;

    private double currentX = 0.0;
    private double currentY = 0.0;
    private double currentZ = 0.0;
    private double currentD = 0.0;
    private double currentE = 0.0;

    private ToolState t0 = new ToolState(0);
    private ToolState t1 = new ToolState(1);
    private ToolState currentTool = t0;
 
    boolean relativeMoves = false;
    private boolean relativeExtrusion = false;

    private int currentLayer = Entity.NULL_LAYER;
    private int currentLine = 0;
    private double currentHeight = 0;
    private String currentType = "";

    private RawModel lineModel;
    
    private List<Entity> lines = new ArrayList<>();
    
    static private final double RADIANS_TO_DEGREES = 57.2957795131;
    
    public GCodeVisualiser(RawModel lineModel, GCodeViewerConfiguration configuration)
    {
        this.lineModel = lineModel;
        this.configuration = configuration;
        this.relativeExtrusion = configuration.getRelativeExtrusionAsDefault();
    }

    public int getCurrentLayer()
    {
        return currentLayer;
    }
    
    public double getCurrentHeight()
    {
        return currentHeight;
    }
    
    public List<Entity> getLines()
    {
        return lines;
    }
    
    @Override
    public void reset()
    {
        previousX = 0.0;
        previousY = 0.0;
        previousZ = 0.0;
        previousD = 0.0;
        previousE = 0.0;

        currentX = 0.0;
        currentY = 0.0;
        currentZ = 0.0;
        currentD = 0.0;
        currentE = 0.0;
        currentTool = t0;
        t0.reset();
        t1.reset();
        
        relativeMoves = false;
        relativeExtrusion = configuration.getRelativeExtrusionAsDefault();

        currentLayer = Entity.NULL_LAYER;
        currentHeight = 0;
        currentType = "";
        
        lines.clear();
    }

    @Override
    public void processLine(GCodeLine line)
    {
        currentLine = line.lineNumber;
        
        switch (line.commandLetter)
        {
            case 'G':
                processGCode(line);
                break;

            case 'M':
                processMCode(line);
                break;

            case 'T':
                processTCode(line);
                break;
                
            default:
                processCommentLine(line);
        }
    }

    public void processGCode(GCodeLine line)
    {
        switch (line.commandNumber)
        {
            case 0:
            case 1:
                processMove(line);//Coordinated Movement
                break;

            case 28:
                processG28(line); // Home
                break;

            case 90:
                relativeMoves = false; // Use absolute coordinates on X Y Z axes.
                break;

            case 91:
                relativeMoves = true; // Use relative coordinates on X Y Z axes.
                break;

            case 92:
                processG92(line); // Set position to coordinates given.
                break;

            default:
                // Ignore code that does not affect visualisation.
                break;
        }
    }

    public void processMCode(GCodeLine line)
    {
        switch (line.commandNumber)
        {
            case 82:
                relativeExtrusion = false; // Use absolute coordinates on extruder axes.
                break;

            case 83:
                relativeExtrusion = true; // Use relative coordinates on extruder axes.
                break;

            default:
                // Ignore code that does not affect visualisation.
                break;
        }
    }

    public void processTCode(GCodeLine line)
    {
        switch (line.commandNumber)
        {
            case 0:
                currentTool = t0;
                processMove(line);
                break;

            case 1:
                currentTool = t1;
                processMove(line);
                break;

            default:
                // Ignore code that does not affect visualisation.
                break;
        }
    }

    public void processCommentLine(GCodeLine line)
    {
        if (line.layerNumber > Entity.NULL_LAYER &&
            line.layerNumber != currentLayer)
        {
            currentLayer = line.layerNumber;
        }
        if (!line.type.isEmpty() &&
            line.type != currentType)
        {
            currentType = line.type;
        }
    }
    
    public void processMove(GCodeLine line)
    {
        if (relativeMoves)
        {
            currentX += line.getValue('X', 0.0);
            currentY += line.getValue('Y', 0.0);
            currentZ += line.getValue('Z', 0.0);
        }
        else
        {
            currentX = line.getValue('X', currentX);
            currentY = line.getValue('Y', currentY);
            currentZ = line.getValue('Z', currentZ);
        }
        
        if (relativeExtrusion)
        {
            currentD += line.getValue('D', 0.0);
            currentE += line.getValue('E', 0.0);
        }
        else
        {
            currentD = line.getValue('D', currentD);
            currentE = line.getValue('E', currentE);
        }
        
        generateEntity();
    }

    public void processG28(GCodeLine line)
    {
        // Home axis.
        if (line.isValueSet('X'))
            currentX = 0.0;
        if (line.isValueSet('Y'))
            currentY = 0.0;
        if (line.isValueSet('Z'))
            currentZ = 0.0;
        if (line.isValueSet('D'))
            currentD = 0.0;
        if (line.isValueSet('E'))
            currentE = 0.0;
        generateEntity();
    }

    public void processG92(GCodeLine line)
    {
        // Set Axes to absolute position.
        if (line.isValueSet('X'))
        {
            currentX = line.getValue('X', currentX);
            previousX = currentX;
        }
        if (line.isValueSet('Y'))
        {
            currentY = line.getValue('Y', currentY);
            previousY = currentY;
        }
        if (line.isValueSet('Z'))
        {
            currentZ = line.getValue('Z', currentZ);
            previousZ = currentZ;
        }
        if (line.isValueSet('D'))
        {
            currentD = line.getValue('D', currentD);
            previousD = currentD;
        }
        if (line.isValueSet('E'))
        {
            currentE = line.getValue('E', currentE);
            previousE = currentE;
        }
    }
    
    public void generateEntity()
    {
        double deltaD = currentD - previousD;
        double deltaE = currentE - previousE;
        boolean isExtrusion = (Math.abs(deltaD) > MINIMUM_EXTRUSION ||
                              Math.abs(deltaE) > MINIMUM_EXTRUSION);

        Vector3f direction = new Vector3f((float)(currentX - previousX), (float)(currentY - previousY), (float)(currentZ - previousZ));
        float length = direction.length();
        if (length > MINIMUM_STEP)
        {
            direction.normalise();
            Vector3f normal = Vector3f.cross(direction, new Vector3f(0.0F, 0.0F, 1.0F), null);
            if (normal.length() < 0.1) // Close to vertical.
            {
                Vector3f nx = Vector3f.cross(direction, new Vector3f(1.0F, 0.0F, 0.0F), null);
                Vector3f ny = Vector3f.cross(direction, new Vector3f(0.0F, 1.0F, 0.0F), null);
                if (nx.length() >= ny.length())
                    normal = nx;
                else
                    normal = ny;
            }
            normal.normalise();

            float width = 0.1F;
            if (isExtrusion)
            {
                // Calculate volume of filament to extrude.
                double v = configuration.getFilamentFactor() * (deltaD >= deltaE ? deltaD : deltaE);
                
                // Calculate width of triangular block with the length of this segment with the equivalent volume.
                v *= 2.0 / length;
                if (v > 0.0)
                    width = (float)sqrt(v);
            }

            Vector3f toPosition = new Vector3f((float)(currentX), (float)(currentY), (float)(currentZ));
            Vector3f fromPosition = new Vector3f((float)(previousX), (float)(previousY), (float)(previousZ));
            Vector3f entityPosition = VectorUtils.calculateCenterBetweenVectors(fromPosition, toPosition);
            Entity entity = new Entity(lineModel, entityPosition, direction, normal,
                                       length, width,
                                       currentLayer, currentLine, currentTool.toolNumber, !isExtrusion);
            if (isExtrusion)
                entity.setColour(configuration.getColourForType(currentType));
            else
                entity.setColour(configuration.getColourForMove());

            lines.add(entity);
            
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
            previousD = currentD;
            previousE = currentE;
        }
        else if (isExtrusion)
        {
            // Probably a retraction. Ignore it for the moment.
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
            previousD = currentD;
            previousE = currentE;
        }
    }
}
