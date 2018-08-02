package celtech.gcodeviewer.gcode;

import celtech.roboxbase.postprocessor.nouveau.RoboxGCodeParser;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

/**
 * Provides access to the {@link RoboxGCodeParser} and enables conversion of a
 * G-Code file into it's constituent {@link GCodeEventNode}s.
 * 
 * @author George Salter
 */
public class GCodeConvertor {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(GCodeConvertor.class.getName());
    
    /**
     * Convert the G-Code file from the given file path into {@link GCodeEventNode}s.
     * 
     * @param filePath path to the file to be processed
     * @return list of {@link LayerNode}s to be processed
     */
    public List<LayerNode> convertGCode(String filePath) {
        STENO.debug("Beginning parse of G-Code for G-Code viewer.");
        
        List<LayerNode> layerNodes = new ArrayList<>();
        
        try {
            FileReader fileReader = new FileReader(new File(filePath));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuilder layerBuffer = new StringBuilder();

            int layerCounter = -1;
            int lastFeedrate = 0;
            int lastLineNumber = 0;
        
            for (String lineRead = bufferedReader.readLine(); lineRead != null; lineRead = bufferedReader.readLine()) {
                lineRead = lineRead.trim();
                if (lineRead.matches(";LAYER:[-]*[0-9]+(.)*")) {
                    if (layerCounter >= 0 && layerBuffer.length() > 0) {
                        RoboxGCodeParser gcodeParser = Parboiled.createParser(RoboxGCodeParser.class);
                        
                        if (lastLineNumber > 0) {
                            gcodeParser.setCurrentLineNumber(lastLineNumber);
                            gcodeParser.setFeedrateInForce(lastFeedrate);
                        }
                        
                        ReportingParseRunner runner = new ReportingParseRunner<>(gcodeParser.Layer());
                        
                        ParsingResult result = runner.run(layerBuffer.toString());
                        
                        if (result.hasErrors() || !result.matched) {
                            throw new RuntimeException("Parsing failure");
                        } else {
                            LayerNode layerNode = gcodeParser.getLayerNode();
                            lastFeedrate = gcodeParser.getFeedrateInForce();
                            lastLineNumber = gcodeParser.getCurrentLineNumber();
                            layerNodes.add(layerNode);
                        }
                    }
                    
                    layerCounter++;
                    layerBuffer = new StringBuilder();
                    // Make sure this layer command is at the start
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                    
                } else if (!lineRead.equals("")) {
                    //Ignore blank lines
                    // stash it in the buffer
                    layerBuffer.append(lineRead);
                    layerBuffer.append('\n');
                }
            }
        } catch (IOException ex) {
            STENO.error("IO exception when attempting to parse file: " + filePath);
            STENO.error(ex.toString());
        }
        
        return layerNodes;        
    }
    
}
