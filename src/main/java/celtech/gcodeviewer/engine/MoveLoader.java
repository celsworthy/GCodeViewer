package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.engine.renderers.MasterRenderer;
import celtech.gcodeviewer.entities.Entity;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class MoveLoader {
    
    private final List<RawEntity> moveEntities = new ArrayList<>();
    
    public RawEntity loadToVAO(List<Entity> moves) {
        RawEntity moveEntity = createVAO(2 * moves.size());
        moveEntities.add(moveEntity);
        storeSegmentInAttributeList(moveEntity, 0, moves);
        store2xInteger2InAttributeList(moveEntity, 1, moves, (Entity m) -> {
                Integer[] i2 = new Integer[2];
                i2[0] = m.getLayer();
                i2[1] = m.getLineNumber();
                return i2;
            });
        unbindVAO();
        return moveEntity;
    }
    
    public void cleanUp() {
        moveEntities.stream().forEach(moveEntity -> moveEntity.cleanup());
    }
    
    private RawEntity createVAO(int nVertices) {
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        return new RawEntity(vaoId, nVertices);
    }
    
    private void storeSegmentInAttributeList(RawEntity moveEntity, int attributeNumber, List<Entity> moves) {
        int vboId = glGenBuffers();
        moveEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(6 * moves.size());
        moves.forEach(move -> {
                Vector3f p = move.getPosition();
                Vector3f d = move.getDirection();
                d.scale(0.5f * move.getLength());
                float halfLength = 0.5f * move.getLength();
                Vector3f p1 = Vector3f.sub(p, d, null);
                Vector3f p2 = Vector3f.add(p, d, null);
                floatBuffer.put(p1.getX());
                floatBuffer.put(p1.getY());
                floatBuffer.put(p1.getZ());
                floatBuffer.put(p2.getX());
                floatBuffer.put(p2.getY());
                floatBuffer.put(p2.getZ());
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void store2xInteger2InAttributeList(RawEntity moveEntity, int attributeNumber, List<Entity> moves, Function<Entity, Integer[]> getInteger2) {
        int vboId = glGenBuffers();
        moveEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * moves.size());
        moves.forEach(move -> {
                Integer[] i2 = getInteger2.apply(move);
                // Store values twice - one for each vertex in the segment.
                floatBuffer.put((float)i2[0]);
                floatBuffer.put((float)i2[1]);
                floatBuffer.put((float)i2[0]);
                floatBuffer.put((float)i2[1]);
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 2, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        glBindVertexArray(0);
    }
}
