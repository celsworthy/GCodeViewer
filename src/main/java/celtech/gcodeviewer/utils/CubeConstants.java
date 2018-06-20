package celtech.gcodeviewer.utils;

/**
 *  Simply holds the constants needed to create a basic cube model.
 * 
 * @author George Salter
 */
public class CubeConstants {
    
    public static final float[] VERTICES = {			
        -0.5f,0.5f,-0.5f,	
        -0.5f,-0.5f,-0.5f,	
        0.5f,-0.5f,-0.5f,	
        0.5f,0.5f,-0.5f,		

        -0.5f,0.5f,0.5f,	
        -0.5f,-0.5f,0.5f,	
        0.5f,-0.5f,0.5f,	
        0.5f,0.5f,0.5f,

        0.5f,0.5f,-0.5f,	
        0.5f,-0.5f,-0.5f,	
        0.5f,-0.5f,0.5f,	
        0.5f,0.5f,0.5f,

        -0.5f,0.5f,-0.5f,	
        -0.5f,-0.5f,-0.5f,	
        -0.5f,-0.5f,0.5f,	
        -0.5f,0.5f,0.5f,

        -0.5f,0.5f,0.5f,
        -0.5f,0.5f,-0.5f,
        0.5f,0.5f,-0.5f,
        0.5f,0.5f,0.5f,

        -0.5f,-0.5f,0.5f,
        -0.5f,-0.5f,-0.5f,
        0.5f,-0.5f,-0.5f,
        0.5f,-0.5f,0.5f
    };
    
    public static final int[] INDICES = {
        3,2,0,	
        0,2,1,	
        4,5,7,
        7,5,6,
        8,11,10,
        8,10,9,
        15,12,13,
        15,13,14,	
        17,16,19,
        17,19,18,
        20,21,23,
        23,21,22
    };
    
    public static final float[] NORMALS = {
        0f,0f,-1f,	
        0f,0f,-1f,	
        0f,0f,-1f,
        0f,0f,-1f,

        0f,0f,1f,	
        0f,0f,1f,	
        0f,0f,1f,
        0f,0f,1f,

        1f,0f,0f,	
        1f,0f,0f,	
        1f,0f,0f,	
        1f,0f,0f,

        -1f,0f,0f,	
        -1f,0f,0f,	
        -1f,0f,0f,	
        -1f,0f,0f,

        0f,1f,0f,
        0f,1f,0f,
        0f,1f,0f,
        0f,1f,0f,

        0f,-1f,0f,
        0f,-1f,0f,
        0f,-1f,0f,
        0f,-1f,0f
    };
}
