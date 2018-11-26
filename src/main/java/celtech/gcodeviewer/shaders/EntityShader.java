package celtech.gcodeviewer.shaders;

import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Light;
import static celtech.gcodeviewer.shaders.ShaderProgram.SHADER_DIRECTORY;
import celtech.gcodeviewer.utils.MatrixUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author George Salter
 */
public class EntityShader  extends ShaderProgram {
    private static final String VERTEX_FILE = SHADER_DIRECTORY + "entityVertexShader.txt";
    private static final String GEOMETRY_FILE = SHADER_DIRECTORY + "entityGeometryShader.txt";
    private static final String FRAGMENT_FILE = SHADER_DIRECTORY + "entityFragmentShader.txt";
    
    private int location_lightPosition;
    private int location_lightColour;
    private int location_compositeMatrix;
    private int location_topVisibleLayer;
    private int location_bottomVisibleLayer;
    private int location_firstVisibleLine;
    private int location_lastVisibleLine;
    private int location_showFlags;
    private int location_t0Colour;
    private int location_t1Colour;
    
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
            
    public EntityShader() {
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
        location_firstVisibleLine = super.getUniformLocation("firstVisibleLine");
        location_lastVisibleLine = super.getUniformLocation("lastVisibleLine");
        location_showFlags = super.getUniformLocation("showFlags");
        location_t0Colour = super.getUniformLocation("t0Colour");
        location_t1Colour = super.getUniformLocation("t1Colour");
    }
    
    public void setViewMatrix(Camera camera) {
        this.viewMatrix = MatrixUtils.createViewMatrix(camera);
    }
    
    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }
    
    public void loadCompositeMatrix() {
        Matrix4f t = new Matrix4f();
        Matrix4f mx = new Matrix4f();
        Matrix4f composite = new Matrix4f();
        Matrix4f.mul(projectionMatrix, viewMatrix, t);        
        // Mirror in X.
        mx.m00 = -1.0f;
        // Swap Y and Z
        mx.m11 = 0.0f;
        mx.m12 = 1.0f;
        mx.m21 = 1.0f;
        mx.m22 = 0.0f;
        Matrix4f.mul(t, mx, composite);
        
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

    public void loadLineLimits(int firstVisibleLine, int lastVisibleLine) {
        super.loadInt(location_firstVisibleLine, firstVisibleLine);
        super.loadInt(location_lastVisibleLine, lastVisibleLine);
    }

    public void loadShowFlags(int showFlags) {
        super.loadInt(location_showFlags, showFlags);
    }

    public void loadToolColours(Vector3f t0Colour, Vector3f t1Colour) {
        super.loadVector3(location_t0Colour, t0Colour);
        super.loadVector3(location_t1Colour, t1Colour);
    }
}
