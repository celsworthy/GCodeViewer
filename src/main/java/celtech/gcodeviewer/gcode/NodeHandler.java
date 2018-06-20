package celtech.gcodeviewer.gcode;

import celtech.gcodeviewer.engine.RawModel;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.LineEntity;
import celtech.gcodeviewer.utils.VectorUtils;
import celtech.roboxbase.postprocessor.nouveau.nodes.CommentNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ExtrusionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.FillSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.InnerPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerChangeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionDuringTravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OrphanObjectDelineationNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OuterPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SkinSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Movement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.util.vector.Vector3f;

/**
 * Handles the nodes produced as a result of parsing G-Code
 * 
 * @author George Salter
 */
public class NodeHandler {
    
    private Vector3f previousPosition = new Vector3f(0, 0, 0);
    private float currentLayerHeight = 0;
    private float extrusionWidth = 0;
    
    private final RawModel model;
    private final RawModel lineModel;
    
    public NodeHandler(RawModel model, RawModel lineModel) {
        this.model = model;
        this.lineModel = lineModel;
    }
    
    public List<Layer> processLayerNodes(List<LayerNode> layerNodes) {
        List<Layer> layers = new ArrayList<>();
        
        layerNodes.forEach((layerNode) -> {
            Layer layer = new Layer(layerNode.getLayerNumber());
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
            
            while (layerIterator.hasNext()) {
                GCodeEventNode node = layerIterator.next();
                
                if (node instanceof ExtrusionNode) {
                    Entity extrusionEntity = processExtrusionNode((ExtrusionNode) node);
                    layer.addEntity(extrusionEntity);
                } else if (node instanceof TravelNode) {
                    LineEntity travelEntity = processTravelNode((TravelNode) node);
                    layer.addLineEntity(travelEntity);
                } else if (node instanceof LayerChangeDirectiveNode) {
                    LineEntity layerChangeEntity = processLayerChangeDirectiveNode((LayerChangeDirectiveNode) node);
                    layer.addLineEntity(layerChangeEntity);
                } else if (node instanceof NozzleValvePositionDuringTravelNode) {
                    Entity nozzleValvePositionDuringTravelEntity = processNozzleValvePositionDuringTravelNode((NozzleValvePositionDuringTravelNode) node);
                    layer.addEntity(nozzleValvePositionDuringTravelEntity);
                } else if (node instanceof NozzleValvePositionNode) {
                    processNozzleValvePositionNode((NozzleValvePositionNode) node);
                } else if (node instanceof MCodeNode) {
                    processMCodeNode((MCodeNode) node);
                } else if (node instanceof CommentNode) {
                    processCommentNode((CommentNode) node);
                } else if (node instanceof OrphanObjectDelineationNode) {
                    processOrphanObjectDelineationNode((OrphanObjectDelineationNode) node);
                } else if (node instanceof InnerPerimeterSectionNode) {
                    processInnerPerimeterSectionNode((InnerPerimeterSectionNode) node);
                } else if (node instanceof OuterPerimeterSectionNode) {
                    processOuterPerimeterSectionNode((OuterPerimeterSectionNode) node);
                } else if (node instanceof SkinSectionNode) {
                    processSkinSectionNode((SkinSectionNode) node);
                } else if (node instanceof FillSectionNode) {
                    processFillSectionNode((FillSectionNode) node);
                } else {
                    System.out.println(node.getClass().getName());
                }
            }
            
            layers.add(layer);
        });
        
        return layers;
    }
        
    private Entity processExtrusionNode(ExtrusionNode extrusionNode) {
        
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = extrusionNode.getMovement();
        float toX = (float) -toMove.getX();
        float toY;
        float toZ = (float) toMove.getY();
        if(toMove.isZSet()) {
            toY = (float) toMove.getZ();
            currentLayerHeight = (float) toMove.getZ();
        } else {
            toY = currentLayerHeight;
        }
        Vector3f toPosition = new Vector3f(toX, toY, toZ);
        Vector3f fromPosition = previousPosition;

        float length = VectorUtils.calculateLengthBetweenVectors(fromPosition, toPosition);
        float angleAroundY = VectorUtils.calculateRotationAroundYOfVectors(fromPosition, toPosition);
        float angleAroundZ = VectorUtils.calculateRotationAroundZOfVectors(fromPosition, toPosition);
        Vector3f entityPosition = VectorUtils.calculateCenterBetweenVectors(fromPosition, toPosition);
        
        Entity extrusionEntity = new Entity(model, entityPosition, 0, angleAroundY, angleAroundZ, length, extrusionWidth, extrusionWidth);
        extrusionEntity.setColour(new Vector3f(0.2f, 0.8f, 1.0f));
        
        previousPosition = new Vector3f(toX, toY, toZ);
        
        return extrusionEntity;
    }
    
