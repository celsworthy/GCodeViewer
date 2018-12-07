package celtech.gcodeviewer.shaders;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.List;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

public abstract class ShaderProgram {
    
    private static final int MAX_UNIFORM_VECTOR = 8;
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer UNIFORM_VECTOR_BUFFER = BufferUtils.createFloatBuffer(MAX_UNIFORM_VECTOR * 4);
    
    protected static final String SHADER_DIRECTORY = "/celtech/gcodeviewer/resources/";
    
    protected final int programId;
    private final int vertexShaderId;
    private final int geometryShaderId;
    private final int fragmentShaderId;
    
    public ShaderProgram(String vertexFile, String fragmentFile) {
        this(vertexFile, null, fragmentFile);
    }

    public ShaderProgram(String vertexFile, String geometryFile, String fragmentFile) {
        vertexShaderId = loadShader(vertexFile, GL_VERTEX_SHADER);
        if (geometryFile != null)
            geometryShaderId = loadShader(geometryFile, GL_GEOMETRY_SHADER);
        else
            geometryShaderId = -1;
        fragmentShaderId = loadShader(fragmentFile, GL_FRAGMENT_SHADER);
        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        if (geometryShaderId != -1)
            glAttachShader(programId, geometryShaderId);
        glAttachShader(programId, fragmentShaderId);
        bindAttributes();
        glLinkProgram(programId);
        if(glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
        {
            System.err.println(glGetProgramInfoLog(programId, 1024));
        }
        glValidateProgram(programId);
        if(glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE)
        {
            System.err.println(glGetProgramInfoLog(programId, 1024));
        }
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
        if (geometryShaderId != -1)
            glDetachShader(programId, geometryShaderId);            
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        if (geometryShaderId != -1)
            glDeleteShader(geometryShaderId);            
        glDeleteShader(fragmentShaderId);
        glDeleteProgram(programId);        
    }
    
    protected void bindAttribute(int attribute, String variableName) {
        glBindAttribLocation(programId, attribute, variableName);
    }
    
    protected void loadFloat(int location, float value) {
        glUniform1f(location, value);
    }
    
    protected void loadInt(int location, int value) {
        glUniform1i(location, value);
    }
    
    protected void loadVector3(int location, Vector3f vector) {
        glUniform3f(location, vector.x, vector.y, vector.z);
    }
    
    protected void loadVector4(int location, Vector4f vector) {
        glUniform4f(location, vector.x, vector.y, vector.z, vector.w);
    }

    protected void loadVector3ArraytoUVector4(int location, List<Vector3f> vl) {
        UNIFORM_VECTOR_BUFFER.clear();
        for (int i = 0; i < MAX_UNIFORM_VECTOR; ++i)
        {
            if (i < vl.size())
            {
                Vector3f v4 = vl.get(i);
                UNIFORM_VECTOR_BUFFER.put(v4.getX());
                UNIFORM_VECTOR_BUFFER.put(v4.getY());
                UNIFORM_VECTOR_BUFFER.put(v4.getZ());
                UNIFORM_VECTOR_BUFFER.put(1.0f);
            }
            else
            {
                UNIFORM_VECTOR_BUFFER.put(0.0f);
                UNIFORM_VECTOR_BUFFER.put(0.0f);
                UNIFORM_VECTOR_BUFFER.put(0.0f);
                UNIFORM_VECTOR_BUFFER.put(0.0f);
            }
        }
        UNIFORM_VECTOR_BUFFER.flip();
        glUniform4fv(location, UNIFORM_VECTOR_BUFFER);
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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ShaderProgram.class.getResourceAsStream(file)))) {
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
