package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawEntity;
import celtech.gcodeviewer.engine.RawModel;
import celtech.gcodeviewer.engine.RenderParameters;
import celtech.gcodeviewer.engine.RenderingEngine;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.LineEntity;
import celtech.gcodeviewer.gcode.GCodeLine;
import celtech.gcodeviewer.shaders.SegmentShader;
import celtech.gcodeviewer.shaders.FloorShader;
import celtech.gcodeviewer.shaders.LineShader;
import celtech.gcodeviewer.shaders.MoveShader;
//import celtech.gcodeviewer.shaders.StaticShader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer {

    private static final float FOV = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 500f;
    
//    private final StaticShader staticShader = new StaticShader();
//    private final StaticRenderer staticEntityRenderer;
    
    private final SegmentShader segmentShader = new SegmentShader();
    private final SegmentRenderer segmentRenderer;

    private final MoveShader moveShader = new MoveShader();
    private final MoveRenderer moveRenderer;

    private final LineShader lineShader = new LineShader();
    private final LineRenderer lineRenderer;
    
    private final FloorShader floorShader = new FloorShader();
    private final FloorRenderer floorRenderer;
    
    private RawEntity segmentEntity = null;
    private RawEntity moveEntity = null;
    private final Map<RawModel, List<Entity>> entities = new HashMap<>();
    private final List<LineEntity> lineEntities = new ArrayList<>();
    private Floor floor;
    private CenterPoint centrePoint;
    
    private Matrix4f projectionMatrix;

    private RenderParameters renderParameters;
    
    public MasterRenderer(RenderParameters renderParameters) {
        createProjectionMatrix(renderParameters.getWindowWidth(), renderParameters.getWindowHeight());
//        this.staticEntityRenderer = new StaticRenderer(staticShader, projectionMatrix);
        this.segmentRenderer = new SegmentRenderer(segmentShader, projectionMatrix);
        this.moveRenderer = new MoveRenderer(moveShader, projectionMatrix);
        this.lineRenderer = new LineRenderer(lineShader, projectionMatrix);
        this.floorRenderer = new FloorRenderer(floorShader, projectionMatrix);
        this.renderParameters = renderParameters;
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }
    
    public RenderParameters getRenderParameters() {
        return renderParameters;
    }

    public void setRenderParameters(RenderParameters renderParameters) {
        this.renderParameters = renderParameters;
    }

    public void render(Camera camera, Light light) {   
        prepare();
        
//        staticShader.start();
//        staticShader.loadLight(light);
//        staticShader.loadViewMatrix(camera);
//        staticEntityRenderer.render(entities, showMovesFlag, topLayerToShow, bottomLayerToShow, firstLineToShow, lastLineToShow);
//        staticShader.stop();
        
        if (!lineEntities.isEmpty() || centrePoint != null &&  centrePoint.isRendered()) {
            lineShader.start();
            lineShader.loadViewMatrix(camera);
            if(!lineEntities.isEmpty()) {
                lineRenderer.render(lineEntities);
            }
            if (centrePoint != null && centrePoint.isRendered()) {
                lineRenderer.render(centrePoint.getLineEntities());
            }
            lineShader.stop();
        }

        if (segmentEntity != null) {
            segmentRenderer.render(segmentEntity, camera, light, renderParameters);
        }
        
        if (moveEntity != null && renderParameters.getShowMoves()) {
            moveRenderer.render(moveEntity, camera, light, renderParameters);
        }

        if (floor != null) {
            floorShader.start();
            floorShader.loadLight(light);
            floorShader.loadViewMatrix(camera);
            floorRenderer.render(floor);
            floorShader.stop();
        }
        checkErrors();
    }
    
    public static void checkErrors() {
        int i = glGetError ();
        if (i != GL_NO_ERROR) {
            System.out.println("OpenGL error " + Integer.toString(i));
        }
    }
    
    public void processEntity(Entity entity) {
        RawModel entityModel = entity.getModel();
        List<Entity> modelEntities;
        
        if(entities.containsKey(entityModel)) {
            modelEntities = entities.get(entityModel);
            modelEntities.add(entity);
        } else {
            modelEntities = new ArrayList<>();
            modelEntities.add(entity);
            entities.put(entityModel, modelEntities);
        }
    }
    
    public void processSegmentEntity(RawEntity segmentEntity) {
        this.segmentEntity = segmentEntity;
    }

    public RawEntity getSegmentEntity() {
        return segmentEntity;
    }
    
    public void processMoveEntity(RawEntity moveEntity) {
        this.moveEntity = moveEntity;
    }

    public void processFloor(Floor floor) {
        this.floor = floor;
    }
    
    public void processCentrePoint(CenterPoint centrePoint) {
        this.centrePoint = centrePoint;
    }
    
    public void processLine(LineEntity lineEntity) {
        lineEntities.add(lineEntity);
    }
    
    public void clearEntities() {
        this.segmentEntity = null;
        this.moveEntity = null;
        entities.clear();
        lineEntities.clear();
    }
    
    public void cleanUp() {
        //staticShader.cleanUp();
        floorShader.cleanUp();
        lineShader.cleanUp();
        segmentShader.cleanUp();
    }
    
    private void prepare() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(0.6f, 0.6f, 0.6f, 1);
    }
    
    /**
     * Generates the projection matrix for a perspective view.
     * 
     * @param windowWidth
     * @param windowHeight 
     */
    public final void createProjectionMatrix(float windowWidth, float windowHeight) {
        float aspectRatio = windowWidth / windowHeight;
        float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))) * aspectRatio);
        float x_scale = y_scale / aspectRatio;
        float frustum_length = FAR_PLANE - NEAR_PLANE;
        
        projectionMatrix = new Matrix4f().zero();
        projectionMatrix.m00(x_scale);
        projectionMatrix.m11(y_scale);
        projectionMatrix.m22(-((FAR_PLANE + NEAR_PLANE) / frustum_length));
        projectionMatrix.m23(-1.0f);
        projectionMatrix.m32(-((2.0f * NEAR_PLANE * FAR_PLANE) / frustum_length));
        projectionMatrix.m33(0.0f);
    }
    
    /**
     * Re-loads the projection matrix into the renderer's respective shaders.
     */
    public final void reLoadProjectionMatrix() {
//        staticEntityRenderer.loadProjectionMatrix(projectionMatrix);
        lineRenderer.loadProjectionMatrix(projectionMatrix);
        segmentRenderer.setProjectionMatrix(projectionMatrix);
        moveRenderer.setProjectionMatrix(projectionMatrix);
        floorRenderer.loadProjectionMatrix(projectionMatrix);
    }

    public final void prepareModelEntities(RawModel model) {
        
    }

    public final void prepareEntities() {
        entities.keySet().stream().forEach(model -> prepareModelEntities(model));
    }
}


