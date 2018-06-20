package celtech.gcodeviewer.utils;

import celtech.gcodeviewer.entities.Camera;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class MatrixUtils {
    
    public static Matrix4f createTransformationMatrix(Vector3f translation, 
            float rx, float ry, float rz, float scale) {
        return createTransformationMatrix(translation, rx, ry, rz, scale, scale, scale);
    }
    
    public static Matrix4f createTransformationMatrix(Vector3f translation, 
            float rx, float ry, float rz,
            float sx, float sy, float sz) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        Matrix4f.translate(translation, matrix, matrix);
        Matrix4f.rotate((float) rx, new Vector3f(1, 0, 0), matrix, matrix);
        Matrix4f.rotate((float) ry, new Vector3f(0, 1, 0), matrix, matrix);
        Matrix4f.rotate((float) rz, new Vector3f(0, 0, 1), matrix, matrix);
        Matrix4f.scale(new Vector3f(sx, sy, sz), matrix, matrix);
        return matrix;
    }
    
    public static Matrix4f createTransformationMatrixForLine(Vector3f translation, 
            float rx, float ry, float rz, float length) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        Matrix4f.translate(translation, matrix, matrix);
        Matrix4f.rotate((float) rx, new Vector3f(1, 0, 0), matrix, matrix);
        Matrix4f.rotate((float) ry, new Vector3f(0, 1, 0), matrix, matrix);
        Matrix4f.rotate((float) rz, new Vector3f(0, 0, 1), matrix, matrix);
        Matrix4f.scale(new Vector3f(length, 1, 1), matrix, matrix);
        return matrix;
    }
    
    public static Matrix4f createViewMatrix(Camera camera) {
        Matrix4f viewMatrix = new Matrix4f();
        viewMatrix.setIdentity();
        
        Matrix4f.rotate((float) Math.toRadians(camera.getPitch()), new Vector3f(1,0,0), viewMatrix, viewMatrix);
        Matrix4f.rotate((float) Math.toRadians(camera.getYaw()), new Vector3f(0,1,0), viewMatrix, viewMatrix);
        
        Vector3f cameraPos = camera.getPosition();
        Vector3f negativeCameraPos = new Vector3f(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);
        
        return viewMatrix;
    }
}
