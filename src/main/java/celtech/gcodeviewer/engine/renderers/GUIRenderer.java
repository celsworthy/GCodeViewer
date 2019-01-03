package celtech.gcodeviewer.engine.renderers;

import celtech.gcodeviewer.engine.RenderParameters;
import celtech.gcodeviewer.entities.Camera;
import celtech.gcodeviewer.gui.GCVControlPanel2;
import celtech.gcodeviewer.gui.GCVGCodePanel2;
import celtech.gcodeviewer.gui.GCVSliderPanel2;
import celtech.gcodeviewer.shaders.GUIShader;
import static org.lwjgl.nuklear.Nuklear.nk_buffer_init_fixed;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.lwjgl.nuklear.NkAllocator;
import org.lwjgl.nuklear.NkBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkConvertConfig;
import org.lwjgl.nuklear.NkDrawCommand;
import org.lwjgl.nuklear.NkDrawVertexLayoutElement;
import static org.lwjgl.nuklear.Nuklear.NK_ANTI_ALIASING_ON;
import static org.lwjgl.nuklear.Nuklear.NK_FORMAT_COUNT;
import static org.lwjgl.nuklear.Nuklear.NK_FORMAT_FLOAT;
import static org.lwjgl.nuklear.Nuklear.NK_FORMAT_R8G8B8A8;
import static org.lwjgl.nuklear.Nuklear.NK_VERTEX_ATTRIBUTE_COUNT;
import static org.lwjgl.nuklear.Nuklear.NK_VERTEX_COLOR;
import static org.lwjgl.nuklear.Nuklear.NK_VERTEX_POSITION;
import static org.lwjgl.nuklear.Nuklear.NK_VERTEX_TEXCOORD;
import static org.lwjgl.nuklear.Nuklear.nk__draw_begin;
import static org.lwjgl.nuklear.Nuklear.nk__draw_next;
import static org.lwjgl.nuklear.Nuklear.nk_buffer_clear;
import static org.lwjgl.nuklear.Nuklear.nk_buffer_free;
import static org.lwjgl.nuklear.Nuklear.nk_buffer_init;
import static org.lwjgl.nuklear.Nuklear.nk_clear;
import static org.lwjgl.nuklear.Nuklear.nk_convert;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryUtil.nmemAllocChecked;
import static org.lwjgl.system.MemoryUtil.nmemFree;

/**
 *
 * @author George Salter
 */
public class GUIRenderer {
    
    public static final int GUI_GCODE_PANEL_X = 5;
    public static final int GUI_GCODE_PANEL_Y = 5;
    public static final int GUI_SLIDER_PANEL_X = 5;
    public static final int GUI_SLIDER_PANEL_Y = 5;
    public static final int GUI_CONTROL_PANEL_X = 5;
    public static final int GUI_CONTROL_PANEL_Y = 5;

    private static final int MAX_VERTEX_BUFFER  = 512 * 1024;
    private static final int MAX_ELEMENT_BUFFER = 128 * 1024;
    private static final int BUFFER_INITIAL_SIZE = 4 * 1024;
    
    private static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;
    
    private static final NkAllocator ALLOCATOR;
    
    static {
        VERTEX_LAYOUT = NkDrawVertexLayoutElement.create(4)
            .position(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0)
            .position(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8)
            .position(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16)
            .position(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0)
            .flip();
        
        ALLOCATOR = NkAllocator.create()
            .alloc((handle, old, size) -> nmemAllocChecked(size))
            .mfree((handle, ptr) -> nmemFree(ptr));
    }
    
    private final NkContext nkContext;
    private final GUIShader guiShader;
    private final RenderParameters renderParameters;
    
    private final NkBuffer cmds = NkBuffer.create();

    private final GCVControlPanel2 controlPanel = new GCVControlPanel2();
    private final GCVSliderPanel2 sliderPanel = new GCVSliderPanel2();
    private final GCVGCodePanel2 gCodePanel = new GCVGCodePanel2();

    public GUIRenderer(NkContext nkContext,
                       GUIShader guiShader,
                       RenderParameters renderParameters) {
        this.nkContext = nkContext;
        this.guiShader = guiShader;
        this.renderParameters = renderParameters;
        this.guiShader.createVAOandVBO();
        nk_buffer_init(cmds, ALLOCATOR, BUFFER_INITIAL_SIZE);
    }
    
    public void loadProjectionMatrix() {
        try (MemoryStack stack = stackPush()) {     
            guiShader.loadTexture();
            guiShader.loadProjectionMatrix(stack.floats(
                2.0f / renderParameters.getWindowWidth(), 0.0f, 0.0f, 0.0f,
                0.0f, -2.0f / renderParameters.getWindowHeight(), 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f, 1.0f
            ));
        }
    }
    
    public void setToolSet(Set<Integer> toolSet) {
        controlPanel.setToolSet(toolSet);
    }

    public void setLines(List<String> lines) {
        gCodePanel.setLines(lines);
    }

    public RenderParameters getRenderParameters() {
        return renderParameters;
    }

