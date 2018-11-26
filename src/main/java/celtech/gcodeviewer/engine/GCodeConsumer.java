/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodeviewer.engine;

import celtech.gcodeviewer.gcode.GCodeLine;

/**
 *
 * @author Tony
 */
public interface GCodeConsumer {
    public void reset();
    public void processLine(GCodeLine line);
}
