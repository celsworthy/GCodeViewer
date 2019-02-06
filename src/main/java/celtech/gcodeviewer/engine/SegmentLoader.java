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
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SegmentLoader {
    
    private final List<RawEntity> segmentEntities = new ArrayList<>();
    private double currentLayerHeight = 0.0;
    private double layerThickness = 0.0;
    private int currentLayer = 0;
    private boolean firstSegment = true;

    public RawEntity loadToVAO(List<Entity> segments) {
        RawEntity segmentEntity = createVAO(segments.size());
        segmentEntities.add(segmentEntity);
        storeTrianglesInElementBuffer(segmentEntity, segments);
        storeVerticesInAttributeList(segmentEntity, 0, segments);
        storeNormalsInAttributeList(segmentEntity, 1, segments);
        storeVector3InAttributeList(segmentEntity, 2, segments, Entity::getColour);
        storeVector3InAttributeList(segmentEntity, 3, segments, (Entity s) -> {
				int layerNumber = s.getLayer();
                if (layerNumber == Entity.NULL_LAYER)
                    layerNumber = s.getLineNumber();
                Vector3f v = new Vector3f(layerNumber, s.getLineNumber(), s.getToolNumber());
                return v;
            });
        unbindVAO();
        return segmentEntity;
    }
    
    private RawEntity storeTrianglesInElementBuffer(RawEntity segmentEntity, List<Entity> segments) {
        
        int vboId = glGenBuffers();
        segmentEntity.setIndexVboId(vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        IntBuffer triangleBuffer = BufferUtils.createIntBuffer(48 * segments.size());
        for (int i = 0; i < segments.size(); ++i) {
            int offset = i * 10; // Each segment has ten vertices.
            
            // Add the vertex indices of the sixteen triangles making up the segment block.
            // End pyramid
            triangleBuffer.put(offset + 8); // pp1
            triangleBuffer.put(offset + 0); // p1
            triangleBuffer.put(offset + 1); // p2
            
            triangleBuffer.put(offset + 8); // pp1
            triangleBuffer.put(offset + 1); // p2
            triangleBuffer.put(offset + 2); // p3
            
            triangleBuffer.put(offset + 8); // pp1
            triangleBuffer.put(offset + 2); // p3
            triangleBuffer.put(offset + 3); // p4
            
            triangleBuffer.put(offset + 8); // pp1
            triangleBuffer.put(offset + 3); // p4
            triangleBuffer.put(offset + 0); // p1

            // Side 1 triangles.

            triangleBuffer.put(offset + 0); // p1
            triangleBuffer.put(offset + 4); // p5
            triangleBuffer.put(offset + 5); // p6
            
            triangleBuffer.put(offset + 0); // p1
            triangleBuffer.put(offset + 5); // p6
            triangleBuffer.put(offset + 1); // p2

            // Side 2 triangles.
            triangleBuffer.put(offset + 1); // p2
            triangleBuffer.put(offset + 5); // p6
            triangleBuffer.put(offset + 6); // p7
            
            triangleBuffer.put(offset + 1); // p2
            triangleBuffer.put(offset + 6); // p7
            triangleBuffer.put(offset + 2); // p3

            // Side 3 triangles.
            triangleBuffer.put(offset + 2); // p3
            triangleBuffer.put(offset + 6); // p7
            triangleBuffer.put(offset + 7); // p8
            
            triangleBuffer.put(offset + 2); // p3
            triangleBuffer.put(offset + 7); // p8
            triangleBuffer.put(offset + 3); // p4

            // Side 4 triangles.
            triangleBuffer.put(offset + 3); // p4
            triangleBuffer.put(offset + 7); // p8
            triangleBuffer.put(offset + 4); // p5
            
            triangleBuffer.put(offset + 3); // p4
            triangleBuffer.put(offset + 4); // p5
            triangleBuffer.put(offset + 0); // p1

            // End pyramid
            triangleBuffer.put(offset + 9); // pp2
            triangleBuffer.put(offset + 5); // p6
            triangleBuffer.put(offset + 4); // p5
            
            triangleBuffer.put(offset + 9); // pp2
            triangleBuffer.put(offset + 4); // p5
            triangleBuffer.put(offset + 7); // p8
            
            triangleBuffer.put(offset + 9); // pp2
            triangleBuffer.put(offset + 7); // p8
            triangleBuffer.put(offset + 6); // p7
            
            triangleBuffer.put(offset + 9); // pp2
            triangleBuffer.put(offset + 6); // p7
            triangleBuffer.put(offset + 5); // p6
            offset++;
        }
        triangleBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, triangleBuffer, GL_STATIC_DRAW);
        // Keep element buffer bound so the state is stored in the VAO when it is unbound.
        //glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        return segmentEntity;
    }
        
    private RawEntity storeVerticesInAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments) {
        
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(30 * segments.size());
        segments.forEach(segment -> { generateVertices(segment, vertexBuffer); });
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return segmentEntity;
    }
        
    private void generateVertices(Entity segment, FloatBuffer vertexBuffer) {
        // Each segment is expanded to a rectangular block capped with pyramids.
        // This method generates the ten vertices required.
        Vector3f p = segment.getPosition();
        Vector3f d = segment.getDirection();
        Vector3f n = segment.getNormal();
        Vector3f b = new Vector3f(n).cross(d).normalize(); // bi-normal.
	
        float l = segment.getLength();
        float w = segment.getWidth();
        float t = segment.getThickness();
        // Same vDimension also has the segment length in x and width in y,
        // but also has the thickness in z. 
        Vector3f dd = new Vector3f(d).mul(0.5f * l);
        Vector3f dp = new Vector3f(d).mul(0.5f * w);
        Vector3f nn = new Vector3f(n).mul(0.5f * w);
        Vector3f bb = new Vector3f(b).mul(0.5f * t);
		
        // Calculate the 8 points of the block.
        Vector3f p1 = new Vector3f(p).sub(bb);
        Vector3f p2 = new Vector3f(p).add(nn);
        Vector3f p3 = new Vector3f(p).add(bb);
        Vector3f p4 = new Vector3f(p).sub(nn);
        Vector3f p5 = new Vector3f(p1).add(dd);
        Vector3f p6 = new Vector3f(p2).add(dd);
        Vector3f p7 = new Vector3f(p3).add(dd);
        Vector3f p8 = new Vector3f(p4).add(dd);
        p1.sub(dd);
        p2.sub(dd);
        p3.sub(dd);
        p4.sub(dd);

        // Calculate the centre  points of the end pyramids.
        Vector3f pp1 = new Vector3f(p).sub(dd).sub(dp);
        Vector3f pp2 = new Vector3f(p).add(dd).add(dp);
        
        vertexBuffer.put(p1.x());
        vertexBuffer.put(p1.y());
        vertexBuffer.put(p1.z());

        vertexBuffer.put(p2.x());
        vertexBuffer.put(p2.y());
        vertexBuffer.put(p2.z());

        vertexBuffer.put(p3.x());
        vertexBuffer.put(p3.y());
        vertexBuffer.put(p3.z());

        vertexBuffer.put(p4.x());
        vertexBuffer.put(p4.y());
        vertexBuffer.put(p4.z());

        vertexBuffer.put(p5.x());
        vertexBuffer.put(p5.y());
        vertexBuffer.put(p5.z());

        vertexBuffer.put(p6.x());
        vertexBuffer.put(p6.y());
        vertexBuffer.put(p6.z());

        vertexBuffer.put(p7.x());
        vertexBuffer.put(p7.y());
        vertexBuffer.put(p7.z());

        vertexBuffer.put(p8.x());
        vertexBuffer.put(p8.y());
        vertexBuffer.put(p8.z());

        vertexBuffer.put(pp1.x());
        vertexBuffer.put(pp1.y());
        vertexBuffer.put(pp1.z());

        vertexBuffer.put(pp2.x());
        vertexBuffer.put(pp2.y());
        vertexBuffer.put(pp2.z());
    }
    
    private RawEntity storeNormalsInAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments) {
        // This stores the normals associated with each vertex.
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(30 * segments.size());
        segments.forEach(segment -> {
            Vector3f d = segment.getDirection();
            Vector3f n = segment.getNormal();
            Vector3f b = new Vector3f(n).cross(d).normalize(); // bi-normal.
            
            // p1
            normalBuffer.put(-b.x());
            normalBuffer.put(-b.y());
            normalBuffer.put(-b.z());

            // p2
            normalBuffer.put(n.x());
            normalBuffer.put(n.y());
            normalBuffer.put(n.z());

            // p3
            normalBuffer.put(b.x());
            normalBuffer.put(b.y());
            normalBuffer.put(b.z());

            // p4
            normalBuffer.put(-n.x());
            normalBuffer.put(-n.y());
            normalBuffer.put(-n.z());

            // p5
            normalBuffer.put(-b.x());
            normalBuffer.put(-b.y());
            normalBuffer.put(-b.z());

            // p6
            normalBuffer.put(n.x());
            normalBuffer.put(n.y());
            normalBuffer.put(n.z());

            // p7
            normalBuffer.put(b.x());
            normalBuffer.put(b.y());
            normalBuffer.put(b.z());

            // p8
            normalBuffer.put(-n.x());
            normalBuffer.put(-n.y());
            normalBuffer.put(-n.z());

            // pp1
            normalBuffer.put(-d.x());
            normalBuffer.put(-d.y());
            normalBuffer.put(-d.z());

            // pp2
            normalBuffer.put(d.x());
            normalBuffer.put(d.y());
            normalBuffer.put(d.z());
        });
        normalBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return segmentEntity;
    }
        
    public void reloadColours(RawEntity segmentEntity, List<Entity> segments) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(30 * segments.size());
        segments.forEach(segment -> {
                Vector3f v = segment.getColour();
                for (int i = 0; i < 10; ++i) {
                    floatBuffer.put(v.x());
                    floatBuffer.put(v.y());
                    floatBuffer.put(v.z());
                }
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
    
    private RawEntity createVAO(int nSegments) {
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        return new RawEntity(vaoId, 3 * 16 * nSegments); // Each segment has 16 triangles, each of which has three vertices.
    }
    
    private void storeVector3InAttributeList(RawEntity segmentEntity, int attributeNumber, List<Entity> segments, Function<Entity, Vector3f> getVector3) {
        int vboId = glGenBuffers();
        segmentEntity.setVboId(attributeNumber, vboId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(30 * segments.size());
        segments.forEach(segment -> {
                Vector3f v = getVector3.apply(segment);
                for (int i = 0; i < 10; ++i) {
                    floatBuffer.put(v.x());
                    floatBuffer.put(v.y());
                    floatBuffer.put(v.z());
                }
            });
        floatBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, floatBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, 3, GL_FLOAT, false, 0, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void unbindVAO() {
        glBindVertexArray(0);
    }
}
