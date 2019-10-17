package celuk.gcodeviewer.engine.renderers;

import celuk.gcodeviewer.engine.RawEntity;
import celuk.gcodeviewer.engine.RenderParameters;
import celuk.gcodeviewer.entities.Camera;
import celuk.gcodeviewer.entities.Entity;
import celuk.gcodeviewer.entities.Light;
import celuk.gcodeviewer.shaders.SegmentShader;
import celuk.gcodeviewer.utils.MatrixUtils;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SegmentRenderer {
   
    private final SegmentShader shader;
    private Matrix4f projectionMatrix;
    
    public SegmentRenderer(SegmentShader shader, Matrix4f projectionMatrix) {
        this.shader = shader;
        this.projectionMatrix = projectionMatrix;
    }
    
    public void render(RawEntity rawEntity,
                       Camera camera,
                       Light light,
                       RenderParameters renderParameters) {
        if (rawEntity != null) {
            shader.start();
            shader.setProjectionMatrix(projectionMatrix);
            shader.setViewMatrix(camera);
            shader.loadCompositeMatrix();
            shader.loadLight(light);
            shader.loadLayerLimits(renderParameters.getTopLayerToRender(),
                                   renderParameters.getBottomLayerToRender());
            shader.loadLineLimits(renderParameters.getFirstSelectedLine(),
                                  renderParameters.getLastSelectedLine());
            shader.loadShowFlags(renderParameters.getShowFlags());
            shader.loadShowTools(renderParameters.getShowTools());
            shader.loadShowTypes(renderParameters.getShowTypes());
            shader.loadToolColours(renderParameters.getToolColours());
            shader.loadTypeColours(renderParameters.getTypeColours());
            shader.loadSelectColour(renderParameters.getSelectColour());
            bindRawModel(rawEntity);
            glDrawArrays(GL_POINTS, 0, rawEntity.getVertexCount());
            unbindRawModel();
            shader.stop();
        }
    }
    
    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }
    
    public void bindRawModel(RawEntity rawEntity) {
        glBindVertexArray(rawEntity.getVaoId());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);
        glEnableVertexAttribArray(5);
        glEnableVertexAttribArray(6);
    }
    
    public void unbindRawModel() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
        glDisableVertexAttribArray(5);
        glDisableVertexAttribArray(6);
        glBindVertexArray(0);
    }
}
