package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandHandler;
import celtech.gcodeviewer.engine.renderers.MasterRenderer;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.PrintVolume;
import celtech.gcodeviewer.gui.GUIManager;
import celtech.gcodeviewer.i18n.MessageLookup;
import celtech.gcodeviewer.utils.CubeConstants;
import java.io.File;
import java.nio.IntBuffer;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 *
 * @author George Salter
 */
public class RenderingEngine {
    
    private static final double MINIMUM_ITERATION_TIME = 0.01; // (100 frames per second)
    private static final double FPS_UPDATE_INTERVAL = 1.0; // 1 update per second.
    
    private final static float MINIMUM_STEP = 0.0005f;
    private final static Stenographer STENO = StenographerFactory.getStenographer(
            RenderingEngine.class.getName());
    
    private final long windowId;
    
    private float printVolumeWidth;
    private float printVolumeHeight;
    private float printVolumeDepth;
    private float printVolumeOffsetX;
    private float printVolumeOffsetY;
    private float printVolumeOffsetZ;
    
    private RenderParameters renderParameters = new RenderParameters();
    private List<Entity> segments = null;
    private List<Entity> moves = null;
    private Camera camera = null;
    
    private final MasterRenderer masterRenderer;
    private final GUIManager guiManager;
    
    private final ModelLoader modelLoader = new ModelLoader();
    private final ModelLoader floorLoader = new ModelLoader();

    private final SegmentLoader segmentLoader = new SegmentLoader();
    private final MoveLoader moveLoader = new MoveLoader();
    
    private final GCodeViewerConfiguration configuration;
    private final GCodeViewerGUIConfiguration guiConfiguration;

    private final CommandHandler commandHandler;

    private RawModel lineModel;
    private RawModel model;

    private CenterPoint centerPoint = null;
    Floor floor = null;
    String printerType;
    PrintVolume printVolume = null;
    
    GCodeLoader fileLoader = null;
    String currentFilePath = null;

    private final double minDataValues[];
    private final double maxDataValues[];
    
    private double previousTime = 0.0;
    private double updateDueTime = 0.0;
    private double averageFrameTime = 0.0;
    private double frameTimeAccumulator = 0.0;
    private int nFrames = 0;

    public RenderingEngine(long windowId,
                           int windowWidth,
                           int windowHeight,
                           int windowXPos,
                           int windowYPos,
                           String printerType,
                           GCodeViewerConfiguration configuration,
                           GCodeViewerGUIConfiguration guiConfiguration) {
        this.windowId = windowId;
        this.configuration = configuration;
        this.guiConfiguration = guiConfiguration;
        this.printerType = printerType;
        renderParameters.setFromConfiguration(configuration);
        renderParameters.setFromGUIConfiguration(guiConfiguration);
        renderParameters.setWindowWidth(windowWidth);
        renderParameters.setWindowHeight(windowHeight);
        renderParameters.setWindowXPos(windowXPos);
        renderParameters.setWindowYPos(windowYPos);
        this.commandHandler = new CommandHandler();
        commandHandler.setRenderParameters(renderParameters);
        commandHandler.setRenderingEngine(this);
 
        masterRenderer = new MasterRenderer(renderParameters);
        guiManager = new GUIManager(windowId, renderParameters);
        guiManager.setFromGUIConfiguration(guiConfiguration);
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
    
    public void start(String gCodeFile) {      
        STENO.debug("Starting RenderingEngine.");

        createWindowResizeCallback();
        
        Vector3f lightPos = new Vector3f(configuration.getLightPosition().x(),
                                         configuration.getLightPosition().y(),
                                         configuration.getLightPosition().z());
        Light light = new Light(lightPos, configuration.getLightColour());

        model = modelLoader.loadToVAO(CubeConstants.VERTICES, CubeConstants.NORMALS, CubeConstants.INDICES);
        lineModel = modelLoader.loadToVAO(new float[]{-0.5f, 0, 0, 0.5f, 0, 0});
        
        setPrinterType(printerType);
        
        glfwSetCharCallback(windowId, (window, codepoint) -> {
            guiManager.onChar(window, codepoint);
        });
        
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            guiManager.onKey(window, key, scancode, action, mods);
        });

        if (gCodeFile != null)
            startLoadingGCodeFile(gCodeFile);
        
        commandHandler.start();

        STENO.debug("Running rendering loop.");
        double previousTime = glfwGetTime();
        boolean  frameRendered = false;
        while (!glfwWindowShouldClose(windowId)) {
            frameRendered = false;
            
            GL11.glViewport(0, 0, renderParameters.getWindowWidth(), renderParameters.getWindowHeight());
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);

