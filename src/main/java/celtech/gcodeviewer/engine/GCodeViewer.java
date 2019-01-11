package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.i18n.MessageLookup;
import celtech.gcodeviewer.i18n.languagedata.LanguageData;
import celtech.gcodeviewer.comms.CommandHandler;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Locale;
import libertysystems.stenographer.LogLevel;

import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glGetError;

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
    private boolean floatingWindow = true;

    private long windowId;
    
    public CommandHandler commandHandler;
    
    /**
     * Run the program 
     */
    public void run(String[] argv) {
        System.out.println("Hello!");
        StenographerFactory.changeAllLogLevels(libertysystems.stenographer.LogLevel.INFO);
        STENO.debug("Running " + PROGRAM_NAME);
        
        configuration = GCodeViewerConfiguration.loadFromConfig();

        String gCodeFile = null;
        String languageTag = null;
        for (String arg : argv) {
            if (arg.startsWith("-")) {
                if (arg.startsWith("-l")) {
                    if (arg.length() > 2)
                        languageTag = arg.substring(2);
                }
                else if (arg.equals("-t")) {
                    floatingWindow = false;
                }
            }
            else
                gCodeFile = arg;
        }
        
        MessageLookup.loadMessages(configuration.getApplicationInstallDirectory(),
                                   MessageLookup.getDefaultApplicationLocale(languageTag));
            
        init();
        loop(gCodeFile);

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowId);
        glfwDestroyWindow(windowId);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        System.out.println("Goodbye!");
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
        if (floatingWindow)
            glfwWindowHint(GLFW_FLOATING, GLFW_TRUE); // the window will stay on top.
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
        GLCapabilities caps = GL.createCapabilities();
        Callback debugProc = GLUtil.setupDebugMessageCallback();
        
        if (caps.OpenGL43) {
            GL43.glDebugMessageControl(GL43.GL_DEBUG_SOURCE_API,
                                       GL43.GL_DEBUG_TYPE_OTHER,
                                       GL43.GL_DEBUG_SEVERITY_NOTIFICATION,
                                       (IntBuffer)null,
                                       false);
        }
        else if (caps.GL_KHR_debug) {
            KHRDebug.glDebugMessageControl(KHRDebug.GL_DEBUG_SOURCE_API,
                                           KHRDebug.GL_DEBUG_TYPE_OTHER,
                                           KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION,
                                           (IntBuffer)null,
                                           false);
        }
        else if (caps.GL_ARB_debug_output) {
            glDebugMessageControlARB(GL_DEBUG_SOURCE_API_ARB,
                                     GL_DEBUG_TYPE_OTHER_ARB,
                                     GL_DEBUG_SEVERITY_LOW_ARB,
                                     (IntBuffer)null,
                                     false);
        }
        
        // Make the window visible
        glfwShowWindow(windowId);
    }
    
    private void loop(String gCodeFile) {
        RenderingEngine renderingEngine = new RenderingEngine(windowId,
                                                              windowWidth,
                                                              windowHeight,
                                                              configuration);
        renderingEngine.start(gCodeFile);
    }
    
    /**
     * Start of the program, simply calls {@link GCodeViewer#run()}
     * 
     * @param args 
     */
    public static void main(String[] argv) {
        GCodeViewer viewer = new GCodeViewer();
        viewer.run(argv);
    }
}
