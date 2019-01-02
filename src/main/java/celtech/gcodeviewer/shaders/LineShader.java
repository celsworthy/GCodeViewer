package celtech.gcodeviewer.shaders;

import celtech.gcodeviewer.entities.Camera;
import static celtech.gcodeviewer.shaders.ShaderProgram.SHADER_DIRECTORY;
import celtech.gcodeviewer.utils.MatrixUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 *
 * @author George Salter
 */
public class LineShader  extends ShaderProgram {
    private static final String VERTEX_FILE = SHADER_DIRECTORY + "lineVertexShader.txt";
    private static final String FRAGMENT_FILE = SHADER_DIRECTORY + "lineFragmentShader.txt";
    
    private int location_transformationMatrix;
    private int location_viewMatrix;
    private int location_projectionMatrix;
    private int location_modelColour;
    
    public LineShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }
            
    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
    }

    @Override
    protected void getAllUniformLocations() {
        location_transformationMatrix = super.getUniformLocation("transformationMatrix");
        location_viewMatrix = super.getUniformLocation("viewMatrix");
        location_projectionMatrix = super.getUniformLocation("projectionMatrix");
        location_modelColour = super.getUniformLocation("modelColour");
    }
    
    public void loadTransformationMatrix(Matrix4f transformation) {
        super.loadMatrix(location_transformationMatrix, transformation);
    }
    
    public void loadViewMatrix(Camera camera) {
        Matrix4f viewMatrix = MatrixUtils.createViewMatrix(camera);
        super.loadMatrix(location_viewMatrix, viewMatrix);
    }
    
    public void loadProjectionMatrix(Matrix4f projection) {
        super.loadMatrix(location_projectionMatrix, projection);
    }
    
    public void loadColour(Vector3f colour) {
        super.loadVector3(location_modelColour, colour);
    }
}
