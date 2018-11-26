package celtech.gcodeviewer.engine;

public class RawEntity {
    
    private final int vaoId;
    private final int vertexCount;
    
    public RawEntity(int vaoId, int vertexCount) {
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }

    public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }
    
}
