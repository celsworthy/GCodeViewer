package celuk.gcodeviewer.gui;

import celuk.gcodeviewer.engine.RenderParameters;
import static celuk.gcodeviewer.engine.renderers.GUIRenderer.GUI_GCODE_PANEL_X;
import celuk.language.I18n;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.function.Consumer;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

public class GCVSliderPanel extends GCVPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_SLIDER_PANEL_OPEN_HEIGHT = 100;
    public static final int GUI_SLIDER_PANEL_CLOSED_HEIGHT = 30;
    public static final int GUI_SLIDER_PANEL_ROW_HEIGHT = 30;
    public static final int GUI_SLIDER_PANEL_SLIDER_HEIGHT = 15;
    public static final int GUI_SLIDER_PANEL_ANNOTATION_HEIGHT = 25;
    public static final int GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH = 50;
    public static final int GUI_SLIDER_PANEL_TITLE_WIDTH = 80;
    public static final int GUI_SLIDER_PANEL_SIDE_WIDTH = 10;
    public static final int GUI_SLIDER_PANEL_FIDDLE_FACTOR = 10;
    
    private String topLayerMsg = "sliderPanel.topLayer";
    private String bottomLayerMsg = "sliderPanel.bottomLayer";
    
    public GCVSliderPanel() {
    }

    public void loadMessages() {
        topLayerMsg = I18n.t(topLayerMsg);
        bottomLayerMsg = I18n.t(bottomLayerMsg);
    }

    public void layout(NkContext ctx, int x, int y, boolean fullWidth, float gcodePanelWidth, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            float windowPaddingX = ctx.style().window().padding().x();
            float windowPaddingY = ctx.style().window().padding().y();
            float groupPaddingX = ctx.style().window().group_padding().x();
            float groupPaddingY = ctx.style().window().group_padding().y();

            if (panelExpanded) {
                if (fullWidth)
                    panelWidth = renderParameters.getWindowWidth() - 2.0f * x;
                else
                    panelWidth = renderParameters.getWindowWidth() - x - 2.0f * GUI_GCODE_PANEL_X - gcodePanelWidth;
                panelHeight = GUI_SLIDER_PANEL_OPEN_HEIGHT;
            }
            else {
                panelWidth = GUI_SLIDER_PANEL_SIDE_WIDTH + 4.0f * windowPaddingX;
                panelHeight = GUI_SLIDER_PANEL_CLOSED_HEIGHT;
            }

            panelX = x;
            panelY =  renderParameters.getWindowHeight() - panelHeight - y;

            nk_rect(panelX, panelY, panelWidth, panelHeight, rect);
            if (nk_begin(ctx,
                         "Slider Panel",
                         rect,
                         NK_WINDOW_NO_SCROLLBAR)) {
                if (panelExpanded) {
                    float w = rect.w() - 4.0f * windowPaddingX - 4.0f * groupPaddingX - GUI_SLIDER_PANEL_SIDE_WIDTH;
                    nk_layout_row_begin(ctx, NK_STATIC, rect.h() - 2.0f * windowPaddingY, 2);
                    nk_layout_row_push(ctx, w);
                    if (nk_group_begin(ctx, "SliderGroup", NK_WINDOW_NO_SCROLLBAR)) {
                        layoutSliderTopRow(ctx, w, renderParameters);
                        layoutSliderRow(ctx,
                                        w,
                                        topLayerMsg,
                                        renderParameters.getIndexOfBottomLayer(),
                                        renderParameters.getIndexOfTopLayer(),
                                        1,
                                        renderParameters.getTopLayerToRender(),
                                        (v) -> {
                                            if (v < renderParameters.getBottomLayerToRender())
                                                renderParameters.setTopLayerToRender(renderParameters.getBottomLayerToRender());
                                            else   
                                                renderParameters.setTopLayerToRender(v);
                                        });
                        layoutSliderRow(ctx,
                                        w,
                                        bottomLayerMsg,
                                        renderParameters.getIndexOfBottomLayer(),
                                        renderParameters.getIndexOfTopLayer(),
                                        1,
                                        renderParameters.getBottomLayerToRender(),
                                        (v) -> {
                                            if (v > renderParameters.getTopLayerToRender())
                                                renderParameters.setBottomLayerToRender(renderParameters.getTopLayerToRender());
                                            else   
                                                renderParameters.setBottomLayerToRender(v);
                                        });
                        nk_group_end(ctx);
                    }
                    nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SIDE_WIDTH);
                    layoutSideButton(ctx, renderParameters); 
                    nk_layout_row_end(ctx);
                }
                else {
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_CLOSED_HEIGHT - 2.0f * windowPaddingY, 1);
                    nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SIDE_WIDTH);
                    layoutSideButton(ctx, renderParameters); 
                    nk_layout_row_end(ctx);
                }
            }
            nk_end(ctx);
        }
    }

    private void layoutSliderTopRow(NkContext ctx,
                                    float width,
                                    RenderParameters renderParameters) {
        nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_ANNOTATION_HEIGHT, 4);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_TITLE_WIDTH);
        if(nk_button_label(ctx, "*")) {
            renderParameters.setAllLayersToRender();
        }
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(renderParameters.getIndexOfBottomLayer()), NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, width - 2.0f * GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH - GUI_SLIDER_PANEL_TITLE_WIDTH - 6.0f * ctx.style().window().group_padding().x() - GUI_SLIDER_PANEL_FIDDLE_FACTOR);
        nk_label(ctx, "[" + Integer.toString(renderParameters.getBottomLayerToRender()) + " - " + Integer.toString(renderParameters.getTopLayerToRender()) + "]", NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(renderParameters.getIndexOfTopLayer()), NK_TEXT_ALIGN_RIGHT | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_end(ctx);
    }

    private void layoutSliderRow(NkContext ctx,
                                 float width,
                                 String label,
                                 int minValue,
                                 int maxValue,
                                 int step,
                                 int currentValue,
                                 Consumer<Integer> setValue) {
        nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_ANNOTATION_HEIGHT, 4);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_TITLE_WIDTH);
        nk_label(ctx, label + " ", NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_MIDDLE);
        try (MemoryStack stack = stackPush()) {
            IntBuffer valueBuffer = stack.mallocInt(1);
            valueBuffer.put(0, currentValue);
            float groupWidth = width - GUI_SLIDER_PANEL_TITLE_WIDTH - 6.0f * ctx.style().window().group_padding().x();
            nk_layout_row_push(ctx, groupWidth);
            nk_slider_int(ctx, minValue, valueBuffer, maxValue, step);
            setValue.accept(valueBuffer.get(0));
        }
        nk_layout_row_end(ctx);
    }
}