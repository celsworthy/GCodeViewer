package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandQueue;
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

    private final CommandQueue commandQueue;

    private RawModel lineModel;
    private RawModel model;

    private CenterPoint centerPoint = null;
    Floor floor = null;
    PrintVolume printVolume = null;

    private float minDataValues[];
    private float maxDataValues[];
    
    public RenderingEngine(long windowId,
                           int windowWidth,
                           int windowHeight,
                           GCodeViewerConfiguration configuration,
                           CommandQueue commandQueue) {
        this.windowId = windowId;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.configuration = configuration;
        this.commandQueue = commandQueue;
        Vector3f printVolume = configuration.getPrintVolume();
        this.printVolumeWidth = (float)printVolume.getX();
        this.printVolumeHeight = (float)printVolume.getZ();
        this.printVolumeDepth = (float)printVolume.getY();

        renderParameters.setMoveColour(configuration.getMoveColour());
        renderParameters.setToolColours(configuration.getToolColours());
        renderParameters.setDataColourPalette(configuration.getDataColourPalette());

        this.minDataValues = new float[Entity.N_DATA_VALUES];
        this.minDataValues[0] = -30.0f;
        this.minDataValues[1] = -180.0f;
        this.minDataValues[2] = -1.0f;
        this.minDataValues[3] = -1.0f;
        this.minDataValues[4] = 0.0f;

        this.maxDataValues = new float[Entity.N_DATA_VALUES];
        this.maxDataValues[0] = 30.0f;
        this.maxDataValues[1] = 180.0f;
        this.maxDataValues[2] = 1.0f;
        this.maxDataValues[3] = 1.0f;
        this.maxDataValues[4] = 5000.0f;
 
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

            if (commandQueue.commandAvailable())
                processCommands();
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

    private void loadGCodeFile(String gCodeFile) {
        try {
            GCodeProcessor processor = new GCodeProcessor();
            GCodeVisualiser visualiser = new GCodeVisualiser(model, configuration);
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
                renderParameters.setFirstLineToRender(0);
                renderParameters.setLastLineToRender(renderParameters.getNumberOfLines());
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
                STENO.info("Top layer = " + Integer.toString(renderParameters.getIndexOfTopLayer()));
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
    
    private void processCommands() {
        while (commandQueue.commandAvailable()) {
            String command = commandQueue.getNextCommandFromQueue().trim();
            STENO.debug("Processing command " + command);
            if (command.equalsIgnoreCase("q") || command.equalsIgnoreCase("quit")) {
                glfwSetWindowShouldClose(windowId, true);
                break;
            }
            else {
                try {
                    String commandParameter;
                    Scanner commandScanner = new Scanner(command);
                    if (commandScanner.hasNext()) {
                        String commandWord = commandScanner.next().toLowerCase();
                        switch (commandWord) {
                            case "load":
                                loadGCodeFile(commandScanner.nextLine().trim());
                                break;

                            case "l1":
                                loadGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\snake.gcode");
                                break;

                            case "l2":
                                loadGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\gear-box.gcode");
                                break;

                            case "l3":
                                loadGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\spiral-65-0p5.gcode");
                                break;

                            case "l4":
                                loadGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\cones_robox.gcode");
                                break;

                            case "l5":
                                loadGCodeFile("D:\\CEL\\Dev\\ImpactWire\\step\\test_part_spline_offset_top_minus.gcode");
                                break;

                            case "show":
                            case "s":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                    case "m":
                                        renderParameters.setShowMoves(true);
                                        break;

                                    case "tool0":
                                    case "t0":
                                    case "0":
                                        renderParameters.setShowTool0(true);
                                        break;

                                    case "tool1":
                                    case "t1":
                                    case "1":
                                        renderParameters.setShowTool1(true);
                                        break;

                                    default:
                                        STENO.error("Unrecognised option in command " + command);
                                }
                                break;

                            case "hide":
                            case "h":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                    case "m":
                                        renderParameters.setShowMoves(false);
                                        break;

                                    case "tool0":
                                    case "t0":
                                    case "0":
                                        renderParameters.setShowTool0(false);
                                        break;

                                    case "tool1":
                                    case "t1":
                                    case "1":
                                        renderParameters.setShowTool1(false);
                                        break;

                                    default:
                                        STENO.error("Unrecognised option in command " + command);
                                }
                                break;

                            case "top":
                            case "t":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setTopLayerToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setTopLayerToRender(renderParameters.getIndexOfTopLayer());
                                break;

                            case "bottom":
                            case "b":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setBottomLayerToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setBottomLayerToRender(renderParameters.getIndexOfBottomLayer());
                                break;


                            case "first":
                            case "f":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setFirstLineToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setFirstLineToRender(0);
                                break;

                            case "last":
                            case "l":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setLastLineToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setLastLineToRender(renderParameters.getNumberOfLines());
                                break;

                            case "colour":
                            case "c":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "type":
                                    case "ty":
                                        colourSegmentsFromType();
                                        reloadSegments();
                                        renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TYPE);
                                        break;

                                    case "tool":
                                    case "to":
                                        renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TOOL);
                                        break;

                                    case "data":
                                    case "d":
                                        int dataIndex = -1;
                                        if (commandScanner.hasNextInt()) {
                                            dataIndex = commandScanner.nextInt();
                                            if (dataIndex < 0 || dataIndex >= Entity.N_DATA_VALUES)
                                                dataIndex = -1;
                                        }
                                        else {
                                            commandParameter = commandScanner.next().toLowerCase();
                                            switch (commandParameter)
                                            {
                                                case "a":
                                                    dataIndex = 0;
                                                    break;
                                                case "b":
                                                    dataIndex = 1;
                                                    break;
                                                case "d":
                                                    dataIndex = 2;
                                                    break;
                                                case "e":
                                                    dataIndex = 3;
                                                    break;
                                                case "f":
                                                    dataIndex = 4;
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                        if (dataIndex >= 0)
                                        {
                                            colourSegmentsFromData(dataIndex, minDataValues[dataIndex], maxDataValues[dataIndex]);
                                            reloadSegmentColours();
                                            renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TYPE);
                                        }
                                        break;
                                
                                    default:
                                        System.out.println("Unrecognised colour mode in command " + command);
                                }
                                break;

                            default:
                                STENO.error("Ignoring command " + command);
                                break;
                        }
                    }
                }
                catch (RuntimeException ex) {
                    STENO.exception("Command parsing error", ex);
                }
            }
        }
    }

    private void reloadSegments() {
        masterRenderer.processSegmentEntity(null);
        segmentLoader.cleanUp();
        if (segments != null && segments.size() > 0) {
            masterRenderer.processSegmentEntity(segmentLoader.loadToVAO(segments));           
        }
    }
    
    private void reloadSegmentColours() {
        if (segments != null &&
            segments.size() > 0 &&
            masterRenderer.getSegmentEntity() != null) {
            segmentLoader.reloadColours(masterRenderer.getSegmentEntity(), segments);
        }
    }
        
    private void colourSegmentsFromType() {
        if (segments != null && segments.size() > 0) {
            segments.forEach(segment -> {
                segment.setColour(segment.getTypeColour());
            });
        }
    }
    
    private void colourSegmentsFromData(int dataIndex, float minValue, float maxValue) {
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
