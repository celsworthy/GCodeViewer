package celtech.gcodeviewer.shaders;

import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.utils.MatrixUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class StaticShader extends ShaderProgram {

    private static final String VERTEX_FILE = SHADER_DIRECTORY + "staticVertexShader.txt";
    private static final String FRAGMENT_FILE = SHADER_DIRECTORY + "staticFragmentShader.txt";
    
    private int location_transformationMatrix;
    private int location_viewMatrix;
    private int location_projectionMatrix;
    private int location_lightPosition;
    private int location_lightColour;
    private int location_modelColour;
    
    public StaticShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }
            
    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "normal");
    }

    @Override
    protected void getAllUniformLocations() {
        location_transformationMatrix = super.getUniformLocation("transformationMatrix");
        location_viewMatrix = super.getUniformLocation("viewMatrix");
        location_projectionMatrix = super.getUniformLocation("projectionMatrix");
        location_lightPosition = super.getUniformLocation("lightPosition");
        location_lightColour = super.getUniformLocation("lightColour");
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
    
    public void loadLight(Light light) {
        super.loadVector3(location_lightPosition, light.getPosition());
        super.loadVector3(location_lightColour, light.getColour());
    }
    
    public void loadColour(Vector3f colour) {
        super.loadVector3(location_modelColour, colour);
    }
}
