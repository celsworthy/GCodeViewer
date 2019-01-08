/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.LayerDetails;
import celtech.gcodeviewer.engine.RenderParameters;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.lwjgl.BufferUtils;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.joml.Vector3f;

public class GCVGCodePanel {

    public static final int GUI_GCODE_PANEL_WIDTH = 450;
    public static final int GUI_GCODE_PANEL_SIDE_WIDTH = 10;
    public static final int GUI_GCODE_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_GCODE_PANEL_BUTTON_WIDTH = 35;
    public static final int GUI_GCODE_PANEL_EDIT_WIDTH = 175;
    public static final int GUI_GCODE_PANEL_LINE_HEIGHT = 25;
    public static final int GUI_GCODE_PANEL_CHAR_WIDTH = 14;
    public static final int GUI_GCODE_PANEL_LINE_WIDTH = 80 * GUI_GCODE_PANEL_CHAR_WIDTH;
    public static final int GUI_GCODE_PANEL_CLOSED_HEIGHT = 30;
    public static final int GUI_GCODE_PANEL_VERTICAL_BORDER = 100;
    public static final int GUI_GCODE_PANEL_TOP_HEIGHT = 82;
    public static final int GUI_GCODE_PANEL_SCROLL_PADDING = 10;
    public static final int GUI_GCODE_PANEL_FIDDLE_FACTOR = 10;

    private boolean panelExpanded = false;
    private float panelX = 0.0f;
    private float panelY = 0.0f;

    private float panelWidth = 0.0f;
    private float panelHeight = 0.0f;

    private List<String> lines = null;
    private Map<Integer, LayerDetails> layerMap = null;
    private List<Integer> layerList = null;
    private int maxOffset = 0;
    private int offsetToFirstLayer = 0;

    boolean dragging = false;
    int dragStartLine = 0;

    private int[] hOffset = new int[1];
    private int[] vOffset = new int[1];

    private int firstSelected = 0;
    private int lastSelected = 0;
    private boolean goToLineBlank = true;
    private int goToLine = 0;
    
    private final DecimalFormat format = new DecimalFormat();
    final NkPluginFilter numberFilter;
    
    public GCVGCodePanel() {
        hOffset[0] = 0;
        vOffset[0] = 0;
        numberFilter = NkPluginFilter.create(Nuklear::nnk_filter_float);
    }
    
    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public void setLayerMap(Map<Integer, LayerDetails> layerMap) {
        this.layerMap = layerMap;
        if (layerMap != null) {
            this.layerList = new ArrayList( this.layerMap.keySet());
            Collections.sort(this.layerList);
            calculateLayerOffsets(true);
        }
        else
            layerList = null;
    }
    
    private void calculateLayerOffsets(boolean closeAllLayers) {
        // This can be done by a reduce, but looks better
        // using an old-school for loop because it has the
        // side effect of setting the offsets in the layer
        // details.
        if (layerList == null || layerList.size() == 0) {
            offsetToFirstLayer = lines.size();
            maxOffset = lines.size();
        }
        else
        {
            maxOffset = 0;
            for (int i = 0; i < layerList.size(); ++i) {
                int layer = layerList.get(i);
                LayerDetails details = layerMap.get(layer);
                if (i == 0) {
                    offsetToFirstLayer = details.getStartLine();
                    maxOffset = offsetToFirstLayer;
                }
                details.setStartOffset(maxOffset);
                if (closeAllLayers)
                {
                    details.setLayerOpen(false);
                    details.setMouseWasDown(false);
                }
                if (details.getLayerOpen())
                    maxOffset += details.getNumberOfLines();
                else
                    ++maxOffset;

                details.setEndOffset(maxOffset);
            }
        }
    }

    private int lineToOffset(int line, boolean autoExpand) {
        int offset = line;
        if (line >= offsetToFirstLayer && layerList != null) {
            for (int i = 0; i < layerList.size(); ++i) {
                int layer = layerList.get(i);
                LayerDetails details = layerMap.get(layer);
                if (line >= details.getStartLine() && line < details.getEndLine()) {
                    if (!autoExpand || line == details.getStartLine())
                        offset = details.getStartOffset();
                    else {
                        if (!details.getLayerOpen()) {
                            details.setLayerOpen(true);
                            calculateLayerOffsets(false);
                        }
                        offset = details.getStartOffset() + line - details.getStartLine();
                    }
                    break;
                }
            }
        }
        
        return offset;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }
    
    public int getPanelWidth() {
        return (int)panelWidth;
    }

    public int getPanelHeight() {
        return (int)panelHeight;
    }
    
    public int getPanelX() {
        return (int)panelX;
    }
    
