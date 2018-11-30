package celtech.gcodeviewer.shaders;

import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Light;
import static celtech.gcodeviewer.shaders.ShaderProgram.SHADER_DIRECTORY;
import celtech.gcodeviewer.utils.MatrixUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Tony Aldhous
 */
public class MoveShader  extends ShaderProgram {
    private static final String VERTEX_FILE = SHADER_DIRECTORY + "moveVertexShader.txt";
    private static final String FRAGMENT_FILE = SHADER_DIRECTORY + "moveFragmentShader.txt";
    
    private int location_compositeMatrix;
    private int location_topVisibleLayer;
    private int location_bottomVisibleLayer;
    private int location_firstVisibleLine;
    private int location_lastVisibleLine;
    private int location_moveColour;
    
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
            
    public MoveShader() {
        super(VERTEX_FILE, null, FRAGMENT_FILE);
    }
            
    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "attributes");
    }

    @Override
    protected void getAllUniformLocations() {
        location_compositeMatrix = super.getUniformLocation("compositeMatrix");
        location_topVisibleLayer = super.getUniformLocation("topVisibleLayer");
        location_bottomVisibleLayer = super.getUniformLocation("bottomVisibleLayer");
        location_firstVisibleLine = super.getUniformLocation("firstVisibleLine");
        location_lastVisibleLine = super.getUniformLocation("lastVisibleLine");
        location_moveColour = super.getUniformLocation("moveColour");
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
    
    public void loadLayerLimits(int topVisibleLayer, int bottomVisibleLayer) {
        super.loadInt(location_topVisibleLayer, topVisibleLayer);
        super.loadInt(location_bottomVisibleLayer, bottomVisibleLayer);
    }

    public void loadLineLimits(int firstVisibleLine, int lastVisibleLine) {
        super.loadInt(location_firstVisibleLine, firstVisibleLine);
        super.loadInt(location_lastVisibleLine, lastVisibleLine);
    }

    public void loadMoveColour(Vector3f moveColour) {
        super.loadVector3(location_moveColour, moveColour);
    }
}
