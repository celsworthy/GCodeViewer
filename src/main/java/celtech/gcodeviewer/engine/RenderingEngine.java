package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandQueue;
import celtech.gcodeviewer.engine.renderers.MasterRenderer;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.PrintVolume;
import celtech.gcodeviewer.utils.CubeConstants;
import java.util.List;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
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
    
    public static enum ColourMode {
        COLOUR_AS_TYPE,
        COLOUR_AS_TOOL;
    }
        
    private final static Stenographer STENO = StenographerFactory.getStenographer(
            RenderingEngine.class.getName());
    
    private final long windowId;
    private int windowWidth;
    private int windowHeight;
    
    private final float printVolumeWidth;
    private final float printVolumeHeight;
    private final float printVolumeDepth;
    
    private int indexOfTopLayer = 0;
    private int indexOfBottomLayer = 0;
    private int topLayerToRender = 0;
    private int bottomLayerToRender = 0;
    
    private int numberOfLines;
    private int firstLineToRender = 0;
    private int lastLineToRender = 0;
    private List<Entity> lines = null;
    
    private boolean showMoves = false;
    private boolean showTool0 = true;
    private boolean showTool1 = true;
    private ColourMode colourMode = ColourMode.COLOUR_AS_TOOL;
    
    private final MasterRenderer masterRenderer;
    
    private final ModelLoader modelLoader = new ModelLoader();

    private final EntityLoader entityLoader = new EntityLoader();
    
    private final GCodeViewerConfiguration configuration;

    private final CommandQueue commandQueue;

    private RawModel lineModel;
    private RawModel model;

    private CenterPoint centerPoint = null;
    Floor floor = null;
    PrintVolume printVolume = null;
    
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

        masterRenderer = new MasterRenderer(windowWidth, windowHeight);
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
        Camera camera = new Camera(windowId, centerPoint);
        floor = new Floor(printVolumeWidth, printVolumeDepth, modelLoader);
        printVolume = new PrintVolume(lineModel, printVolumeWidth, printVolumeHeight, printVolumeDepth);
        
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
                int step = 4;
                if (mods == GLFW.GLFW_MOD_SHIFT)
                    step = 1;
                else if (mods == GLFW.GLFW_MOD_CONTROL)
                    step = 2;
                if (key == GLFW.GLFW_KEY_UP)
                        topLayerToRender += step;
                if (key == GLFW.GLFW_KEY_DOWN)
                        topLayerToRender -= step;
            }
        });

        masterRenderer.processFloor(floor);
        masterRenderer.processCentrePoint(centerPoint);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);
            
        STENO.debug("Running rendering loop.");
        while (!glfwWindowShouldClose(windowId)) {
            GL11.glViewport(0, 0, windowWidth, windowHeight);

            camera.move();
            checkRenderLimits();

            masterRenderer.render(camera, light);
            
            glfwSwapBuffers(windowId); // swap the color buffers
            
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
            
            if (commandQueue.commandAvailable())
                processCommands();
        }
        
        masterRenderer.cleanUp();
        modelLoader.cleanUp();
        entityLoader.cleanUp();
    }
    
    private void checkRenderLimits() {
        if (bottomLayerToRender < indexOfBottomLayer)
            bottomLayerToRender = indexOfBottomLayer;
        if (topLayerToRender > indexOfTopLayer)
            topLayerToRender = indexOfTopLayer;
        if (topLayerToRender < bottomLayerToRender)
            topLayerToRender = bottomLayerToRender;

        if (firstLineToRender < 0)
            firstLineToRender = 0;
        if (lastLineToRender > numberOfLines)
            lastLineToRender = numberOfLines;
        if (lastLineToRender < firstLineToRender)
            lastLineToRender = firstLineToRender;

        masterRenderer.setTopLayerToShow(topLayerToRender);
        masterRenderer.setBottomLayerToShow(bottomLayerToRender);
        masterRenderer.setFirstLineToShow(firstLineToRender);
        masterRenderer.setLastLineToShow(lastLineToRender);
        masterRenderer.setShowFlags(showMoves, showTool0, showTool1);
        masterRenderer.setColourMode(colourMode);
        masterRenderer.setToolColours(configuration.getTool0Colour(), configuration.getTool1Colour());
    }
    
    private void createWindowResizeCallback() {
        glfwSetWindowSizeCallback(windowId, (window, width, height) -> {
            windowWidth = width;
            windowHeight = height;
            masterRenderer.createProjectionMatrix(windowWidth, windowHeight);
            masterRenderer.reLoadProjectionMatrix();
        });
    }

    private void loadGCodeFile(String gCodeFile) {
        try {
            GCodeProcessor processor = new GCodeProcessor();
            GCodeVisualiser visualiser = new GCodeVisualiser(model, configuration);
            if (processor.processFile(gCodeFile, visualiser))
            {
                lines = visualiser.getLines();
                //numberOfLines = processor.numberOfLines();
                numberOfLines = lines.get(lines.size() - 1).getLineNumber();
                firstLineToRender = 0;
                lastLineToRender = numberOfLines;
                System.out.println("Number of lines = " + numberOfLines);

                indexOfTopLayer = processor.getNumberOfTopLayer();
                indexOfBottomLayer = processor.getNumberOfBottomLayer();
                if (indexOfTopLayer <= Entity.NULL_LAYER)
                    indexOfTopLayer = numberOfLines;
                if (indexOfBottomLayer <= Entity.NULL_LAYER)
                    indexOfBottomLayer = 0;
                topLayerToRender = indexOfTopLayer;
                bottomLayerToRender = indexOfBottomLayer;
                System.out.println("Top layer = " + indexOfTopLayer);
            }
        }
        catch (RuntimeException ex)
        {
            numberOfLines = 0;
            firstLineToRender = 0;
            lastLineToRender = 0;

            indexOfTopLayer = 0;
            indexOfBottomLayer = 0;
            topLayerToRender = 0;
            bottomLayerToRender = 0;
            System.out.println("Parsing error");
        }
        masterRenderer.clearEntities();
        entityLoader.cleanUp();
//        if (lines != null && lines.size() > 0)
//            lines.forEach(masterRenderer::processEntity);
        if (lines != null && lines.size() > 0) {
            masterRenderer.processRawEntity(entityLoader.loadToVAO(lines));
        }
        masterRenderer.processCentrePoint(centerPoint);
        masterRenderer.processFloor(floor);
        printVolume.getLineEntities().forEach(masterRenderer::processLine);
    }
    
    private void processCommands() {
        while (commandQueue.commandAvailable()) {
            String command = commandQueue.getNextCommandFromQueue().trim();
            //System.out.println("Processing command " + command);
            if (command.equalsIgnoreCase("q") || command.equalsIgnoreCase("quit")) {
                glfwSetWindowShouldClose(windowId, true);
                break;
            }
            else {
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

                        case "show":
                            try {
                                String commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                        showMoves = true;
                                        break;

                                    case "t0":
                                        showTool0 = true;
                                        break;

                                    case "t1":
                                        showTool1 = true;
                                        break;
                                        
                                    default:
                                        System.out.println("Ignoring command " + command);
                                }
                            }
                            catch (RuntimeException ex) {
                                System.out.println("Command parsing error");
                            }
                            break;

                        case "hide":
                            try {
                                String commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                        showMoves = false;
                                        break;

                                    case "t0":
                                        showTool0 = false;
                                        break;

                                    case "t1":
                                        showTool1 = false;
                                        break;
                                        
                                    default:
                                        System.out.println("Ignoring command " + command);
                                }
                            }
                            catch (RuntimeException ex) {
                                System.out.println("Command parsing error");
                            }
                            break;

                        case "top":
                            if (commandScanner.hasNextInt()) {
                                topLayerToRender = commandScanner.nextInt();
                            }
                            else {
                                topLayerToRender = indexOfTopLayer;
                            }
                            break;

                        case "bottom":
                            if (commandScanner.hasNextInt()) {
                                bottomLayerToRender = commandScanner.nextInt();
                            }
                            else {
                                bottomLayerToRender = indexOfBottomLayer;
                            }
                            break;


                        case "first":
                            if (commandScanner.hasNextInt()) {
                                firstLineToRender = commandScanner.nextInt();
                            }
                            else {
                                firstLineToRender = 0;
                            }
                            break;

                        case "last":
                            if (commandScanner.hasNextInt()) {
                                lastLineToRender = commandScanner.nextInt();
                            }
                            else {
                                lastLineToRender = numberOfLines;
                            }
                            break;
                            
                        case "colour":
                            try {
                                String commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "type":
                                        colourMode = ColourMode.COLOUR_AS_TYPE;
                                        break;

                                    case "tool":
                                        colourMode = ColourMode.COLOUR_AS_TOOL;
                                        break;
                                        
                                    default:
                                        System.out.println("Ignoring command " + command);
                                }
                            }
                            catch (RuntimeException ex) {
                                System.out.println("Command parsing error");
                            }
                            break;

                        default:
                            System.out.println("Ignoring command " + command);
                            break;
                    }
                }
            }
        }
    }
}
