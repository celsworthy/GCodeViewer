package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandQueue;
import celtech.gcodeviewer.engine.renderers.MasterRenderer;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.CenterPoint;
import celtech.gcodeviewer.entities.Floor;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.entities.PrintVolume;
import celtech.gcodeviewer.gcode.GCodeConvertor;
import celtech.gcodeviewer.gcode.Layer;
import celtech.gcodeviewer.gcode.NodeHandler;
import celtech.gcodeviewer.utils.CubeConstants;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
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
    
    private final static Stenographer STENO = StenographerFactory.getStenographer(
            RenderingEngine.class.getName());
    
    private final long windowId;
    private int windowWidth;
    private int windowHeight;
    
    private final float printVolumeWidth = 210;
    private final float printVolumeHeight = 100;
    private final float printVolumeDepth = 150;
    
    private int topLayerToRender = 0;
    
    private final MasterRenderer masterRenderer;
    
    private final ModelLoader modelLoader = new ModelLoader();
    
    private final CommandQueue commandQueue;
        
    private List<LayerNode> layerNodes = null;
    private List<Layer> layers = null;
    private NodeHandler nodeHandler = null;

    public RenderingEngine(long windowId, int windowWidth, int windowHeight, CommandQueue commandQueue) {
        this.windowId = windowId;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.commandQueue = commandQueue;
        masterRenderer = new MasterRenderer(windowWidth, windowHeight);
    }
    
    public void start() {      
        STENO.debug("Starting RenderingEngine.");

        createWindowResizeCallback();
        
        RawModel model = modelLoader.loadToVAO(CubeConstants.VERTICES, CubeConstants.NORMALS, CubeConstants.INDICES);
        RawModel lineModel = modelLoader.loadToVAO(new float[]{-0.5f, 0, 0, 0.5f, 0, 0});
        
        Vector3f centerPointStartPos = new Vector3f(-printVolumeWidth / 2, printVolumeHeight / 2, printVolumeDepth / 2);
        Vector3f lightPos = new Vector3f(-printVolumeWidth, printVolumeHeight * 2, 0);
        
        Light light = new Light(lightPos, new Vector3f(1, 1, 1));
        CenterPoint centerPoint = new CenterPoint(centerPointStartPos, lineModel);
        Camera camera = new Camera(windowId, centerPoint);
        Floor floor = new Floor(printVolumeWidth, printVolumeDepth, modelLoader);
        PrintVolume printVolume = new PrintVolume(lineModel, printVolumeWidth, printVolumeHeight, printVolumeDepth);
        nodeHandler = new NodeHandler(model, lineModel);
        
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            if ( key == GLFW.GLFW_KEY_UP)
                    topLayerToRender += 4;
            if ( key == GLFW.GLFW_KEY_DOWN)
                    topLayerToRender -= 4;
        });

        STENO.debug("Running rendering loop.");
        while (!glfwWindowShouldClose(windowId)) {
            GL11.glViewport(0, 0, windowWidth, windowHeight);
            camera.move();
            
            if (layers != null)
            {
                layersToRenderCheck(layers);
                layers.forEach(masterRenderer::processLayer);
                masterRenderer.processFloor(floor);
            }
            if(centerPoint.isRendered()) {
                centerPoint.getLineEntities().forEach(masterRenderer::processLine);
            }
            
            printVolume.getLineEntities().forEach(masterRenderer::processLine);
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
    }
    
    private void layersToRenderCheck(List<Layer> layers) {
        if (topLayerToRender > layers.size() - 1)
            topLayerToRender = layers.size() - 1;
        if (topLayerToRender < 0)
            topLayerToRender = 0;
        layers.forEach((layer) -> {
            layer.setRendered(layer.getLayerNo() <= topLayerToRender);
        });
    }
    
    private void createWindowResizeCallback() {
        glfwSetWindowSizeCallback(windowId, (window, width, height) -> {
            windowWidth = width;
            windowHeight = height;
            masterRenderer.createProjectionMatrix(windowWidth, windowHeight);
            masterRenderer.reLoadProjectionMatrix();
        });
    }

    private void processCommands() {
        while (commandQueue.commandAvailable()) {
            String command = commandQueue.getNextCommandFromQueue().trim();
            System.out.println("Processing command " + command);
            if (command.equalsIgnoreCase("q")) {
                glfwSetWindowShouldClose(windowId, true);
                break;
            }
            else {
                Scanner commandScanner = new Scanner(command);
                if (commandScanner.hasNext()) {
                    String commandWord = commandScanner.next().toLowerCase();
                    switch (commandWord)
                    {
                        case "load":
                            try {
                                String gCodeFile = commandScanner.nextLine().trim();
                                GCodeConvertor gCodeConvertor = new GCodeConvertor();
                                layerNodes = gCodeConvertor.convertGCode(gCodeFile);
                                layers = nodeHandler.processLayerNodes(layerNodes);
                                topLayerToRender = layers.size() - 1;
                                System.out.println("layerCount " + layers.size());
                            }
                            catch (RuntimeException ex)
                            {
                                layerNodes = null;
                                layers = null;
                                topLayerToRender = 0;
                                System.out.println("Parsing error");
                            }
                            break;
                            
                        case "top":
                            if (commandScanner.hasNextInt()) {
                                topLayerToRender = commandScanner.nextInt();
                            }
                            break;
                    }
                }
            }
        }
    }
}
