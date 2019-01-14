package celtech.gcodeviewer.engine;

import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony
 */
public class GCodeLoader extends Thread {
    private final static Stenographer STENO = StenographerFactory.getStenographer(GCodeLoader.class.getName());

    GCodeProcessor processor;
    GCodeLineProcessor lineProcessor;
    
    private boolean loadOK = false;
    private boolean loadDone = false;
    private String gCodeFile;

    public GCodeLoader(String gCodeFile, RawModel lineModel, RenderParameters renderParameters, GCodeViewerConfiguration configuration)
    {
        this.setName("GCodeLoader");
        this.gCodeFile = gCodeFile;
        this.processor = new GCodeProcessor();
        this.lineProcessor = new GCodeLineProcessor(lineModel, renderParameters, configuration);
    }

    @Override
    public void run()
    {
        STENO.debug("Starting command queue.");
        loadOK = processor.processFile(gCodeFile, lineProcessor);

        loadDone = true;
    }
    
    public GCodeProcessor getProcessor() {
        return processor;
    }

    public GCodeLineProcessor getLineProcessor() {
        return lineProcessor;
    }

    public boolean loadSuccess() {
        return loadOK;
    }

    public boolean loadFinished() {
        return loadDone;
    }

    public String getFilePath() {
        return gCodeFile;
    }
}
