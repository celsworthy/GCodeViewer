package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.utils.VectorUtils;
import celtech.gcodeviewer.gcode.GCodeLine;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    
    private final GCodeViewerConfiguration configuration;
    private final RenderParameters renderParameters;

    private double previousX = 0.0;
    private double previousY = 0.0;
    private double previousZ = 0.0;
    private double previousA = 0.0;
    private double previousB = 0.0;
    private double previousD = 0.0;
    private double previousE = 0.0;
    private double previousF = 0.0;

    private double currentX = 0.0;
    private double currentY = 0.0;
    private double currentZ = 0.0;
    private double currentA = 0.0;
    private double currentB = 0.0;
    private double currentD = 0.0;
    private double currentE = 0.0;
    private double currentF = 0.0;

    private double minX = 0.0;
    private double minY = 0.0;
    private double minZ = 0.0;
    private double minA = 0.0;
    private double minB = 0.0;
    private double minD = 0.0;
    private double minE = 0.0;
    private double minF = 0.0;

    private double maxX = 0.0;
    private double maxY = 0.0;
    private double maxZ = 0.0;
    private double maxA = 0.0;
    private double maxB = 0.0;
    private double maxD = 0.0;
    private double maxE = 0.0;
    private double maxF = 0.0;

    private int currentTool = 0;
    private Set<Integer> toolSet = new HashSet<>();
 
    boolean relativeMoves = false;
    private boolean relativeExtrusion = false;

    private int currentLayer = Entity.NULL_LAYER;
    private int currentLine = 0;
    private double currentHeight = 0;
    private String currentType = "";
    private Set<String> typeSet = new HashSet<>();

    private RawModel lineModel;
    
    // Moves a rendered separately from lines.
    private List<Entity> segments = new ArrayList<>();
    private List<Entity> moves = new ArrayList<>();
    
    static private final double RADIANS_TO_DEGREES = 57.2957795131;
    
    public GCodeVisualiser(RawModel lineModel, RenderParameters renderParameters, GCodeViewerConfiguration configuration)
    {
        this.lineModel = lineModel;
        this.configuration = configuration;
        this.renderParameters = renderParameters;
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
    
    public List<Entity> getSegments()
    {
        return segments;
    }
    
    public List<Entity> getMoves()
    {
        return moves;
    }

    public Set<Integer> getToolSet()
    {
        // If the toolSet is empty when
        // retrieved, then no tool was selected.
        // The default tool is tool 0, so add
        // it to the toolSet.
        if (toolSet.size() == 0)
            toolSet.add(currentTool);
        return toolSet;
    }

    public Set<String> getTypeSet()
    {
        return typeSet;
    }

    @Override
    public void reset()
    {
        previousX = 0.0;
        previousY = 0.0;
        previousZ = 0.0;
        previousA = 0.0;
        previousB = 0.0;
        previousD = 0.0;
        previousE = 0.0;
        previousF = 0.0;

        currentX = 0.0;
        currentY = 0.0;
        currentZ = 0.0;
        currentA = 0.0;
        currentB = 0.0;
        currentD = 0.0;
        currentE = 0.0;
        currentF = 0.0;

        currentTool = 0;
        toolSet.clear();
        
        relativeMoves = false;
        relativeExtrusion = configuration.getRelativeExtrusionAsDefault();

        currentLayer = Entity.NULL_LAYER;
        currentHeight = 0;
        currentType = "";
        typeSet.clear();
        
        segments.clear();
        moves.clear();
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
        currentTool = line.commandNumber;
        toolSet.add(currentTool);
        processMove(line);
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
            typeSet.add(currentType);
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
        
        // The rest are always absolute?
        currentA = line.getValue('A', currentA);
        currentB = line.getValue('B', currentB);
        currentF = line.getValue('F', currentF);
        
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
        if (line.isValueSet('A'))
            currentA = 0.0;
        if (line.isValueSet('B'))
            currentB = 0.0;
        if (line.isValueSet('D'))
            currentD = 0.0;
        if (line.isValueSet('E'))
            currentE = 0.0;
        if (line.isValueSet('F'))
            currentF = 0.0;
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
        if (line.isValueSet('A'))
        {
            currentA = line.getValue('A', currentA);
            previousA = currentA;
        }
        if (line.isValueSet('B'))
        {
            currentB = line.getValue('B', currentB);
            previousB = currentB;
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
        if (line.isValueSet('F'))
        {
            currentF = line.getValue('F', currentF);
            previousF = currentF;
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
                double v = renderParameters.getFilamentFactorForTool(currentTool) * (deltaD >= deltaE ? deltaD : deltaE);
                
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
                                       currentLayer, currentLine, currentTool, !isExtrusion, null);
            if (isExtrusion)
            {
                entity.setType(currentType);
                Vector3f typeColour = configuration.getColourForType(currentType);
                entity.setTypeColour(typeColour);
                entity.setColour(typeColour);
                entity.setDataValue(0, (float)currentA);
                entity.setDataValue(1, (float)currentB);
                entity.setDataValue(2, (float)currentD);
                entity.setDataValue(3, (float)currentE);
                entity.setDataValue(4, (float)currentF);
                segments.add(entity);
            }
            else
            {
                entity.setColour(null);
                entity.setTypeColour(null);
                entity.setDataValues(null);
                moves.add(entity);
            }

            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
            previousA = currentA;
            previousB = currentB;
            previousD = currentD;
            previousE = currentE;
            previousF = currentF;
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
        minX = min(currentX, minX);
        minY = min(currentY, minX);
        minZ = min(currentZ, minX);
        minA = min(currentA, minX);
        minB = min(currentB, minX);
        minD = min(currentD, minX);
        minE = min(currentE, minX);
        minF = min(currentF, minX);

        maxX = max(currentX, maxX);
        maxY = max(currentY, maxX);
        maxZ = max(currentZ, maxX);
        maxA = max(currentA, maxX);
        maxB = max(currentB, maxX);
        maxD = max(currentD, maxX);
        maxE = max(currentE, maxX);
        maxF = max(currentF, maxX);
    }
}
