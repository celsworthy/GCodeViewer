package celtech.gcodeviewer.shaders;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Matrix4f;

public abstract class ShaderProgram {
    
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    
    protected static final String SHADER_DIRECTORY = "src/main/java/celtech/gcodeviewer/shaders/";
    
    protected final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;
    
    public ShaderProgram(String vertexFile, String fragmentFile) {
        vertexShaderId = loadShader(vertexFile, GL_VERTEX_SHADER);
        fragmentShaderId = loadShader(fragmentFile, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        bindAttributes();
        glLinkProgram(programId);
        glValidateProgram(programId);
        getAllUniformLocations();
    }
    
    protected abstract void getAllUniformLocations();
    
    protected abstract void bindAttributes();
    
    protected int getUniformLocation(String uniformName) {
        return glGetUniformLocation(programId, uniformName);
    }
    
    public void start() {
        glUseProgram(programId);
    }
    
    public void stop() {
        glUseProgram(0);
    }
    
    public void cleanUp() {
        stop();
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        glDeleteProgram(programId);        
    }
    
    protected void bindAttribute(int attribute, String variableName) {
        glBindAttribLocation(programId, attribute, variableName);
    }
    
    protected void loadFloat(int location, float value) {
        glUniform1f(location, value);
    }
    
    protected void loadVector3(int location, Vector3f vector) {
        glUniform3f(location, vector.x, vector.y, vector.z);
    }
    
    protected void loadBoolean(int location, boolean value) {
        float toLoad = 0;
        if(value) {
            toLoad = 1;
        }
        glUniform1f(location, toLoad);
    }
    
    protected void loadMatrix(int location, Matrix4f matrix) {
        matrix.store(MATRIX_BUFFER);
        MATRIX_BUFFER.flip();
        glUniformMatrix4fv(location, false, MATRIX_BUFFER);
    }
    
    private static int loadShader(String file, int type) {
        StringBuilder shaderSource = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                shaderSource.append(line).append("\n");
            }
            
        } catch(IOException e) {
            System.err.println("Could not read file!");
            e.printStackTrace();
        }
        
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, shaderSource);
        glCompileShader(shaderId);
        if(glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Could not compile shader " + file);
        }
        return shaderId;
    }
}
