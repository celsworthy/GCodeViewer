package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandHandler;
import celtech.gcodeviewer.engine.renderers.MasterRenderer;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Entity;
import static celtech.gcodeviewer.entities.Entity.N_DATA_VALUES;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.PrintVolume;
import celtech.gcodeviewer.gui.GUIManager;
import celtech.gcodeviewer.utils.CubeConstants;
import java.util.List;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author George Salter
 */
public class RenderingEngine {
    
    private final static float MINIMUM_STEP = 0.0005f;
    private final static Stenographer STENO = StenographerFactory.getStenographer(
            RenderingEngine.class.getName());
    
    private final long windowId;
    private int windowWidth;
    private int windowHeight;
    
    private final float printVolumeWidth;
    private final float printVolumeHeight;
    private final float printVolumeDepth;
    
    private RenderParameters renderParameters = new RenderParameters();
    private List<Entity> segments = null;
    private List<Entity> moves = null;
        
    private final MasterRenderer masterRenderer;
    private final GUIManager guiManager;
    
    private final ModelLoader modelLoader = new ModelLoader();

    private final SegmentLoader segmentLoader = new SegmentLoader();
    private final MoveLoader moveLoader = new MoveLoader();
    
    private final GCodeViewerConfiguration configuration;

    private final CommandHandler commandHandler;

    private RawModel lineModel;
    private RawModel model;

    private CenterPoint centerPoint = null;
    Floor floor = null;
    PrintVolume printVolume = null;

    public RenderingEngine(long windowId,
                           int windowWidth,
                           int windowHeight,
                           GCodeViewerConfiguration configuration,
                           CommandHandler commandHandler) {
        this.windowId = windowId;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.configuration = configuration;
        Vector3f printVolume = configuration.getPrintVolume();
        this.printVolumeWidth = (float)printVolume.getX();
        this.printVolumeHeight = (float)printVolume.getZ();
        this.printVolumeDepth = (float)printVolume.getY();

        renderParameters.setFromConfiguration(configuration);

        this.commandHandler = commandHandler;
        commandHandler.setRenderParameters(renderParameters);
        commandHandler.setRenderingEngine(this);
 
        masterRenderer = new MasterRenderer(windowWidth, windowHeight, renderParameters);
        guiManager = new GUIManager(windowId, windowWidth, windowHeight, renderParameters);
        model = null;
        lineModel = null;
    }
    