    public int getPanelY() {
        return (int)panelY;
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        // This is a hand-built copy of the nuklear list view, which spoofs a
        // nuklear scrolled group into thinking it contains a much larger list than it
        // actually does. The list view is not used because there is no way to scroll
        // it to a specified point. This automatically scrolls to the selected
        // line when it changes.
        
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            float windowPaddingX = ctx.style().window().padding().x();
            float windowPaddingY = ctx.style().window().padding().y();
            float groupPaddingX = ctx.style().window().group_padding().x();
            float groupPaddingY = ctx.style().window().group_padding().y();
            
            if (panelExpanded) {
                panelWidth = GUI_GCODE_PANEL_WIDTH;
                panelHeight = renderParameters.getWindowHeight() - 2 * y;
                if (panelHeight < GUI_GCODE_PANEL_CLOSED_HEIGHT)
                    panelHeight = GUI_GCODE_PANEL_CLOSED_HEIGHT;
            }
            else {
                panelWidth = GUI_GCODE_PANEL_SIDE_WIDTH + 4.0f * windowPaddingX;
                panelHeight = GUI_GCODE_PANEL_CLOSED_HEIGHT;
            }
            panelX = renderParameters.getWindowWidth() - panelWidth - x;
            panelY = y;
            nk_rect(panelX, panelY, panelWidth, panelHeight, rect);
                
            if (nk_begin(ctx, "GCode Panel", rect, NK_WINDOW_NO_SCROLLBAR)) {
                if (panelExpanded) {
                    float w = rect.w() - 4.0f * windowPaddingX - GUI_GCODE_PANEL_SIDE_WIDTH;
                    nk_layout_row_begin(ctx, NK_STATIC, rect.h() - 2.0f * windowPaddingY, 2);
                    nk_layout_row_push(ctx, GUI_GCODE_PANEL_SIDE_WIDTH);
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                    }
                    nk_layout_row_push(ctx, w);
                    if (nk_group_begin(ctx, "GCodeGroup", NK_WINDOW_NO_SCROLLBAR)) {
                        w -= 4.0f * groupPaddingX + GUI_GCODE_PANEL_FIDDLE_FACTOR;
                        layoutLayers(ctx, w, renderParameters);
                        nk_group_end(ctx);
                    }
                }
                else {
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_CLOSED_HEIGHT - 2.0f * windowPaddingY, 1);
                    nk_layout_row_push(ctx, GUI_GCODE_PANEL_SIDE_WIDTH);
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                    }
                }
            }
            nk_end(ctx);
        }
    }
    private void layoutTopRow(NkContext ctx, float width, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_ROW_HEIGHT, 3);
            int step = 1;
            if (renderParameters.getNumberOfLines() < 1000)
                step = 10;
            else if (renderParameters.getNumberOfLines() < 10000)
                step = 50;
            else if (renderParameters.getNumberOfLines() < 100000)
                step = 100;
            else if (renderParameters.getNumberOfLines() < 1000000)
                step = 500;
            else
                step = 1000;
            float pWidth = (width - GUI_GCODE_PANEL_BUTTON_WIDTH - 2.0f * ctx.style().window().group_padding().x()) / 2;
            layoutProperty(ctx,
                           "First",
                           pWidth,
                           0,
                           renderParameters.getLastSelectedLine(),
                           1,
                           step,
                           renderParameters.getFirstSelectedLine(),
                           renderParameters::setFirstSelectedLine);
            layoutProperty(ctx,
                           "Last",
                           pWidth,
                           renderParameters.getFirstSelectedLine(),
                           renderParameters.getNumberOfLines(),
                           1,
                           step,
                           renderParameters.getLastSelectedLine(),
                           renderParameters::setLastSelectedLine);

            nk_layout_row_push(ctx, GUI_GCODE_PANEL_BUTTON_WIDTH);
            if(nk_button_label(ctx, "*")) {
                renderParameters.setFirstSelectedLine(0);
                renderParameters.setLastSelectedLine(0);
            }
            nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_ROW_HEIGHT, 5);
            nk_layout_row_push(ctx, GUI_GCODE_PANEL_EDIT_WIDTH);
            ByteBuffer editBuffer = stack.calloc(256);
            int length = memASCII((goToLineBlank ? "" : format.format(goToLine)), false, editBuffer);
            IntBuffer len = stack.ints(length);
            nk_edit_string(ctx, NK_EDIT_SIMPLE, editBuffer, len, 255, numberFilter);
            try {
                goToLineBlank = (len.get(0) == 0);
                if (goToLineBlank)
                    goToLine = 0;
                else {
                    int l = format.parse(memASCII(editBuffer, len.get(0))).intValue();
                    if (l >= 0 && l < renderParameters.getNumberOfLines())
                        goToLine = l;
                }
            } catch (ParseException e) {
                //e.printStackTrace();
            }
            float bWidth = (width - GUI_GCODE_PANEL_EDIT_WIDTH - 5.0f * ctx.style().window().group_padding().x()) / 3.0f;
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "G") && !goToLineBlank && lines != null) {
                vOffset[0] = lineToOffset(goToLine, true) * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "F") &&
               renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                vOffset[0] = lineToOffset(renderParameters.getFirstSelectedLine(), true) * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "L") &&
               renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                vOffset[0] = lineToOffset(renderParameters.getLastSelectedLine() - 1, true) * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
        }
    }
    
    private void layoutProperty(NkContext ctx,
                                String label,
                                float width,
                                int minValue,
                                int maxValue,
                                int step,
                                int incPerPixel,
                                int currentValue,
                                Consumer<Integer> setValue) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer valueBuffer = stack.mallocInt(1);
            valueBuffer.put(0, currentValue);
            nk_layout_row_push(ctx, width);
            if (nk_input_is_key_down(ctx.input(), NK_KEY_CTRL))
                nk_button_set_behavior(ctx, NK_BUTTON_REPEATER);
            if (nk_input_is_key_down(ctx.input(), NK_KEY_SHIFT))
                step = 1;
            nk_property_int(ctx, label, minValue, valueBuffer, maxValue, step, incPerPixel);
            nk_button_set_behavior(ctx, NK_BUTTON_DEFAULT);
            setValue.accept(valueBuffer.get(0));
        }
    }

    private void layoutLayers(NkContext ctx, float width, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            layoutTopRow(ctx, width, renderParameters);
            if (lines != null && layerList != null)
            {
                // Assume all the offsets in the layers have been calculated.
                float viewHeight = (panelHeight - GUI_GCODE_PANEL_TOP_HEIGHT);
                int viewBegin = (int)(vOffset[0] / (float)GUI_GCODE_PANEL_LINE_HEIGHT);
                if (viewBegin < 0)
                    viewBegin = 0;
                int viewCount = (int)((viewHeight - GUI_GCODE_PANEL_SCROLL_PADDING) / (float)GUI_GCODE_PANEL_LINE_HEIGHT);
                if (viewCount < 0)
                    viewCount = 0;
                if (viewCount > maxOffset - viewBegin)
                    viewCount = maxOffset - viewBegin;

                int remainingHeight = (maxOffset - viewCount) * GUI_GCODE_PANEL_LINE_HEIGHT;

                // Clamp the offset.
                if ( vOffset[0] < 0)
                     vOffset[0] = 0;
                if (vOffset[0] > remainingHeight)
                    vOffset[0] = remainingHeight;

                nk_layout_row_dynamic(ctx, viewHeight, 1);
                NkColor textColour = NkColor.mallocStack(stack);
                textColour.set((byte)255, (byte)255, (byte)255, (byte)255);
                NkColor selectedTextColour = NkColor.mallocStack(stack);
                Vector3f selectedColour = renderParameters.getSelectColour();
                selectedTextColour.set((byte)(255 * selectedColour.x()), (byte)(255 * selectedColour.y()), (byte)(255 * selectedColour.z()), (byte)255);

                NkColor originalTextColour = ctx.style().text().color();
                int scrollShowingButtons = ctx.style().scrollv().show_buttons();
                ctx.style().scrollv().show_buttons(1);
                int previousVOffset = vOffset[0];
                vOffset[0] = 0;
                if (nk_group_scrolled_offset_begin(ctx, hOffset, vOffset, "GCode", 0)) {
                    
                    // Find the layer in which the view begins.
                    LayerDetails currentDetails = null;
                    int currentLayerIndex = 0;
                    if (viewBegin >= offsetToFirstLayer)
                    {
                        for (int i = 0; currentDetails == null && i < layerList.size(); ++i) {
                             LayerDetails details = layerMap.get(layerList.get(i));
                             if (viewBegin >= details.getStartOffset() &&
                                 viewBegin < details.getEndOffset()) {
                                 currentDetails = details;
                                 currentLayerIndex = i;
                             }
                        }
                    }
                    
                    int labelSpace = ((int)Math.floor(Math.log10(previousVOffset + viewCount - 1)) + 6) * GUI_GCODE_PANEL_CHAR_WIDTH;
                    for (int i = 0; i < viewCount; ++i) {
                        int index = viewBegin + i;
                        if (index == offsetToFirstLayer &&
                            currentDetails == null &&
                            layerList != null &&
                            layerList.size() > 0) {
                            currentLayerIndex = 0;
                            currentDetails = layerMap.get(layerList.get(currentLayerIndex));
                        }
                        else {
                            if (index >= offsetToFirstLayer && index >= currentDetails.getEndOffset()) {
                                ++currentLayerIndex;
                                if (currentLayerIndex >= layerList.size())
                                    break;
                                currentDetails = layerMap.get(layerList.get(currentLayerIndex));
                            }
                        }
                        boolean mouseIsDown = false;
                        int lineIndex = 0;
                        nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_LINE_HEIGHT, 2);
                        if (currentDetails != null && index == currentDetails.getStartOffset())
                        {
                            // Draw a tree entry
                            nk_layout_row_push(ctx, labelSpace);
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                if (!currentDetails.getMouseWasDown()) {
                                    currentDetails.setLayerOpen(!currentDetails.getLayerOpen());
                                    calculateLayerOffsets(false);
                                    currentDetails.setMouseWasDown(true);
                                }
                            }
                            else
                                currentDetails.setMouseWasDown(false);
                            ctx.style().text().color(textColour);
                            lineIndex = currentDetails.getStartLine();
                            if (lineIndex >= renderParameters.getFirstSelectedLine() && lineIndex < renderParameters.getLastSelectedLine())
                                ctx.style().text().color(selectedTextColour);
                            else
                                ctx.style().text().color(textColour);
                            String prefix;
                            if (currentDetails.getLayerOpen())
                                prefix = "V";
                            else
                                prefix = ">";
                            nk_label(ctx, prefix + " " + Integer.toString(lineIndex) + ": ", NK_TEXT_LEFT);
                            nk_layout_row_push(ctx, GUI_GCODE_PANEL_LINE_WIDTH - labelSpace);
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                //System.out.println("Mouse click inside line label");
                                mouseIsDown = true;
                            }
                            nk_label(ctx, lines.get(lineIndex), NK_TEXT_LEFT);
                            nk_layout_row_end(ctx);
                        }
                        else
                        {
                            // Draw a line
                            if (index < offsetToFirstLayer)
                                lineIndex = index;
                            else
                                lineIndex = currentDetails.getStartLine() + index - currentDetails.getStartOffset();
                            nk_layout_row_push(ctx, labelSpace);
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                //System.out.println("Mouse click inside line number label");
                                mouseIsDown = true;
                            }
                            if (lineIndex >= renderParameters.getFirstSelectedLine() && lineIndex < renderParameters.getLastSelectedLine())
                                ctx.style().text().color(selectedTextColour);
                            else
                                ctx.style().text().color(textColour);
                            nk_label(ctx, "  " + Integer.toString(lineIndex) + ":", NK_TEXT_LEFT);
                            nk_layout_row_push(ctx, GUI_GCODE_PANEL_LINE_WIDTH - labelSpace);
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                //System.out.println("Mouse click inside line label");
                                mouseIsDown = true;
                            }
                            nk_label(ctx, lines.get(lineIndex), NK_TEXT_LEFT);
                            nk_layout_row_end(ctx);
                        }

                        if (mouseIsDown) {
                            if (nk_input_is_key_down(ctx.input(), NK_KEY_SHIFT) &&
                                renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                                // Extend existing selection.
                                if (lineIndex >= offsetToFirstLayer && lineIndex == currentDetails.getStartLine() && !currentDetails.getLayerOpen()) {
                                    // Extend over the whole layer.
                                    if (lineIndex < renderParameters.getFirstSelectedLine())
                                        renderParameters.setFirstSelectedLine(lineIndex);
                                    else if (currentDetails.getEndLine() > renderParameters.getLastSelectedLine())
                                        renderParameters.setLastSelectedLine(currentDetails.getEndLine());
                                }
                                else
                                {
                                    if (lineIndex < renderParameters.getFirstSelectedLine())
                                        renderParameters.setFirstSelectedLine(lineIndex);
                                    else if (lineIndex >= renderParameters.getLastSelectedLine())
                                        renderParameters.setLastSelectedLine(lineIndex + 1);
                                }
                            }
                            else {
                                if (lineIndex >= offsetToFirstLayer && lineIndex == currentDetails.getStartLine() && !currentDetails.getLayerOpen()) {
                                    // Set selection to cover the whole of the layer.
                                    renderParameters.setFirstSelectedLine(currentDetails.getStartLine());
                                    renderParameters.setLastSelectedLine(currentDetails.getEndLine());                                        
                                }
                                else {
                                    // Set selection to line index.
                                    renderParameters.setFirstSelectedLine(lineIndex);
                                    renderParameters.setLastSelectedLine(lineIndex + 1);
                                }
                            }
                        }
                    }
                    vOffset[0] = vOffset[0] + previousVOffset;
                    nk_layout_row_dynamic(ctx, remainingHeight, 1);
                    nk_group_end(ctx);
                }
                ctx.style().text().color(originalTextColour);
                ctx.style().scrollv().show_buttons(scrollShowingButtons);
            }
        }
    }
}