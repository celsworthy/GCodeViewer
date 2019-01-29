package celtech.gcodeviewer.engine;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.converters.PathConverter;
import java.io.File;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Command line arguments for GCode Viewer.
 * 
 * @author Tony Aldhous
 */
public class GCodeViewerCommandLineArgs {
    @Parameter(names={"--language-tag", "-l"}, description = "tag to indicate the interface language")
    String languageTag = "en";
    @Parameter(names={"--printer-type", "-p"}, description = "printer type to define the printer volume")
    String printerType = "RBX01";
    @Parameter(names={"--project-directory", "-pd"}, converter = PathConverter.class, description = "directory in which the interface configuration is stored")
    Path projectDirectory = null;
    @Parameter(names={"--always-on-top", "-wt"}, description = "flag to indicate window should always be above other desktop windows")
    boolean windowAlwaysOnTop = false;
    @Parameter(names={"--centered", "-wc"}, description = "flag to indicate window should be centered on screen")
    boolean windowCentered = false;
    @Parameter(names={"--decorated", "-wd"}, description = "flag to indicate window should be decorated with title, close box etc")
    boolean windowDecorated = true;
    @Parameter(names={"--window-height", "-wh"}, description = "Window height on screen")
    double windowHeight = -1.0;
    @Parameter(names={"--normalised-window", "-wn"}, description = "flag to indicate window sizes are normalised")
    boolean windowNormalised = false;
    @Parameter(names={"--resizeable", "-wr"}, description = "flag to indicate window should be resizable")
    boolean windowResizable = true;
    @Parameter(names={"--window-width", "-ww"}, description = "Window width on screen")
    double windowWidth = -1.0;
    @Parameter(names={"--window-x", "-wx"}, description = "Window X position on screen")
    double windowX = -1.0;
    @Parameter(names={"--window-y", "-wy"}, description = "Window Y position on screen")
    double windowY = -1.0;
    @Parameter(converter = FileConverter.class, description = "the gcode file to be displayed")
    List<File> gCodeFile = new ArrayList<File>();

    public GCodeViewerCommandLineArgs() {
        // Default to the system locale.
        Locale appLocale = Locale.getDefault();
        if (appLocale != null)
            languageTag = appLocale.getLanguage();
    }
}
