package celtech.gcodeviewer.utils;

import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author George Salter
 */
public class VectorUtils {
    
    public static Vector3f calculateCenterBetweenVectors(Vector3f from, Vector3f to) {
        Vector3f center = new Vector3f();
        Vector3f.sub(to, from, center);
        center = new Vector3f(center.x / 2, center.y / 2, center.z / 2);
        Vector3f.add(center, from, center);
        return center;
    }
    
    public static float calculateLengthBetweenVectors(Vector3f v1, Vector3f v2) {
        Vector3f positionDiff = new Vector3f();
        Vector3f.sub(v2, v1, positionDiff);
        float length = positionDiff.length();
        return length;
    }
    
    public static float calculateRotationAroundYOfVectors(Vector3f from, Vector3f to) {
        Vector3f positionDiff = new Vector3f();
        Vector3f.sub(to, from, positionDiff);
        if(positionDiff.x == 0 && positionDiff.z == 0) {
            return 0;
        }
        float angle = Vector3f.angle(new Vector3f(0, 0, 1), positionDiff);
        angle = angle - (float) Math.toRadians(90);
        if(from.x > to.x) {
            angle = -angle;
        }
        return angle;
    }
    
    public static float calculateRotationAroundZOfVectors(Vector3f from, Vector3f to) {
        Vector3f positionDiff = new Vector3f();
        Vector3f.sub(to, from, positionDiff);
        if(positionDiff.y == 0) {
            return 0;
        }
        float angle = Vector3f.angle(new Vector3f(0, 1, 0), positionDiff);
        angle = angle - (float) Math.toRadians(90);
        if(from.y > to.y) {
            angle = -angle;
        }
        return angle;
    }
}
