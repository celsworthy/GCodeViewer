package celtech.gcodeviewer.gcode;

import static celtech.gcodeviewer.gcode.GCodeLine.NULL_NUMBER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import celtech.gcodeviewer.gcode.GCodeLine;
import org.junit.Test;

/**
 *
 * @author Tony Aldhous
 */
public class GCodeLineTest {
    @Test
    public void testGCodeLine() {
        
        GCodeLine line = new GCodeLine();
        line.commandLetter = 'G';
        line.commandNumber = 34;
        line.lineNumber = 1024;
        line.layerNumber = 768;
        line.layerHeight = 42.0;
        line.type = "SKIN";
        line.comment = "The end of days";
        line.setValue('A', 5.2);
        line.setValue('B', 36.2);
        
        line.reset();
        assertEquals(line.commandLetter, '!');
        assertEquals(line.commandNumber, -1);
        assertEquals(line.lineNumber, NULL_NUMBER);
        assertEquals(line.layerNumber, NULL_NUMBER);
        assertEquals(line.layerHeight, -Double.MAX_VALUE, 0.00005);
        assertTrue(line.comment.isEmpty());
        assertTrue(line.valueMap.isEmpty());

        line.setValue('A', 5.2);
        line.setValue('B', 36.2);
        line.setValue('C', -5.7);

        assertTrue(line.isValueSet('A'));
        assertFalse(line.isValueSet('D'));
        assertEquals(line.getValue('A', 0.0), 5.2, 0.0005);
        assertEquals(line.getValue('E', 404.0), 404.0, 0.0005);
    }
    
}
