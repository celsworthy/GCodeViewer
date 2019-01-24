package celtech.gcodeviewer.entities;

import celtech.gcodeviewer.engine.ModelLoader;
import celtech.gcodeviewer.engine.RawModel;

/**
 * 
 * @author George Salter
 */
public class Floor {
    
    private static final int VERTEX_COUNT = 40;
    
    private final float sizeX;
    private final float sizeZ;
    
    private final float xPos;
    private final float zPos;
    private final RawModel model;
    
    public Floor(float sizeX, float sizeZ, float offsetX, float offsetZ, ModelLoader modelLoader) {
        this.xPos = offsetX - sizeX;
        this.zPos = offsetZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.model = generateFloor(modelLoader);
    }

    public float getxPos() {
        return xPos;
    }

    public float getzPos() {
        return zPos;
    }

    public RawModel getModel() {
        return model;
    }
    
    private RawModel generateFloor(ModelLoader modelLoader) {
        int count = VERTEX_COUNT * VERTEX_COUNT;
        float[] vertices = new float[count * 3];
        float[] normals = new float[count * 3];
        //float[] textureCoords = new float[count*2];
        int[] indices = new int[6 * (VERTEX_COUNT-1) * (VERTEX_COUNT - 1)];
        int vertexPointer = 0;
        for(int i = 0; i<VERTEX_COUNT; i++){
            for(int j = 0; j < VERTEX_COUNT; j++){
                vertices[vertexPointer * 3] = (float) j / ((float) VERTEX_COUNT - 1) * sizeX;
                vertices[vertexPointer * 3 + 1] = 0;
                vertices[vertexPointer * 3 + 2] = (float) i / ((float) VERTEX_COUNT - 1) * sizeZ;
                normals[vertexPointer * 3] = 0;
                normals[vertexPointer * 3 + 1] = 1;
                normals[vertexPointer * 3 + 2] = 0;
                //textureCoords[vertexPointer*2] = (float)j/((float)VERTEX_COUNT - 1);
                //textureCoords[vertexPointer*2+1] = (float)i/((float)VERTEX_COUNT - 1);
                vertexPointer++;
            }
        }
        int pointer = 0;
        for(int gz = 0; gz < VERTEX_COUNT - 1; gz++){
            for(int gx = 0; gx < VERTEX_COUNT - 1; gx++){
                int topLeft = (gz * VERTEX_COUNT) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }
        return modelLoader.loadToVAO(vertices, normals, indices);
    }
}
