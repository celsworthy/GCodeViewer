package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.comms.CommandQueue;
import java.io.IOException;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * Main entry point for the program. Window initialisation happens here.
 * 
 * @author George Salter
 */
public class GCodeViewer {

    private static final Stenographer STENO = StenographerFactory.getStenographer(GCodeViewer.class.getName());
    
    private static final String PROGRAM_NAME = "G-Code Viewer";
    
    private final int windowWidth = 1280;
    private final int windowHeight = 700;  
    private GCodeViewerConfiguration configuration = null;

    private long windowId;
    
    public CommandQueue commandQueue;
    
    /**
     * Run the program 
     */
    public void run() {
        STENO.debug("Running " + PROGRAM_NAME);
        
        configuration = GCodeViewerConfiguration.loadFromFile("D:\\CEL\\Dev\\GCodeViewer\\GCodeViewer.config");
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowId);
        glfwDestroyWindow(windowId);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    /**
     * Initialisation happens here, this mostly encompasses GLFW and the 
     * associated window.
     */
    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_FLOATING, GLFW_TRUE); // the window will stay on top.
        // Create the window
        windowId = glfwCreateWindow(windowWidth, windowHeight, "GCodeViewer", NULL, NULL);
        if ( windowId == NULL ) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(windowId, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowId, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            // Center the window
            glfwSetWindowPos(
                windowId,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(windowId);
        // Enable v-sync
        glfwSwapInterval(1);
        
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        
        // Make the window visible
        glfwShowWindow(windowId);
    }
    
    private void loop() {
        commandQueue = new CommandQueue();
        commandQueue.start();
        
        RenderingEngine renderingEngine = new RenderingEngine(windowId,
                                                              windowWidth,
                                                              windowHeight,
                                                              configuration,
                                                              commandQueue);
        renderingEngine.start();
    }
    
    /**
     * Start of the program, simply calls {@link GCodeViewer#run()}
     * 
     * @param args 
     */
    public static void main(String[] args) {
        new GCodeViewer().run();
    }
}
