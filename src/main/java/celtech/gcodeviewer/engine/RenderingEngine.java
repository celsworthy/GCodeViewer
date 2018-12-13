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
import java.nio.IntBuffer;
import java.util.List;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
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
    
    private final float printVolumeWidth;
    private final float printVolumeHeight;
    private final float printVolumeDepth;
    
    private RenderParameters renderParameters = new RenderParameters();
    private List<Entity> segments = null;
    private List<Entity> moves = null;
    private List<String> lines = null;
        
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
    
    GCodeLoader fileLoader = null;
    private final double minDataValues[];
    private final double maxDataValues[];

    public RenderingEngine(long windowId,
                           int windowWidth,
                           int windowHeight,
                           GCodeViewerConfiguration configuration) {
        this.windowId = windowId;
        this.configuration = configuration;
        Vector3f printVolume = configuration.getPrintVolume();
        this.printVolumeWidth = (float)printVolume.getX();
        this.printVolumeHeight = (float)printVolume.getZ();
        this.printVolumeDepth = (float)printVolume.getY();

        renderParameters.setFromConfiguration(configuration);
        renderParameters.setWindowWidth(windowWidth);
        renderParameters.setWindowHeight(windowHeight);

        this.commandHandler = new CommandHandler();
        commandHandler.setRenderParameters(renderParameters);
        commandHandler.setRenderingEngine(this);
 
        masterRenderer = new MasterRenderer(renderParameters);
        guiManager = new GUIManager(windowId, renderParameters);
        model = null;
        lineModel = null;
        
        this.minDataValues = new double[Entity.N_DATA_VALUES];
        this.maxDataValues = new double[Entity.N_DATA_VALUES];
        for (int dataIndex = 0; dataIndex < Entity.N_DATA_VALUES; ++dataIndex)
        {
            this.minDataValues[dataIndex] = 0.0;
            this.maxDataValues[dataIndex] = 0.0;
        }
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
            guiManager.onKey(window, key, scancode, action, mods);
        });

        masterRenderer.processFloor(floor);
        masterRenderer.processCentrePoint(centerPoint);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);

        commandHandler.start();

        STENO.debug("Running rendering loop.");
        while (!glfwWindowShouldClose(windowId)) {
            GL11.glViewport(0, 0, renderParameters.getWindowWidth(), renderParameters.getWindowHeight());
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);

                glfwGetFramebufferSize(windowId, w, h);
                renderParameters.setDisplayWidth(w.get(0));
                renderParameters.setDisplayHeight(h.get(0));
            }

            camera.move();
            renderParameters.checkLimits();

            masterRenderer.render(camera, light);
            
            guiManager.render();
            
            glfwSwapBuffers(windowId); // swap the color buffers
            
            guiManager.pollEvents(windowId);

            if (commandHandler.processCommands())
                glfwSetWindowShouldClose(windowId, true);
            
            if (fileLoader != null && fileLoader.loadFinished())
                completeLoadingGCodeFile();
        }
        
        commandHandler.stop();
        masterRenderer.cleanUp();
        guiManager.cleanUp();
        modelLoader.cleanUp();
        segmentLoader.cleanUp();
        moveLoader.cleanUp();
        fileLoader = null;
    }
    
    private void createWindowResizeCallback() {
        glfwSetWindowSizeCallback(windowId, (window, width, height) -> {
            renderParameters.setWindowWidth(width);
            renderParameters.setWindowHeight(height);
            masterRenderer.createProjectionMatrix(width, height);
            masterRenderer.reLoadProjectionMatrix();
        });
    }

    public void startLoadingGCodeFile(String gCodeFile) {
        fileLoader = new GCodeLoader(gCodeFile, model, renderParameters, configuration);
        fileLoader.start();
    }

    public void completeLoadingGCodeFile() {
        if (fileLoader != null && fileLoader.loadFinished()) {
            try {
                if (fileLoader.loadSuccess())
                {
                    GCodeProcessor processor = fileLoader.getProcessor();
                    GCodeLineProcessor lineProcessor = fileLoader.getLineProcessor();

                    segments = lineProcessor.getSegments();
                    moves = lineProcessor.getMoves();
                    
                    for (int dataIndex = 0; dataIndex < Entity.N_DATA_VALUES; ++dataIndex)
                    {
                        minDataValues[dataIndex] = lineProcessor.getMinDataValue(dataIndex);
                        maxDataValues[dataIndex] = lineProcessor.getMaxDataValue(dataIndex);
                    }

                    renderParameters.setNumberOfLines(processor.getLines().size());
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
                    guiManager.setToolSet(lineProcessor.getToolSet());
                    guiManager.setLines(processor.getLines());
                }
            }
            catch (RuntimeException ex)
            {
                renderParameters.clearLinesAndLayer();
                STENO.error("Parsing error");
            }
            fileLoader = null;
            
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
    
    public void colourSegmentsFromData(int dataIndex) {
        
        List<Vector3f> colourPalette = renderParameters.getDataColourPalette();
        if (dataIndex >= 0 &&
            dataIndex < Entity.N_DATA_VALUES &&
            colourPalette.size() > 0 &&
            minDataValues[dataIndex] < maxDataValues[dataIndex])
        {
            double minValue = minDataValues[dataIndex];
            double maxValue = maxDataValues[dataIndex];
            int nSteps = colourPalette.size();
            double span = maxValue - minValue;
            double step = span / nSteps;
            if (colourPalette.size() > 1 && step > MINIMUM_STEP) {
                segments.forEach(segment -> {
                    //System.out.println("Data[" + Integer.toString(dataIndex) + "] = " + Float.toString(segment.getDataValue(dataIndex)));
                    double index = (segment.getDataValue(dataIndex) - minValue) / step;
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