    public void start() {      
        STENO.debug("Starting RenderingEngine.");

        createWindowResizeCallback();
        
        Vector3f lightPos = new Vector3f(configuration.getLightPosition().getX(),
                                         configuration.getLightPosition().getY(),
                                         configuration.getLightPosition().getZ());
        Light light = new Light(lightPos, configuration.getLightColour());

        model = modelLoader.loadToVAO(CubeConstants.VERTICES, CubeConstants.NORMALS, CubeConstants.INDICES);
        lineModel = modelLoader.loadToVAO(new float[]{-0.5f, 0, 0, 0.5f, 0, 0});
        
        Vector3f centerPointStartPos = new Vector3f(-printVolumeWidth / 2, printVolumeHeight / 2, printVolumeDepth / 2);
        centerPoint = new CenterPoint(centerPointStartPos, lineModel);
        Camera camera = new Camera(windowId, centerPoint, guiManager);
        floor = new Floor(printVolumeWidth, printVolumeDepth, modelLoader);
        printVolume = new PrintVolume(lineModel, printVolumeWidth, printVolumeHeight, printVolumeDepth);
        
        glfwSetCharCallback(windowId, (window, codepoint) -> {
            guiManager.onChar(window, codepoint);
        });
        
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                int step = 4;
                if (mods == GLFW.GLFW_MOD_SHIFT)
                    step = 1;
                else if (mods == GLFW.GLFW_MOD_CONTROL)
                    step = 2;
                if (key == GLFW.GLFW_KEY_UP)
                    renderParameters.setTopLayerToRender(renderParameters.getTopLayerToRender() + step);
                else if (key == GLFW.GLFW_KEY_DOWN)
                    renderParameters.setTopLayerToRender(renderParameters.getTopLayerToRender() - step);
                else
                    guiManager.onKey(window, key, scancode, action, mods);
            }
        });

        masterRenderer.processFloor(floor);
        masterRenderer.processCentrePoint(centerPoint);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);
            
        STENO.debug("Running rendering loop.");
        while (!glfwWindowShouldClose(windowId)) {
            GL11.glViewport(0, 0, windowWidth, windowHeight);

            camera.move();
            renderParameters.checkLimits();

            masterRenderer.render(camera, light);
            
            guiManager.render();
            
            glfwSwapBuffers(windowId); // swap the color buffers
            
            guiManager.pollEvents(windowId);

            if (commandHandler.processCommands())
                glfwSetWindowShouldClose(windowId, true);
        }
        
        masterRenderer.cleanUp();
        guiManager.cleanUp();
        modelLoader.cleanUp();
        segmentLoader.cleanUp();
        moveLoader.cleanUp();
    }
    
    private void createWindowResizeCallback() {
        glfwSetWindowSizeCallback(windowId, (window, width, height) -> {
            windowWidth = width;
            windowHeight = height;
            masterRenderer.createProjectionMatrix(windowWidth, windowHeight);
            masterRenderer.reLoadProjectionMatrix();
            
            guiManager.onWindowResize(windowWidth, windowHeight);
            
        });
    }

    public void loadGCodeFile(String gCodeFile) {
        try {
            GCodeProcessor processor = new GCodeProcessor();
            GCodeVisualiser visualiser = new GCodeVisualiser(model, renderParameters, configuration);
            if (processor.processFile(gCodeFile, visualiser))
            {
                segments = visualiser.getSegments();
                moves = visualiser.getMoves();
                int numberOfLines = 0;
                if (segments.size() > 0)
                    numberOfLines = segments.get(segments.size() - 1).getLineNumber();
                if (moves.size() > 0)
                {
                    int n = moves.get(moves.size() - 1).getLineNumber();
                    if (numberOfLines < n)
                        numberOfLines = n;
                }
                renderParameters.setNumberOfLines(numberOfLines);
                renderParameters.setFirstSelectedLine(0);
                renderParameters.setLastSelectedLine(0);
                STENO.info("Number of lines = " + Integer.toString(renderParameters.getNumberOfLines()));

                if (processor.getNumberOfTopLayer() > Entity.NULL_LAYER)
                    renderParameters.setIndexOfTopLayer(processor.getNumberOfTopLayer());
                else
                    renderParameters.setIndexOfTopLayer(renderParameters.getNumberOfLines());
                if (processor.getNumberOfBottomLayer() > Entity.NULL_LAYER)
                    renderParameters.setIndexOfBottomLayer(processor.getNumberOfBottomLayer());
                else
                    renderParameters.setIndexOfBottomLayer(0);
                renderParameters.setTopLayerToRender(renderParameters.getIndexOfTopLayer());
                renderParameters.setBottomLayerToRender(renderParameters.getIndexOfBottomLayer());
                guiManager.setToolSet(visualiser.getToolSet());
            }
        }
        catch (RuntimeException ex)
        {
            renderParameters.clearLinesAndLayer();
            STENO.error("Parsing error");
        }
        masterRenderer.clearEntities();
        segmentLoader.cleanUp();
        if (segments != null && segments.size() > 0) {
            masterRenderer.processSegmentEntity(segmentLoader.loadToVAO(segments));           
        }
        moveLoader.cleanUp();
        if (moves != null && moves.size() > 0) {
            masterRenderer.processMoveEntity(moveLoader.loadToVAO(moves));           
        }
        masterRenderer.processCentrePoint(centerPoint);
        masterRenderer.processFloor(floor);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);
    }

    public void clearGCode() {
        renderParameters.clearLinesAndLayer();
        masterRenderer.clearEntities();
        segmentLoader.cleanUp();
        moveLoader.cleanUp();
        masterRenderer.processCentrePoint(centerPoint);
        masterRenderer.processFloor(floor);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);
    }

    public void reloadSegments() {
        masterRenderer.processSegmentEntity(null);
        segmentLoader.cleanUp();
        if (segments != null && segments.size() > 0) {
            masterRenderer.processSegmentEntity(segmentLoader.loadToVAO(segments));           
        }
    }
    
    public void reloadSegmentColours() {
        if (segments != null &&
            segments.size() > 0 &&
            masterRenderer.getSegmentEntity() != null) {
            segmentLoader.reloadColours(masterRenderer.getSegmentEntity(), segments);
        }
    }
        
    public void colourSegmentsFromType() {
        if (segments != null && segments.size() > 0) {
            segments.forEach(segment -> {
                segment.setColour(segment.getTypeColour());
            });
        }
    }
    
    public void colourSegmentsFromData(int dataIndex, float minValue, float maxValue) {
        List<Vector3f> colourPalette = renderParameters.getDataColourPalette();
        if (dataIndex >= 0 &&
            dataIndex < Entity.N_DATA_VALUES &&
            colourPalette.size() > 0 &&
            minValue < maxValue)
        {
            int nSteps = colourPalette.size();
            float span = maxValue - minValue;
            float step = span / nSteps;
            if (colourPalette.size() > 1 && step > MINIMUM_STEP) {
                segments.forEach(segment -> {
                    //System.out.println("Data[" + Integer.toString(dataIndex) + "] = " + Float.toString(segment.getDataValue(dataIndex)));
                    float index = (segment.getDataValue(dataIndex) - minValue) / step;
                    Vector3f segmentColour;
                    if (index < 1.0)
                        segmentColour = colourPalette.get(0);
                    else if (index >= nSteps)
                        segmentColour = colourPalette.get(nSteps - 1);
                    else
                        segmentColour = colourPalette.get((int)index);
                    segment.setColour(segmentColour);
                });
            }
            else {
                Vector3f segmentColour = colourPalette.get(0);
                segments.forEach(segment -> {
                   segment.setColour(segmentColour);
                });
            }
        }
        else {
            Vector3f defaultColour = renderParameters.getDefaultColour();
            segments.forEach(segment -> {
               segment.setColour(defaultColour);
            });
        }
    }
}
