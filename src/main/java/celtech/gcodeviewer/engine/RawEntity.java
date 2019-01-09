package celtech.gcodeviewer.engine;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;

public class RawEntity {
    public static final int N_VBO_ATTRIBUTES = 8;
    private final int vaoId;
    private final int vboAttributes[] = new int[N_VBO_ATTRIBUTES];
    private int indexVboId = 0;
    private final int elementCount;
    
    public RawEntity(int vaoId, int elementCount) {
        this.vaoId = vaoId;
        this.elementCount = elementCount;
        for (int attributeNumber = 0; attributeNumber < N_VBO_ATTRIBUTES; ++attributeNumber)
            vboAttributes[attributeNumber] = 0;
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVboId(int attributeNumber) {
        if (attributeNumber >= 0 && attributeNumber < N_VBO_ATTRIBUTES)
            return vboAttributes[attributeNumber];
        else
           return 0;
    }

    public void setVboId(int attributeNumber, int vboId) {
        if (attributeNumber >= 0 && attributeNumber < N_VBO_ATTRIBUTES)
        {
            if (vboAttributes[attributeNumber] != 0 && vboAttributes[attributeNumber] != vboId)
                glDeleteBuffers(vboAttributes[attributeNumber]);
            vboAttributes[attributeNumber] = vboId;
        }
    }

    public int getIndexVboId() {        
           return indexVboId;
    }

    public void setIndexVboId(int vboId) {
        indexVboId = vboId;
    }

    public int getElementCount() {
        return elementCount;
    }
    
    public void cleanup() {
        glDeleteVertexArrays(vaoId);
        for (int attributeNumber = 0; attributeNumber < N_VBO_ATTRIBUTES; ++attributeNumber) {
            if (vboAttributes[attributeNumber] != 0)
            {
                glDeleteBuffers(vboAttributes[attributeNumber]);
                vboAttributes[attributeNumber] = 0;
            }
        }
        if (indexVboId != 0)
        {
            glDeleteBuffers(indexVboId);
            indexVboId = 0;
        }
    }
}
