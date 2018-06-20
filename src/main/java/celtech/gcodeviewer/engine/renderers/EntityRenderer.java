package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawModel;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.shaders.StaticShader;
import celtech.gcodeviewer.utils.MatrixUtils;
import java.util.List;
import java.util.Map;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.util.vector.Matrix4f;

public class EntityRenderer {
   
    private final StaticShader shader;
    
    public EntityRenderer(StaticShader shader, Matrix4f projectionMatrix) {
        this.shader = shader;
        loadProjectionMatrix(projectionMatrix);
    }
    
    public final void loadProjectionMatrix(Matrix4f projectionMatrix) {
        shader.start();
        shader.loadProjectionMatrix(projectionMatrix);
        shader.stop();
    }
    
    public void render(Map<RawModel, List<Entity>> entities) {
        entities.keySet().stream().forEach(model -> renderModelEntities(model, entities.get(model)));
    }
    
    public void renderModelEntities(RawModel model, List<Entity> modelEntities) {
        prepareRawModel(model);
        modelEntities.forEach(entity -> {
            prepareInstance(entity);
            glDrawElements(GL_TRIANGLES, model.getVertexCount(), GL_UNSIGNED_INT, 0);
        });
        unbindRawModel();
    }
    
    public void prepareRawModel(RawModel model) {
        glBindVertexArray(model.getVaoId());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
    }
    
    public void unbindRawModel() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }
    
    public void prepareInstance(Entity entity) {
        Matrix4f transformationMatrix = MatrixUtils.createTransformationMatrix(
                entity.getPosition(), 
                entity.getRotationX(), 
                entity.getRotationY(), 
                entity.getRotationZ(), 
                entity.getScaleX(), 
                entity.getScaleY(), 
                entity.getScaleZ());
        shader.loadTransformationMatrix(transformationMatrix);
        shader.loadColour(entity.getColour());
    }
}
