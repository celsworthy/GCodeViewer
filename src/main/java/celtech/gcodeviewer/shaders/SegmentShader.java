package celtech.gcodeviewer.shaders;

import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Light;
import static celtech.gcodeviewer.shaders.ShaderProgram.SHADER_DIRECTORY;
import celtech.gcodeviewer.utils.MatrixUtils;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
//import org.lwjgl.util.vector.Matrix4f;
//import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author George Salter
 */
public class SegmentShader  extends ShaderProgram {
    private static final String VERTEX_FILE = SHADER_DIRECTORY + "segmentVertexShader.txt";
    private static final String GEOMETRY_FILE = SHADER_DIRECTORY + "segmentGeometryShader.txt";
    private static final String FRAGMENT_FILE = SHADER_DIRECTORY + "segmentFragmentShader.txt";
    
    private int location_lightPosition;
    private int location_lightColour;
    private int location_compositeMatrix;
    private int location_topVisibleLayer;
    private int location_bottomVisibleLayer;
    private int location_firstSelectedLine;
    private int location_lastSelectedLine;
    private int location_showFlags;
    private int location_showTools;
    private int location_toolColours;
    private int location_selectColour;
    
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
            
    public SegmentShader() {
        super(VERTEX_FILE, GEOMETRY_FILE, FRAGMENT_FILE);
    }
            
    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "direction");
        super.bindAttribute(2, "normal");
        super.bindAttribute(3, "colour");
        super.bindAttribute(4, "attributes");
    }

    @Override
    protected void getAllUniformLocations() {
        location_lightPosition = super.getUniformLocation("lightPosition");
        location_lightColour = super.getUniformLocation("lightColour");
        location_compositeMatrix = super.getUniformLocation("compositeMatrix");
        location_topVisibleLayer = super.getUniformLocation("topVisibleLayer");
        location_bottomVisibleLayer = super.getUniformLocation("bottomVisibleLayer");
        location_firstSelectedLine = super.getUniformLocation("firstSelectedLine");
        location_lastSelectedLine = super.getUniformLocation("lastSelectedLine");
        location_showFlags = super.getUniformLocation("showFlags");
        location_showTools = super.getUniformLocation("showTools");
        location_toolColours = super.getUniformLocation("toolColours");
        location_selectColour = super.getUniformLocation("selectColour");
    }
    
    public void setViewMatrix(Camera camera) {
        this.viewMatrix = MatrixUtils.createViewMatrix(camera);
    }
    
    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }
    
    public void loadCompositeMatrix() {
        Matrix4f composite = new Matrix4f(projectionMatrix);
        composite.mul(viewMatrix);
        
        Matrix4f mx = new Matrix4f();
        mx.identity();

        // Mirror in X.
        mx.m00(-1.0f);
        // Swap Y and Z
        mx.m11(0.0f);
        mx.m12(1.0f);
        mx.m21(1.0f);
        mx.m22(0.0f);
        composite.mul(mx);
        
        super.loadMatrix(location_compositeMatrix, composite);
    }
    
    public void loadLight(Light light) {
        super.loadVector3(location_lightPosition, light.getPosition());
        super.loadVector3(location_lightColour, light.getColour());
    }
    
    public void loadLayerLimits(int topVisibleLayer, int bottomVisibleLayer) {
        super.loadInt(location_topVisibleLayer, topVisibleLayer);
        super.loadInt(location_bottomVisibleLayer, bottomVisibleLayer);
    }

    public void loadLineLimits(int firstSelectedLine, int lastSelectedLine) {
        super.loadInt(location_firstSelectedLine, firstSelectedLine);
        super.loadInt(location_lastSelectedLine, lastSelectedLine);
    }

    public void loadShowFlags(int showFlags) {
        super.loadInt(location_showFlags, showFlags);
    }

    public void loadShowTools(int showTools) {
        super.loadInt(location_showTools, showTools);
    }

    public void loadToolColours(List<Vector3f> toolColours) {
        super.loadVector3ArraytoUVector4(location_toolColours, toolColours);
    }

    public void loadSelectColour(Vector3f selectColour) {
        super.loadVector3(location_selectColour, selectColour);
    }
}