                glfwGetFramebufferSize(windowId, w, h);
                renderParameters.setDisplayWidth(w.get(0));
                renderParameters.setDisplayHeight(h.get(0));
            }

            if (renderParameters.getViewResetRequired())
            {
                GCodeViewerConfiguration.PrintVolumeDetails printVolumeDetails = configuration.getPrintVolumeDetailsForType(printerType);
                Vector3f centerPointStartPos = new Vector3f(printVolumeOffsetX + 0.5f * printVolumeWidth, printVolumeOffsetY + 0.5f * printVolumeDepth, printVolumeOffsetZ + 0.5f * printVolumeHeight);
                camera.reset(centerPointStartPos, printVolumeDetails.getDefaultCameraDistance());
                renderParameters.clearViewResetRequired();
            }
            camera.move();
            
            renderParameters.checkLimits();

            if (renderParameters.getRenderRequired())
            {
                frameRendered = true;
                masterRenderer.render(camera, light);

                guiManager.render();

                glfwSwapInterval(1);
                glfwSwapBuffers(windowId); // swap the color buffers
                renderParameters.clearRenderRequired();
            }
            
            guiManager.pollEvents(windowId);

            if (commandHandler.processCommands())
                glfwSetWindowShouldClose(windowId, true);
            
            if (fileLoader != null && fileLoader.loadFinished())
                completeLoadingGCodeFile();

            // Update the frame timer.
            double currentTime = glfwGetTime();
            double iterationTime = currentTime - previousTime;
            previousTime = currentTime;
            if (frameRendered) {
                frameTimeAccumulator += iterationTime;
                ++nFrames;
                if (currentTime > updateDueTime) {
                    renderParameters.setFrameTime(frameTimeAccumulator / nFrames);
                    frameTimeAccumulator = 0.0;
                    nFrames = 0;
                    updateDueTime = currentTime + FPS_UPDATE_INTERVAL;
                }
            }
            
            // Not sure if this is strictly necessary.
            // However, if nothing is changing, this loop becomes effectively a busy wait.
            // To prevent this, make the system wait at least the MINIMUM_ITERATION_TIME before
            // starting the next iteration. Currently this would give a maximum of 100 frames per second, which
            // should be enough.
            double remainingLoopTime = MINIMUM_ITERATION_TIME - iterationTime;
            if (remainingLoopTime > 0.001) {
                try {
                    Thread.sleep((long)(1000.0 * remainingLoopTime)); // convert seconds to milliseconds.
                }
                catch (InterruptedException ex) {
                    // Carry on!
                }
            }
        }
        renderParameters.saveToGUIConfiguration(guiConfiguration);
        guiManager.saveToGUIConfiguration(guiConfiguration);
        commandHandler.stop();
        masterRenderer.cleanUp();
        guiManager.cleanUp();
        floorLoader.cleanUp();
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
            masterRenderer.reloadProjectionMatrix();
        });
        glfwSetWindowPosCallback(windowId, (window, xPos, yPos) -> {
            renderParameters.setWindowXPos(xPos);
            renderParameters.setWindowYPos(yPos);
        });

    }

    public void startLoadingGCodeFile(String gCodeFile) {
        if (gCodeFile != null && !gCodeFile.isEmpty()) {
            fileLoader = new GCodeLoader(gCodeFile, model, renderParameters, configuration);
            fileLoader.start();
        }
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
                    guiManager.setTypeSet(lineProcessor.getTypeSet());
                    guiManager.setLines(processor.getLines());
                    guiManager.setLayerMap(lineProcessor.getLayerMap());
                    currentFilePath = fileLoader.getFilePath();
                    
                    // Update the window title with the file name.
                    File f = new File(currentFilePath);
                    glfwSetWindowTitle(windowId, MessageLookup.i18n("window.titleWithFileName").replaceAll("#1", f.getName()));
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
        }
    }
    
    public String getCurrentFilePath() {
            return currentFilePath;
    }

    public void clearGCode() {
        renderParameters.clearLinesAndLayer();
        masterRenderer.clearEntities();
        segmentLoader.cleanUp();
        moveLoader.cleanUp();
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
    
    public void setPrinterType(String printerType) {
        if (camera == null || !this.printerType.equalsIgnoreCase(printerType)) {
            this.printerType = printerType;
            GCodeViewerConfiguration.PrintVolumeDetails printVolumeDetails = configuration.getPrintVolumeDetailsForType(printerType);
            this.printVolumeWidth = printVolumeDetails.getDimensions().x();
            this.printVolumeDepth = printVolumeDetails.getDimensions().y();
            this.printVolumeHeight = printVolumeDetails.getDimensions().z();
            this.printVolumeOffsetX = printVolumeDetails.getOffset().x();
            this.printVolumeOffsetY = printVolumeDetails.getOffset().y();
            this.printVolumeOffsetZ = printVolumeDetails.getOffset().z();

            Vector3f centerPointStartPos = new Vector3f(printVolumeOffsetX + 0.5f * printVolumeWidth, printVolumeOffsetY + 0.5f * printVolumeDepth, printVolumeOffsetZ + 0.5f * printVolumeHeight);
            centerPoint = new CenterPoint(centerPointStartPos, lineModel);
            camera = new Camera(windowId, centerPoint, printVolumeDetails.getDefaultCameraDistance(), guiManager);
            floorLoader.cleanUp();
            floor = new Floor(printVolumeWidth, printVolumeDepth, printVolumeOffsetX, printVolumeOffsetY, printVolumeOffsetZ, floorLoader);
            printVolume = new PrintVolume(lineModel, printVolumeWidth, printVolumeDepth, printVolumeHeight, printVolumeOffsetX, printVolumeOffsetY, printVolumeOffsetZ);

            masterRenderer.processFloor(floor);
            masterRenderer.processCentrePoint(centerPoint);
            masterRenderer.processPrintVolume(printVolume);

            renderParameters.setRenderRequired();
        }
    }
}
