
package celtech.gcodeviewer.gcode;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author micro
 */
public class GCodeLine {
    public char commandLetter = '!';
    public int commandNumber = -1;
    public int lineNumber = -9999;
    public int layerNumber = -9999;
    public double height = -Double.MAX_VALUE;
    public String type = "";
    public String comment = "";
    public Map<Character, Double> valueMap = new HashMap<>();
    
    public void reset()
    {
        commandLetter = '!';
        commandNumber = -1;
        lineNumber = -9999;
        layerNumber = -9999;
        height = -Double.MAX_VALUE;
        comment = "";
        valueMap.clear();
    }
    
    public void setValue(char c, double v)
    {
        valueMap.put(c, v);
    }

    public boolean isValueSet(char c)
    {
        return valueMap.containsKey(c);
    }

    public double getValue(char c, double defaultValue)
    {
        return valueMap.getOrDefault(c, defaultValue);
    }
}
