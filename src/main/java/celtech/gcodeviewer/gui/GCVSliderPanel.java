/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
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

public class GCVSliderPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_SLIDER_PANEL_OPEN_HEIGHT = 130;
    public static final int GUI_SLIDER_PANEL_ROW_HEIGHT = 30;
    public static final int GUI_SLIDER_PANEL_SLIDER_HEIGHT = 15;
    public static final int GUI_SLIDER_PANEL_ANNOTATION_HEIGHT = 25;
    public static final int GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH = 40;
    public static final int GUI_SLIDER_PANEL_TITLE_WIDTH = 80;
    
    private int positionX = 0;
    private int positionY = 0;
    private int width = 0;
    
    public GCVSliderPanel() {
    }

    public int getPositionX() {
        return positionX;
    }
    
    public int getPositionY() {
        return positionY;
    }

    public int getHeight() {
        return GUI_SLIDER_PANEL_OPEN_HEIGHT;
    }

    public int getWidth() {
        return width;
    }

    public void layout(NkContext ctx, int x, int y, boolean fullWidth, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            positionX = x;
            positionY = y;
            if (fullWidth)
                width = renderParameters.getWindowWidth() - 2 * x;
            else
                width = renderParameters.getWindowWidth() - 3 * x - GUI_GCODE_PANEL_WIDTH;
            nk_rect(x, y, width, GUI_SLIDER_PANEL_OPEN_HEIGHT, rect);
            if (nk_begin(ctx,
                         "Slider Panel",
                         rect,
                         NK_WINDOW_NO_SCROLLBAR)) {
                float w = width - 4.0f * ctx.style().window().padding().x();
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
                                    if (v > renderParameters.getIndexOfTopLayer())
                                        renderParameters.setBottomLayerToRender(renderParameters.getIndexOfTopLayer());
                                    else   
                                        renderParameters.setBottomLayerToRender(v);
                                });
            }
            nk_end(ctx);
        }
    }

    private void layoutPropertyRow(NkContext ctx,
                                   float width,
                                   String label,
                                   int minValue,
                                   int maxValue,
                                   int step,
                                   int incPerPixel,
                                   int currentValue,
                                   Consumer<Integer> setValue) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer valueBuffer = stack.mallocInt(1);
            valueBuffer.put(0, currentValue);
            nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_ROW_HEIGHT, 1);
            nk_layout_row_push(ctx, width);
            if (nk_input_is_key_down(ctx.input(), NK_KEY_CTRL))
                nk_button_set_behavior(ctx, NK_BUTTON_REPEATER);
            if (nk_input_is_key_down(ctx.input(), NK_KEY_SHIFT))
                step = 1;
            nk_property_int(ctx, label, minValue, valueBuffer, maxValue, step, incPerPixel);
            nk_button_set_behavior(ctx, NK_BUTTON_DEFAULT);
            nk_layout_row_end(ctx);
            setValue.accept(valueBuffer.get(0));
        }
    }
    
    private void layoutSliderLabelsRow(NkContext ctx,
                                       float width,
                                       String label,
                                       int minValue,
                                       int maxValue,
                                       int currentValue) {
        nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_ANNOTATION_HEIGHT, 3);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(minValue), NK_TEXT_ALIGN_LEFT);
        nk_layout_row_push(ctx, width - 2.0f * GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, "[" + Integer.toString(currentValue) + "]", NK_TEXT_ALIGN_CENTERED);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_SLIDER_LABEL_WIDTH);
        nk_label(ctx, Integer.toString(maxValue), NK_TEXT_ALIGN_RIGHT);
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
        nk_layout_row_begin(ctx, NK_STATIC, 2 * GUI_SLIDER_PANEL_ROW_HEIGHT, 2);
        nk_layout_row_push(ctx, GUI_SLIDER_PANEL_TITLE_WIDTH);
        nk_label(ctx, label + " ", NK_TEXT_ALIGN_RIGHT | NK_TEXT_ALIGN_MIDDLE);
        nk_layout_row_push(ctx, width - GUI_SLIDER_PANEL_TITLE_WIDTH);
        if (nk_group_begin(ctx, label + "Group", NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR)) {
            float groupWidth = width - GUI_SLIDER_PANEL_TITLE_WIDTH - 6.0f * ctx.style().window().group_padding().x();
            layoutSliderLabelsRow(ctx, groupWidth, label, minValue, maxValue, currentValue);
            try (MemoryStack stack = stackPush()) {
                IntBuffer valueBuffer = stack.mallocInt(1);
                valueBuffer.put(0, currentValue);
                nk_layout_row_begin(ctx, NK_STATIC, GUI_SLIDER_PANEL_SLIDER_HEIGHT, 1);
                nk_layout_row_push(ctx, groupWidth);
                nk_slider_int(ctx, minValue, valueBuffer, maxValue, step);
                nk_layout_row_end(ctx);
                setValue.accept(valueBuffer.get(0));
            }
            nk_group_end(ctx);
        }
    }
}