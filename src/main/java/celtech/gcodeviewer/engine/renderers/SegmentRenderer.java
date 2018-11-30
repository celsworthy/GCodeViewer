package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawEntity;
import celtech.gcodeviewer.engine.RenderParameters;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Entity;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.shaders.SegmentShader;
import celtech.gcodeviewer.utils.MatrixUtils;
import java.util.List;
import java.util.Map;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

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
            shader.loadLineLimits(renderParameters.getFirstLineToRender(),
                                  renderParameters.getLastLineToRender());
            shader.loadShowFlags(renderParameters.getShowFlags());
            shader.loadToolColours(renderParameters.getColourForTool(0),
                                   renderParameters.getColourForTool(1));
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
    }
    
    public void unbindRawModel() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
        glBindVertexArray(0);
    }
}
