/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
import static celtech.gcodeviewer.engine.renderers.GUIRenderer.GUI_GCODE_PANEL_X;
import static celtech.gcodeviewer.engine.renderers.GUIRenderer.GUI_SLIDER_PANEL_Y;
import static celtech.gcodeviewer.gui.GCVGCodePanel.GUI_GCODE_PANEL_WIDTH;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.lwjgl.BufferUtils;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GCVSliderPanel2 {

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
    
    
    private float panelX = 0.0f;
    private float panelY = 0.0f;
    private float panelWidth = 0.0f;
    private float panelHeight = 0.0f;
    private boolean panelExpanded = false;
    
    public GCVSliderPanel2() {
    }

    public int getPanelX() {
        return (int)panelX;
    }
    
    public int getPanelY() {
        return (int)panelY;
    }

    public int getPanelHeight() {
        return (int)panelHeight;
    }

    public int getPanelWidth() {
        return (int)panelWidth;
    }

    public void layout(NkContext ctx, int x, int y, boolean fullWidth, RenderParameters renderParameters) {
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
                    panelWidth = renderParameters.getWindowWidth() - x - 2.0f * GUI_GCODE_PANEL_X - GUI_GCODE_PANEL_WIDTH;
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
                        layoutSliderLabelsRow(ctx,
                                              w,
                                              renderParameters.getIndexOfBottomLayer(),
                                              renderParameters.getIndexOfTopLayer(),
                                              renderParameters.getBottomLayerToRender(),
                                              renderParameters.getTopLayerToRender());
                        layoutSliderRow(ctx,
                                        w,
                                        "Top",
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
                                        "Bottom",
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
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                    }
                }
                else {
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_CLOSED_HEIGHT - 2.0f * windowPaddingY, 1);
                    nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SIDE_WIDTH);
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                    }
                }
            }
            nk_end(ctx);
        }
    }

    private void layoutSliderLabelsRow(NkContext ctx,
                                       float width,
                                       int minValue,
                                       int maxValue,
                                       int currentMinValue,
                                       int currentMaxValue) {
        nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_ANNOTATION_HEIGHT, 4);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_TITLE_WIDTH);
        nk_label(ctx, "Layer", NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(minValue), NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, width - 2.0f * GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH - GUI_SLIDER_PANEL_TITLE_WIDTH - 6.0f * ctx.style().window().group_padding().x() - GUI_SLIDER_PANEL_FIDDLE_FACTOR);
        nk_label(ctx, "[" + Integer.toString(currentMinValue) + " - " + Integer.toString(currentMaxValue) + "]", NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(maxValue), NK_TEXT_ALIGN_RIGHT | NK_TEXT_ALIGN_MIDDLE);
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
    }
}