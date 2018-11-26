package celtech.gcodeviewer.gcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author George Salter
 */
public class GCodeConvertorTest {
    
    
    @Test
    public void testParseGCode() throws FileNotFoundException, IOException {
        File file = new File("C:/Users/admin/Documents/CEL Robox/PrintJobs/1cd9dfcafd0545e8/1cd9dfcafd0545e8_robox.gcode");
        
    }
    
}
