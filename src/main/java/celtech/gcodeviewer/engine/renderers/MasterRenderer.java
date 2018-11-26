package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawEntity;
import celtech.gcodeviewer.engine.RawModel;
import celtech.gcodeviewer.engine.RenderingEngine;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.LineEntity;
import celtech.gcodeviewer.gcode.GCodeLine;
import celtech.gcodeviewer.shaders.EntityShader;
import celtech.gcodeviewer.shaders.FloorShader;
import celtech.gcodeviewer.shaders.LineShader;
//import celtech.gcodeviewer.shaders.StaticShader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class MasterRenderer {

    private static final float FOV = 70f;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 500f;
    
//    private final StaticShader staticShader = new StaticShader();
//    private final StaticRenderer staticEntityRenderer;
    
    private final EntityShader entityShader = new EntityShader();
    private final RawEntityRenderer entityRenderer;

    private final LineShader lineShader = new LineShader();
    private final LineRenderer lineRenderer;
    
    private final FloorShader floorShader = new FloorShader();
    private final FloorRenderer floorRenderer;
    
    private RawEntity rawEntity = null;
    private final Map<RawModel, List<Entity>> entities = new HashMap<>();
    private final List<LineEntity> lineEntities = new ArrayList<>();
    private Floor floor;
    private CenterPoint centrePoint;
    
    private Matrix4f projectionMatrix;

    private boolean showMovesFlag = false;
    private boolean showTool0Flag = true;
    private boolean showTool1Flag = true;
    RenderingEngine.ColourMode colourMode = RenderingEngine.ColourMode.COLOUR_AS_TOOL;
    private Vector3f tool0Colour = new Vector3f(1.0f, 1.0f, 0.0f);
    private Vector3f tool1Colour = new Vector3f(0.0f, 1.0f, 1.0f);
    private int topLayerToShow = 0;
    private int bottomLayerToShow = 0;
    private int firstLineToShow = 0;
    private int lastLineToShow = 0;
    
    public MasterRenderer(int windowWidth, int windowHeight) {
        createProjectionMatrix(windowWidth, windowHeight);
//        staticEntityRenderer = new StaticRenderer(staticShader, projectionMatrix);
        entityRenderer = new RawEntityRenderer(entityShader, projectionMatrix);
        lineRenderer = new LineRenderer(lineShader, projectionMatrix);
        floorRenderer = new FloorRenderer(floorShader, projectionMatrix);
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    public void setShowFlags(boolean showMovesFlag, boolean showTool0Flag, boolean showTool1Flag)
    {
        this.showMovesFlag = showMovesFlag;
        this.showTool0Flag = showTool0Flag;
        this.showTool1Flag = showTool1Flag;
    }

    public boolean showMoves()
    {
        return showMovesFlag;
    }
    
    public boolean showTool0()
    {
        return showTool0Flag;
    }

    public boolean showTool1()
    {
        return showTool0Flag;
    }

    public void setColourMode(RenderingEngine.ColourMode colourMode)
    {
        this.colourMode = colourMode;
    }

    public void setToolColours(Vector3f tool0Colour, Vector3f tool1Colour)
    {
        this.tool0Colour = tool0Colour;
        this.tool1Colour = tool1Colour;
    }

    public RenderingEngine.ColourMode getColourMode()
    {
        return colourMode;
    }

    public void setTopLayerToShow(int topLayerToShow)
    {
        this.topLayerToShow = topLayerToShow;
    }

    public int getTopLayerToShow()
    {
        return topLayerToShow;
    }
    
    public void setBottomLayerToShow(int bottomLayerToShow)
    {
        this.bottomLayerToShow = bottomLayerToShow;
    }

    public int getBottomLayerToShow()
    {
        return bottomLayerToShow;
    }

    public void setFirstLineToShow(int firstLineToShow)
    {
        this.firstLineToShow = firstLineToShow;
    }

    public int getFirstLineToShow()
    {
        return firstLineToShow;
    }

    public void setLastLineToShow(int lastLineToShow)
    {
        this.lastLineToShow = lastLineToShow;
    }

    public int getLastLineToShow()
    {
        return lastLineToShow;
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

        if (rawEntity != null) {
            int showFlags = (showMovesFlag ? 1 : 0) + (showTool0Flag ? 2 : 0) + (showTool1Flag ? 4 : 0);
            if (colourMode == RenderingEngine.ColourMode.COLOUR_AS_TYPE)
                showFlags += 8;
            entityRenderer.render(rawEntity,
                                  camera, light,
                                  tool0Colour, tool1Colour,
                                  showFlags,
                                  topLayerToShow, bottomLayerToShow,
                                  firstLineToShow, lastLineToShow);
        }
        
        if (floor != null) {
            floorShader.start();
            floorShader.loadLight(light);
            floorShader.loadViewMatrix(camera);
            floorRenderer.render(floor);
            floorShader.stop();
        }
    }
    
    public void checkErrors() {
        int i = glGetError ();
        if (i != GL_NO_ERROR) {
            System.out.println("Master Renderer OpenGL error " + Integer.toString(i));
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
    
    public void processRawEntity(RawEntity rawEntity) {
        this.rawEntity = rawEntity;
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
        this.rawEntity = null;
        entities.clear();
        lineEntities.clear();
    }
    
    public void cleanUp() {
        //staticShader.cleanUp();
        floorShader.cleanUp();
        lineShader.cleanUp();
        entityShader.cleanUp();
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
//        staticEntityRenderer.loadProjectionMatrix(projectionMatrix);
        lineRenderer.loadProjectionMatrix(projectionMatrix);
        entityRenderer.setProjectionMatrix(projectionMatrix);
        floorRenderer.loadProjectionMatrix(projectionMatrix);
    }

    public final void prepareModelEntities(RawModel model) {
        
    }

    public final void prepareEntities() {
        entities.keySet().stream().forEach(model -> prepareModelEntities(model));
    }
    
    
}


