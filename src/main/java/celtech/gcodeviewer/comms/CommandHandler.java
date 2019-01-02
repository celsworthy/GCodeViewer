/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.gcodeviewer.comms;

import celtech.gcodeviewer.engine.RenderParameters;
import celtech.gcodeviewer.engine.RenderingEngine;
import celtech.gcodeviewer.entities.Entity;
import java.util.Scanner;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.joml.Vector3f;

/**
 *
 * @author Tony
 */
public class CommandHandler {
    private final static int MAX_COMMAND_COUNT = 10;
    private final static Stenographer STENO = StenographerFactory.getStenographer(CommandHandler.class.getName());
    private final CommandQueue commandQueue;
    private RenderParameters renderParameters;
    private RenderingEngine renderingEngine;

    public CommandHandler() {
        this.commandQueue = new CommandQueue();
        this.renderParameters = null;
        this.renderingEngine = null;
    }
    
    public void start() {
        // Command handler is not a thread because most of it's actions are fast and affect the rendering engine directly,
        // which can only be changed safely on the main thread. Any actions that do take a long time (e.g. loading a GCode file)
        // should be done on a separate thread
        STENO.debug("Starting command handler.");
        commandQueue.start(); 
    }
     
    public void stop() {
        commandQueue.stopRunning();
    }
    
    public void setRenderParameters(RenderParameters renderParameters) {
       this.renderParameters = renderParameters; 
    }

    public void setRenderingEngine(RenderingEngine renderingEngine) {
       this.renderingEngine = renderingEngine; 
    }

