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

public class EntityLoader {
    
    private final List<Integer> vaos = new ArrayList<>();
    private final List<Integer> vbos = new ArrayList<>();
    
    public RawEntity loadToVAO(List<Entity> entities) {
        checkErrors();
        int vaoId = createVAO();
        checkErrors();
        storeVector3InAttributeList(0, entities, Entity::getPosition);
        checkErrors();
        storeVector4InAttributeList(1, entities, (Entity e) -> {
                Vector3f d = e.getDirection();
                Vector4f d4 = new Vector4f(d.getX(), d.getY(), d.getZ(), e.getLength());
                return d4;
            });
        checkErrors();
        storeVector4InAttributeList(2, entities, (Entity e) -> {
                Vector3f n = e.getNormal();
                Vector4f n4 = new Vector4f(n.getX(), n.getY(), n.getZ(), e.getWidth());
                return n4;
            });
        storeVector3InAttributeList(3, entities, Entity::getColour);
        checkErrors();
        storeInteger4InAttributeList(4, entities, (Entity e) -> {
                Integer[] i4 = new Integer[4];
                i4[0] = (e.isMove() ? 1 : 0);
                i4[1] = e.getLayer();
                i4[2] = e.getLineNumber();
                i4[3] = e.getToolNumber();
                return i4;
            });
        checkErrors();
        unbindVAO();
        checkErrors();
        return new RawEntity(vaoId, entities.size());
    }
    
    public void cleanUp() {
        vaos.stream().forEach(vao -> glDeleteVertexArrays(vao));
        vbos.stream().forEach(vbo -> glDeleteBuffers(vbo));
    }
    
    private int createVAO() {
        int vaoId = glGenVertexArrays();
        vaos.add(vaoId);
        glBindVertexArray(vaoId);
        return vaoId;
    }
    
    private void storeVector3InAttributeList(int attributeNumber, List<Entity> entities, Function<Entity, Vector3f> getVector3) {
        int vboId = glGenBuffers();
        vbos.add(vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(3 * entities.size());
        entities.forEach(entity -> {
                Vector3f v = getVector3.apply(entity);
                floatBuffer.put(v.getX());
                floatBuffer.put(v.getY());
                floatBuffer.put(v.getZ());
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void storeVector4InAttributeList(int attributeNumber, List<Entity> entities, Function<Entity, Vector4f> getVector4) {
        int vboId = glGenBuffers();
        vbos.add(vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * entities.size());
        entities.forEach(entity -> {
                Vector4f v = getVector4.apply(entity);
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

    private void storeInteger4InAttributeList(int attributeNumber, List<Entity> entities, Function<Entity, Integer[]> getInteger4) {
        int vboId = glGenBuffers();
        vbos.add(vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(4 * entities.size());
//        IntBuffer intBuffer = BufferUtils.createIntBuffer(4 * entities.size());
        entities.forEach(entity -> {
                Integer[] i4 = getInteger4.apply(entity);                
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

    private void storeIntInAttributeList(int attributeNumber, List<Entity> entities, Function<Entity, Integer> getInteger) {
        int vboId = glGenBuffers();
        vbos.add(vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        IntBuffer integerBuffer = BufferUtils.createIntBuffer(entities.size());
        entities.forEach(entity -> {
                integerBuffer.put(getInteger.apply(entity));
            });
        integerBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, integerBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 1, GL_INT, false, 0, 0);
//        glVertexAttribIPointer(attributeNumber, 1, GL_INT, 4, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        glBindVertexArray(0);
    }
    
    public void checkErrors() {
        int i = glGetError ();
        if (i != GL_NO_ERROR) {
            System.out.println("Entity loader: OpenGL error " + Integer.toString(i));
        }
    }
}