    public void render() {
        controlPanel.layout(nkContext, GUI_CONTROL_PANEL_X, GUI_CONTROL_PANEL_Y, renderParameters);
        gCodePanel.layout(nkContext, GUI_GCODE_PANEL_X, GUI_GCODE_PANEL_Y, renderParameters);
        sliderPanel.layout(nkContext, GUI_SLIDER_PANEL_X, GUI_SLIDER_PANEL_Y, !gCodePanel.isPanelExpanded(), renderParameters);
 
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // If face calling is enabled, only the text is drawn.
        glDisable(GL_CULL_FACE);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_SCISSOR_TEST);
        glActiveTexture(GL_TEXTURE0);
        
        // convert from command queue into draw list and draw to screen
        loadProjectionMatrix();
        // allocate vertex and element buffer
        glBindVertexArray(guiShader.getVao());
        glBindBuffer(GL_ARRAY_BUFFER, guiShader.getVbo());
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, guiShader.getEbo());

        glBufferData(GL_ARRAY_BUFFER, MAX_VERTEX_BUFFER, GL_STREAM_DRAW);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, MAX_ELEMENT_BUFFER, GL_STREAM_DRAW);

        // load draw vertices & elements directly into vertex + element buffer
        ByteBuffer vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, MAX_VERTEX_BUFFER, null));
        ByteBuffer elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, MAX_ELEMENT_BUFFER, null));
        
        try (MemoryStack stack = stackPush()) {
            // fill convert configuration
            NkConvertConfig config = NkConvertConfig.callocStack(stack)
                .vertex_layout(VERTEX_LAYOUT)
                .vertex_size(20)
                .vertex_alignment(4)
                .null_texture(guiShader.getNullTexture())
                .circle_segment_count(22)
                .curve_segment_count(22)
                .arc_segment_count(22)
                .global_alpha(1.0f)
                .shape_AA(NK_ANTI_ALIASING_ON)
                .line_AA(NK_ANTI_ALIASING_ON);

            // setup buffers to load vertices and elements
            NkBuffer vbuf = NkBuffer.mallocStack(stack);
            NkBuffer ebuf = NkBuffer.mallocStack(stack);

            nk_buffer_init_fixed(vbuf, vertices/*, max_vertex_buffer*/);
            nk_buffer_init_fixed(ebuf, elements/*, max_element_buffer*/);
            nk_convert(nkContext, cmds, vbuf, ebuf, config);
        }
        glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
        glUnmapBuffer(GL_ARRAY_BUFFER);

        // iterate over and execute each draw command
        float fb_scale_x = (float)renderParameters.getDisplayWidth() / (float)renderParameters.getWindowWidth();
        float fb_scale_y = (float)renderParameters.getDisplayHeight() / (float)renderParameters.getWindowHeight();

        long offset = 0;
        long commandIndex = -1;
        for (NkDrawCommand cmd = nk__draw_begin(nkContext, cmds); cmd != null; cmd = nk__draw_next(cmd, cmds, nkContext)) {
            ++commandIndex;
            if (cmd.elem_count() == 0) {
                continue;
            }
            glBindTexture(GL_TEXTURE_2D, cmd.texture().id());
            glScissor(
                (int)(cmd.clip_rect().x() * fb_scale_x),
                (int)((renderParameters.getWindowHeight() - (int)(cmd.clip_rect().y() + cmd.clip_rect().h())) * fb_scale_y),
                (int)(cmd.clip_rect().w() * fb_scale_x),
                (int)(cmd.clip_rect().h() * fb_scale_y)
            );
            glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
            glBindTexture(GL_TEXTURE_2D, 0);
            offset += cmd.elem_count() * 2;
        }
        nk_clear(nkContext);
        nk_buffer_clear(cmds);

        // default OpenGL state
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glDisable(GL_BLEND);
        glDisable(GL_SCISSOR_TEST);
    }
    
    public void cleanUp() {
        nk_buffer_free(cmds);
    }
    
    public boolean pointOverGuiPanel(int x, int y) {
        return ((x >= GUI_CONTROL_PANEL_X &&
                x <= GUI_CONTROL_PANEL_X + controlPanel.getPanelWidth() &&
                y >= GUI_CONTROL_PANEL_Y &&
                y <= GUI_CONTROL_PANEL_Y + controlPanel.getPanelHeight()) ||
                (x >= renderParameters.getWindowWidth() - GUI_CONTROL_PANEL_X - gCodePanel.getPanelWidth() &&
                x <= renderParameters.getWindowWidth() - GUI_CONTROL_PANEL_X  &&
                y >= GUI_GCODE_PANEL_Y &&
                y <= GUI_GCODE_PANEL_Y + gCodePanel.getPanelHeight()) ||
                (x >= sliderPanel.getPanelX() &&
                x <= sliderPanel.getPanelX() + sliderPanel.getPanelWidth() &&
                y >= sliderPanel.getPanelY() &&
                y <= sliderPanel.getPanelY() + sliderPanel.getPanelHeight()));
    }
}
