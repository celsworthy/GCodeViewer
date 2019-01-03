package celtech.gcodeviewer.gcode;

import celtech.gcodeviewer.entities.Entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import celtech.gcodeviewer.gcode.GCodeLine;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Tony Aldhous
 */
public class GCodeLineParserTest {
    @Test
    public void testGCodeLineParser() {
        GCodeLineParser gCodeParser = Parboiled.createParser(GCodeLineParser.class);
        ReportingParseRunner runner = new ReportingParseRunner<>(gCodeParser.Line());
        ParsingResult result = null;
        GCodeLine line = null;

        String testLine = "";
        gCodeParser.resetLine();
        testLine = ";Generated with Cura_SteamEngine Robox 1.5";
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, GCodeLine.NULL_NUMBER, -Double.MAX_VALUE, "", "Generated with Cura_SteamEngine Robox 1.5");

        testLine = "=Alternative comment";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, GCodeLine.NULL_NUMBER, -Double.MAX_VALUE, "", "Alternative comment");

        testLine = ";Layer count: 30";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, GCodeLine.NULL_NUMBER, -Double.MAX_VALUE, "", "Layer count: 30");

        testLine = ";LAYER:0";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, 0, -Double.MAX_VALUE, "", "");

        testLine = ";LAYER:1 height:0.500";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, 1, 0.5, "", "");

        testLine = ";TYPE:WALL-INNER";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, '!', -1, GCodeLine.NULL_NUMBER, GCodeLine.NULL_NUMBER, -Double.MAX_VALUE, "WALL-INNER", "");

        testLine = "G0 F12000 X47.246 Y72.381 Z0.300";
        gCodeParser.resetLine();
        result = runner.run(testLine);
        line = gCodeParser.getLine();
        assertFalse(result.hasErrors() || !result.matched);
        compareLine(line, 'G', 0, GCodeLine.NULL_NUMBER, GCodeLine.NULL_NUMBER, -Double.MAX_VALUE, "WALL-INNER", "");
        assertEquals(12000.0, line.getValue('F', 0.0), 0.0005);
        assertEquals(47.246, line.getValue('X', 0.0), 0.0005);
        assertEquals(72.381, line.getValue('Y', 0.0), 0.0005);
        assertEquals(0.3, line.getValue('Z', 0.0), 0.0005);
    }
    
    private void compareLine(GCodeLine line,
                             char commandLetter,
                             int commandNumber,
                             int lineNumber,
                             int layerNumber,
                             double layerHeight,
                             String type,
                             String comment) {
        assertEquals(commandLetter, line.commandLetter);
        assertEquals(commandNumber, line.commandNumber);
        assertEquals(lineNumber, line.lineNumber);
        assertEquals(layerNumber, line.layerNumber);
        assertEquals(layerHeight, line.layerHeight, 0.00005);
        assertEquals(comment, line.comment);
        assertEquals(type, line.type);
    }
}
