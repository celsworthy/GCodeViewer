package celtech.gcodeviewer.engine;

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

public class SegmentLoader {
    
    private final List<RawEntity> segmentEntities = new ArrayList<>();

    public RawEntity loadToVAO(List<Entity> segments) {
        RawEntity segmentEntity = createVAO(segments.size());
        segmentEntities.add(segmentEntity);
        storeVector3InAttributeList(segmentEntity, 0, segments, Entity::getPosition);
        storeVector4InAttributeList(segmentEntity, 1, segments, (Entity s) -> {
                Vector3f d = s.getDirection();
                Vector4f d4 = new Vector4f(d.getX(), d.getY(), d.getZ(), s.getLength());
                return d4;
            });
        storeVector4InAttributeList(segmentEntity, 2, segments, (Entity s) -> {
                Vector3f n = s.getNormal();
                Vector4f n4 = new Vector4f(n.getX(), n.getY(), n.getZ(), s.getWidth());
                return n4;
            });
        storeVector3InAttributeList(segmentEntity, 3, segments, Entity::getColour);
        storeInteger4InAttributeList(segmentEntity, 4, segments, (Entity s) -> {
                Integer[] i4 = new Integer[4];
                i4[0] = (s.isMove() ? 1 : 0);
                i4[1] = s.getLayer();
                i4[2] = s.getLineNumber();
                i4[3] = s.getToolNumber();
                return i4;
            });
        unbindVAO();
        return segmentEntity;
    }
    
    public void reloadColours(RawEntity segmentEntity, List<Entity> segments) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(3 * segments.size());
        segments.forEach(segment -> {
                Vector3f v = segment.getColour();
                floatBuffer.put(v.getX());
                floatBuffer.put(v.getY());
                floatBuffer.put(v.getZ());
            });
        floatBuffer.flip();
        glBindVertexArray(segmentEntity.getVaoId());
        int vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        segmentEntity.setVboId(3, vboId);
        glBindVertexArray(0);
    }

    public void cleanUp() {
        segmentEntities.stream().forEach(segment -> segment.cleanup());
    }
    
    private RawEntity createVAO(int nVertices) {
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        return new RawEntity(vaoId, nVertices);
    }
    
    private void storeVector3InAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments, Function<Entity, Vector3f> getVector3) {
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(3 * segments.size());
        segments.forEach(segment -> {
                Vector3f v = getVector3.apply(segment);
                floatBuffer.put(v.getX());
                floatBuffer.put(v.getY());
                floatBuffer.put(v.getZ());
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void storeVector4InAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments, Function<Entity, Vector4f> getVector4) {
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * segments.size());
        segments.forEach(segment -> {
                Vector4f v = getVector4.apply(segment);
                floatBuffer.put(v.getX());
                floatBuffer.put(v.getY());
                floatBuffer.put(v.getZ());
                floatBuffer.put(v.getW());
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 4, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void storeInteger4InAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments, Function<Entity, Integer[]> getInteger4) {
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * segments.size());
//        IntBuffer intBuffer = BufferUtils.createIntBuffer(4 * entities.size());
        segments.forEach(segment -> {
                Integer[] i4 = getInteger4.apply(segment);                
                floatBuffer.put((float)i4[0]);
                floatBuffer.put((float)i4[1]);
                floatBuffer.put((float)i4[2]);
                floatBuffer.put((float)i4[3]);
//                intBuffer.put(i4[0]);
//                intBuffer.put(i4[1]);
//                intBuffer.put(i4[2]);
//                intBuffer.put(i4[3]);
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 4, GL_FLOAT, false, 0, 0);
//        intBuffer.flip();
//        glBufferData(GL_ARRAY_BUFFER, intBuffer, GL_STATIC_DRAW);
//        glVertexAttribIPointer(attributeNumber, 4, GL_INT, 32, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        glBindVertexArray(0);
    }
}