    private LineEntity processTravelNode(TravelNode travelNode) {
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = travelNode.getMovement();
        float toX = (float) -toMove.getX();
        float toY;
        float toZ = (float) toMove.getY();
        if(toMove.isZSet()) {
            toY = (float) toMove.getZ();
            currentLayerHeight = (float) toMove.getZ();
        } else {
            toY = currentLayerHeight;
        }
        Vector3f toPosition = new Vector3f(toX, toY, toZ);
        Vector3f fromPosition = previousPosition;
        
        LineEntity travelEntity = new LineEntity(lineModel, fromPosition, toPosition);
        travelEntity.setColour(new Vector3f(1.0f, 0.9f, 1.0f));
        
        previousPosition = new Vector3f(toX, toY, toZ);
        
        return travelEntity;
    }
    
    private LineEntity processLayerChangeDirectiveNode(LayerChangeDirectiveNode layerChangeDirectiveNode) {
        // Switch Z and Y for the OpenGL coordinate system
        Movement toMove = layerChangeDirectiveNode.getMovement();
        float toX = (float) -toMove.getX();
        float toY;
        float toZ = (float) toMove.getY();
        if(toMove.isZSet()) {
            toY = (float) toMove.getZ();
            currentLayerHeight = (float) toMove.getZ();
            if(extrusionWidth == 0) {
                extrusionWidth = currentLayerHeight;
            }
        } else {
            toY = currentLayerHeight;
        }
        Vector3f toPosition = new Vector3f(toX, toY, toZ);
        Vector3f fromPosition = previousPosition;
          
        LineEntity layerChangeEntity = new LineEntity(lineModel, fromPosition, toPosition);
        layerChangeEntity.setColour(new Vector3f(0.6f, 1.0f, 0.6f));
        
        previousPosition = new Vector3f(toX, toY, toZ);
        
        return layerChangeEntity;
    }
    
    private Entity processNozzleValvePositionDuringTravelNode(NozzleValvePositionDuringTravelNode nozzleValvePositionDuringTravelNode) {
        Movement toMove = nozzleValvePositionDuringTravelNode.getMovement();
        float toX = (float) -toMove.getX();
        float toY;
        float toZ = (float) toMove.getY();
        if(toMove.isZSet()) {
            toY = (float) toMove.getZ();
            currentLayerHeight = (float) toMove.getZ();
            if(extrusionWidth == 0) {
                extrusionWidth = currentLayerHeight;
            }
        } else {
            toY = currentLayerHeight;
        }
        Vector3f toPosition = new Vector3f(toX, toY, toZ);
        Vector3f fromPosition = previousPosition;
        
        // Calculate length of side
        Vector3f positionDiff = new Vector3f();
        Vector3f.sub(toPosition, fromPosition, positionDiff);
        float length = positionDiff.length();
        
        // Calculate angle of rotation
        Vector3f positionDiffNormal = new Vector3f();
        positionDiff.normalise(positionDiffNormal);
        float angle = Vector3f.angle(positionDiffNormal, new Vector3f(1, 0, 0));
        if(toPosition.z > fromPosition.z) {
            angle = (float) Math.toRadians(180) - angle;
        }
        
        // Place position of entity in midle of vector
        Vector3f entityPosition = new Vector3f();
        Vector3f.sub(toPosition, fromPosition, entityPosition);
        entityPosition = new Vector3f(entityPosition.x / 2, entityPosition.y / 2, entityPosition.z / 2);
        Vector3f.add(entityPosition, fromPosition, entityPosition);
        
        Entity nozzleValvePositionDuringTravel = new Entity(model, entityPosition, 0, angle, 0, length, extrusionWidth, extrusionWidth);
        nozzleValvePositionDuringTravel.setColour(new Vector3f(1.0f, 0f, 1.0f));
        
        previousPosition = new Vector3f(toX, toY, toZ);
        
        return nozzleValvePositionDuringTravel;
    }
    
    private void processNozzleValvePositionNode(NozzleValvePositionNode nozzleValvePositionNode) {
        // Currently we don't do anything with these
    }
    
    private void processMCodeNode(MCodeNode mCodeNode) {
        // Currently we don't do anything with these.
    }
    
    private void processCommentNode(CommentNode commentNode) {
        // Currently we don't do anything with these.
    }
    
    private void processOrphanObjectDelineationNode(OrphanObjectDelineationNode orphanObjectDelineationNode) {
        // Currently we don't do anything with these.
    }
    
    private void processInnerPerimeterSectionNode(InnerPerimeterSectionNode innerPerimeterSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processOuterPerimeterSectionNode(OuterPerimeterSectionNode outerPerimeterSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processSkinSectionNode(SkinSectionNode skinSectionNode) {
        // Currently we don't do anything with these.
    }
    
    private void processFillSectionNode(FillSectionNode fillSectionNode) {
        // Currently we don't do anything with these.
    }
}