    public boolean processCommands() {
        for (int commandCount = 0; commandCount < MAX_COMMAND_COUNT && commandQueue.commandAvailable(); ++commandCount) {
            String command = commandQueue.getNextCommandFromQueue().trim();
            STENO.debug("Processing command " + command);
            if (command.equalsIgnoreCase("q") || command.equalsIgnoreCase("quit")) {
                return true;
            }
            else {
                try {
                    String commandParameter;
                    Scanner commandScanner = new Scanner(command);
                    if (commandScanner.hasNext()) {
                        String commandWord = commandScanner.next().toLowerCase();
                        switch (commandWord) {
                            case "load":
                                renderingEngine.startLoadingGCodeFile(commandScanner.nextLine().trim());
                                break;

                            case "l1":
                                renderingEngine.startLoadingGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\snake.gcode");
                                break;

                            case "l2":
                                renderingEngine.startLoadingGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\gear-box.gcode");
                                break;

                            case "l3":
                                renderingEngine.startLoadingGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\spiral-65-0p5.gcode");
                                break;

                            case "l4":
                                renderingEngine.startLoadingGCodeFile("D:\\CEL\\Dev\\GCodeViewer\\cones_robox.gcode");
                                break;

                            case "l5":
                                renderingEngine.startLoadingGCodeFile("D:\\CEL\\Dev\\ImpactWire\\step\\test_part_spline_combined.gcode");
                                break;

                            case "l6":
                                renderingEngine.startLoadingGCodeFile("D:\\Documents\\Cel Robox\\Projects\\rbx_twisted_cone\\Draft\\Draft_robox.gcode");
                                break;
                                
                            case "show":
                            case "s":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                    case "m":
                                        renderParameters.setShowMoves(true);
                                        break;

                                    case "tool":
                                    case "t":
                                        if (commandScanner.hasNextInt())
                                            renderParameters.setShowFlagForTool(commandScanner.nextInt(), true);
                                        else
                                           STENO.error("Unrecognised option in command " + command);
                                        break;

                                    default:
                                        STENO.error("Unrecognised option in command " + command);
                                }
                                break;

                            case "hide":
                            case "h":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "moves":
                                    case "m":
                                        renderParameters.setShowMoves(false);
                                        break;

                                    case "tool":
                                    case "t":
                                        if (commandScanner.hasNextInt())
                                            renderParameters.setShowFlagForTool(commandScanner.nextInt(), false);
                                        else
                                           STENO.error("Unrecognised option in command " + command);
                                        break;

                                    default:
                                        STENO.error("Unrecognised option in command " + command);
                                }
                                break;

                            case "top":
                            case "t":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setTopLayerToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setTopLayerToRender(renderParameters.getIndexOfTopLayer());
                                break;

                            case "bottom":
                            case "b":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setBottomLayerToRender(commandScanner.nextInt());
                                else
                                    renderParameters.setBottomLayerToRender(renderParameters.getIndexOfBottomLayer());
                                break;


                            case "first":
                            case "f":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setFirstSelectedLine(commandScanner.nextInt());
                                else
                                    renderParameters.setFirstSelectedLine(0);
                                break;

                            case "last":
                            case "l":
                                if (commandScanner.hasNextInt())
                                    renderParameters.setLastSelectedLine(commandScanner.nextInt());
                                else
                                    renderParameters.setLastSelectedLine(0);
                                break;

                            case "colour":
                            case "co":
                                commandParameter = commandScanner.next().toLowerCase();
                                switch (commandParameter) {
                                    case "type":
                                    case "ty":
                                        renderingEngine.colourSegmentsFromType();
                                        renderingEngine.reloadSegments();
                                        renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TYPE);
                                        break;

                                    case "tool":
                                    case "to":
                                        processColourToolSubCommand(command, commandScanner);
                                        break;

                                    case "data":
                                    case "d":
                                        int dataIndex = -1;
                                        if (commandScanner.hasNextInt()) {
                                            dataIndex = commandScanner.nextInt();
                                            if (dataIndex < 0 || dataIndex >= Entity.N_DATA_VALUES)
                                                dataIndex = -1;
                                        }
                                        else {
                                            commandParameter = commandScanner.next().toLowerCase();
                                            switch (commandParameter)
                                            {
                                                case "a":
                                                    dataIndex = 0;
                                                    break;
                                                case "b":
                                                    dataIndex = 1;
                                                    break;
                                                case "d":
                                                    dataIndex = 2;
                                                    break;
                                                case "e":
                                                    dataIndex = 3;
                                                    break;
                                                case "f":
                                                    dataIndex = 4;
                                                    break;
                                                case "x":
                                                    dataIndex = 5;
                                                    break;
                                                case "y":
                                                    dataIndex = 6;
                                                    break;
                                                case "z":
                                                    dataIndex = 7;
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }
                                        if (dataIndex >= 0)
                                        {
                                            renderingEngine.colourSegmentsFromData(dataIndex);
                                            renderingEngine.reloadSegmentColours();
                                            renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TYPE);
                                        }
                                        break;
                                
                                    case "extrusion":
                                    case "e":
                                        if (commandScanner.hasNextInt()) {
                                            int toolIndex = commandScanner.nextInt();
                                            if (commandScanner.hasNextFloat()) {
                                                float extrusionFactor = commandScanner.nextFloat();
                                                if (toolIndex >= 0 &&
                                                    toolIndex < renderParameters.getToolColours().size() &&
                                                    extrusionFactor > 0.0f &&  extrusionFactor < 10.0f) {
                                                }
                                            }
                                        }
                                        break;

                                    default:
                                        System.out.println("Unrecognised colour mode in command " + command);
                                }
                                break;

                            case "clear":
                            case "cl":
                                renderingEngine.clearGCode();
                                break;
                                
                            default:
                                STENO.error("Ignoring command " + command);
                                break;
                        }
                    }
                }
                catch (RuntimeException ex) {
                    STENO.exception("Command parsing error", ex);
                }
            }
        }
        
        return false;
    }
    
    private void processColourToolSubCommand(String command, Scanner commandScanner) {
        if (commandScanner.hasNextInt()) {
            int toolIndex = commandScanner.nextInt();
            float r = -1.0f;
            float g = -1.0f;
            float b = -1.0f;
            if (commandScanner.hasNextFloat()) {
                r = commandScanner.nextFloat();
            }
            if (commandScanner.hasNextFloat()) {
                g = commandScanner.nextFloat();
            }
            if (commandScanner.hasNextFloat()) {
                b = commandScanner.nextFloat();
            }
            if (!commandScanner.hasNext() &&
                toolIndex >= 0 && toolIndex < renderParameters.getToolColours().size() &&
                r >= 0.0f && r <= 1.0f &&
                g >= 0.0f && g <= 1.0f &&
                b >= 0.0f && b <= 1.0f)
            {
                renderParameters.setColourForTool(toolIndex, new Vector3f(r, g, b));
            }
            else {
                System.out.println("Unrecognised option in colour command " + command);
            }
        }
        else if (!commandScanner.hasNext()) {
            renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TOOL);
        }
        else {
            System.out.println("Unrecognised option in command " + command);
        }
        
    }

   
}
