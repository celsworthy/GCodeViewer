package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RawEntity;
import celtech.gcodeviewer.engine.RenderParameters;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.entities.Light;
import celtech.gcodeviewer.shaders.MoveShader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class MoveRenderer {
   
    private final MoveShader shader;
    private Matrix4f projectionMatrix;
    
    public MoveRenderer(MoveShader shader, Matrix4f projectionMatrix) {
        this.shader = shader;
        this.projectionMatrix = projectionMatrix;
    }
    
    public void render(RawEntity rawEntity,
                       Camera camera,
                       Light light,
                       RenderParameters renderParameters) {
        if (rawEntity != null) {
            MasterRenderer.checkErrors();
            shader.start();
            MasterRenderer.checkErrors();
            shader.setProjectionMatrix(projectionMatrix);
            MasterRenderer.checkErrors();
            shader.setViewMatrix(camera);
            MasterRenderer.checkErrors();
            shader.loadCompositeMatrix();
            MasterRenderer.checkErrors();
            shader.loadMoveColour(renderParameters.getMoveColour());
            MasterRenderer.checkErrors();
            shader.loadLayerLimits(renderParameters.getTopLayerToRender(),
                                   renderParameters.getBottomLayerToRender());
            MasterRenderer.checkErrors();
            shader.loadLineLimits(renderParameters.getFirstLineToRender(),
                                  renderParameters.getLastLineToRender());
            MasterRenderer.checkErrors();
            bindRawModel(rawEntity);
            MasterRenderer.checkErrors();
            glDrawArrays(GL_LINES, 0, rawEntity.getVertexCount());
            MasterRenderer.checkErrors();
            unbindRawModel();
            MasterRenderer.checkErrors();
            shader.stop();
            MasterRenderer.checkErrors();
        }
    }
    
    public void setProjectionMatrix(Matrix4f projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }
    
    public void bindRawModel(RawEntity rawEntity) {
        glBindVertexArray(rawEntity.getVaoId());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
    }
    
    public void unbindRawModel() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
    }
}
