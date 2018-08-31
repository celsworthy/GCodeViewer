package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawModel;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.LineEntity;
import celtech.gcodeviewer.gcode.Layer;
import celtech.gcodeviewer.shaders.FloorShader;
import celtech.gcodeviewer.shaders.GUIShader;
import celtech.gcodeviewer.shaders.LineShader;
import celtech.gcodeviewer.shaders.StaticShader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Matrix4f;

public class MasterRenderer {

    private static final float FOV = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 500f;
    
    private final StaticShader staticShader = new StaticShader();
    private final EntityRenderer entityRenderer;
    
    private final LineShader lineShader = new LineShader();
    private final LineRenderer lineRenderer;
    
    private final FloorShader floorShader = new FloorShader();
    private final FloorRenderer floorRenderer;
    
    private final Map<RawModel, List<Entity>> entities = new HashMap<>();
    private final List<LineEntity> lineEntities = new ArrayList<>();
    private Floor floor;
    
    private Matrix4f projectionMatrix;
    
    public MasterRenderer(int windowWidth, int windowHeight) {
        createProjectionMatrix(windowWidth, windowHeight);
        entityRenderer = new EntityRenderer(staticShader, projectionMatrix);
        lineRenderer = new LineRenderer(lineShader, projectionMatrix);
        floorRenderer = new FloorRenderer(floorShader, projectionMatrix);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

    }
    
    public void render(Camera camera, Light light) {   
        prepare();
        
        staticShader.start();
        staticShader.loadLight(light);
        staticShader.loadViewMatrix(camera);
        entityRenderer.render(entities);
        staticShader.stop();
        
        lineShader.start();
        lineShader.loadViewMatrix(camera);
        if(!lineEntities.isEmpty()) {
            lineRenderer.render(lineEntities);
        }
        lineShader.stop();
        
        if (floor != null) {
            floorShader.start();
            floorShader.loadLight(light);
            floorShader.loadViewMatrix(camera);
            floorRenderer.render(floor);
            floorShader.stop();
        }
        
        entities.clear();
        lineEntities.clear();
    }
    
    public void processLayer(Layer layer) {
        if(layer.isRendered()) {
            layer.getEntities().forEach(this::processEntity);
            layer.getLineEntitys().forEach(this::processLine);
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
    
    public void processFloor(Floor floor) {
        this.floor = floor;
    }
    
    public void processLine(LineEntity lineEntity) {
        lineEntities.add(lineEntity);
    }
    
    public void cleanUp() {
        staticShader.cleanUp();
        floorShader.cleanUp();
        lineShader.cleanUp();
    }
    
    private void prepare() {
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
        
        projectionMatrix = new Matrix4f();
        projectionMatrix.m00 = x_scale;
        projectionMatrix.m11 = y_scale;
        projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
        projectionMatrix.m23 = -1;
        projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
        projectionMatrix.m33 = 0;
    }
    
    /**
     * Re-loads the projection matrix into the renderer's respective shaders.
     */
    public final void reLoadProjectionMatrix() {
        entityRenderer.loadProjectionMatrix(projectionMatrix);
        lineRenderer.loadProjectionMatrix(projectionMatrix);
        floorRenderer.loadProjectionMatrix(projectionMatrix);
    }
}
