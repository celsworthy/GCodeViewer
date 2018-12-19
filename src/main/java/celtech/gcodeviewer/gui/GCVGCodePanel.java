/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
import static celtech.gcodeviewer.gui.GCVControlPanel.GUI_CONTROL_PANEL_ROW_HEIGHT;
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
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.util.vector.Vector3f;

public class GCVGCodePanel {

    public static final int GUI_GCODE_PANEL_WIDTH = 450;
    public static final int GUI_GCODE_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_GCODE_PANEL_BUTTON_WIDTH = 35;
    public static final int GUI_GCODE_PANEL_LINE_HEIGHT = 25;
    public static final int GUI_GCODE_PANEL_CHAR_WIDTH = 14;
    public static final int GUI_GCODE_PANEL_LINE_WIDTH = 80 * GUI_GCODE_PANEL_CHAR_WIDTH;
    public static final int GUI_GCODE_PANEL_CLOSED_HEIGHT = 30;
    public static final int GUI_GCODE_PANEL_VERTICAL_BORDER = 100;
    public static final int GUI_GCODE_PANEL_TOP_HEIGHT = 135;
    public static final int GUI_GCODE_PANEL_SCROLL_PADDING = 10;

    private boolean panelOpen = false;
    private List<String> lines = null;
    int panelHeight = 0;
    
    boolean mouseWasDown = false;
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

    public boolean isPanelOpen() {
        return panelOpen;
    }
    
    public int getWidth() {
        return GUI_GCODE_PANEL_WIDTH;
    }

    public int getHeight() {
        return (panelOpen ? panelHeight
                          : GUI_GCODE_PANEL_CLOSED_HEIGHT);
    }
    
    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        // This is a hand-built copy of the nuklear list view, which spoofs a
        // nuklear scrolled group into thinking it contains a much larger list than it
        // actually does. The list view is not used because there is no way to scroll
        // it to a specified point. This automatically scrolls to the selected
        // line when it changes.
        
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            panelHeight = renderParameters.getWindowHeight() - GUI_GCODE_PANEL_VERTICAL_BORDER;
            if (panelHeight < GUI_GCODE_PANEL_CLOSED_HEIGHT)
                panelHeight = GUI_GCODE_PANEL_CLOSED_HEIGHT;
                
            panelOpen = nk_begin(ctx,
                         "GCode Panel",
                         nk_rect(x, y, GUI_GCODE_PANEL_WIDTH, panelHeight, rect),
                         NK_WINDOW_MINIMIZABLE | NK_WINDOW_NO_SCROLLBAR);
            if (panelOpen)
            {
                layoutTopRow(ctx, renderParameters);
                if (lines != null)
                {
                    int viewHeight = panelHeight - GUI_GCODE_PANEL_TOP_HEIGHT;
                    int viewBegin = (int)(vOffset[0] / (float)GUI_GCODE_PANEL_LINE_HEIGHT);
                    if (viewBegin < 0)
                        viewBegin = 0;
                    int viewCount = (int)((viewHeight - GUI_GCODE_PANEL_SCROLL_PADDING) / (float)GUI_GCODE_PANEL_LINE_HEIGHT);
                    if (viewCount < 0)
                        viewCount = 0;
                    if (viewCount > lines.size() - viewBegin)
                        viewCount = lines.size() - viewBegin;
                    
                    // Clamp the offset.
                    if ( vOffset[0] < 0)
                         vOffset[0] = 0;
                    int maxOffset = (lines.size() - viewCount) * GUI_GCODE_PANEL_LINE_HEIGHT;
                    if (vOffset[0] > maxOffset)
                        vOffset[0] = maxOffset;

                    nk_layout_row_dynamic(ctx, viewHeight, 1);
                    NkColor textColour = NkColor.mallocStack(stack);
                    textColour.set((byte)255, (byte)255, (byte)255, (byte)255);
                    NkColor selectedTextColour = NkColor.mallocStack(stack);
                    Vector3f selectedColour = renderParameters.getSelectColour();
                    selectedTextColour.set((byte)(255 * selectedColour.getX()), (byte)(255 * selectedColour.getY()), (byte)(255 * selectedColour.getZ()), (byte)255);

                    NkColor originalTextColour = ctx.style().text().color();
                    int scrollShowingButtons = ctx.style().scrollv().show_buttons();
                    ctx.style().scrollv().show_buttons(1);
                    int previousVOffset = vOffset[0];
                    vOffset[0] = 0;
                    if (nk_group_scrolled_offset_begin(ctx, hOffset, vOffset, "GCode", 0)) {
                        int remainingHeight = GUI_GCODE_PANEL_LINE_HEIGHT * (lines.size() - viewCount);

                        int labelSpace = ((int)Math.floor(Math.log10(previousVOffset + viewCount - 1)) + 1) * GUI_GCODE_PANEL_CHAR_WIDTH;
                        for (int i = 0; i < viewCount; ++i) {
                            nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_LINE_HEIGHT, 2);
                            int index = viewBegin + i;
                            nk_layout_row_push(ctx, labelSpace);
                            boolean mouseIsDown = false;
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                //System.out.println("Mouse click inside line number label");
                                mouseIsDown = true;
                            }
                            if (index >= renderParameters.getFirstSelectedLine() && index < renderParameters.getLastSelectedLine())
                                ctx.style().text().color(selectedTextColour);
                            else
                                ctx.style().text().color(textColour);
                            nk_label(ctx, Integer.toString(index) + ":", NK_TEXT_LEFT);
                            nk_layout_row_push(ctx, GUI_GCODE_PANEL_LINE_WIDTH - labelSpace);
                            if (nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
                                //System.out.println("Mouse click inside line label");
                                mouseIsDown = true;
                            }
                            nk_label(ctx, lines.get(index), NK_TEXT_LEFT);
                            nk_layout_row_end(ctx);

                            if (mouseIsDown) {
                                if (!mouseWasDown) {
                                    if (nk_input_is_key_down(ctx.input(), NK_KEY_SHIFT) &&
                                        renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                                        // Extend existing selection.
                                        if (index < renderParameters.getFirstSelectedLine())
                                            renderParameters.setFirstSelectedLine(index);
                                        else if (index >= renderParameters.getLastSelectedLine())
                                            renderParameters.setLastSelectedLine(index + 1);
                                    }
                                    else {
                                        // Set selection to index.
                                        renderParameters.setFirstSelectedLine(index);
                                        renderParameters.setLastSelectedLine(index + 1);
                                    }
                                }
                                mouseWasDown = true;
                            }
                            else {
                                mouseWasDown = false;
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
            nk_end(ctx);
        }
    }
    private void layoutTopRow(NkContext ctx, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_ROW_HEIGHT, 3);
            int step = 1000;
            if (renderParameters.getNumberOfLines() < 1000)
                step = 10;
            else if (renderParameters.getNumberOfLines() < 10000)
                step = 100;
            else if (renderParameters.getNumberOfLines() < 100000)
                step = 5000;
            else
                step = 1000;
            int pWidth = (GUI_GCODE_PANEL_WIDTH - GUI_GCODE_PANEL_BUTTON_WIDTH - 20) / 2;
            layoutProperty(ctx,
                           "First",
                           pWidth,
                           0,
                           renderParameters.getLastSelectedLine(),
                           step,
                           step,
                           renderParameters.getFirstSelectedLine(),
                           renderParameters::setFirstSelectedLine);
            layoutProperty(ctx,
                           "Last",
                           pWidth,
                           renderParameters.getFirstSelectedLine(),
                           renderParameters.getNumberOfLines(),
                           step,
                           step,
                           renderParameters.getLastSelectedLine(),
                           renderParameters::setLastSelectedLine);

            nk_layout_row_push(ctx, GUI_GCODE_PANEL_BUTTON_WIDTH);
            if(nk_button_label(ctx, "*")) {
                renderParameters.setFirstSelectedLine(0);
                renderParameters.setLastSelectedLine(0);
            }
            nk_layout_row_begin(ctx, NK_STATIC, GUI_GCODE_PANEL_ROW_HEIGHT, 5);
            nk_layout_row_push(ctx, 175);
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
            int bWidth = (GUI_GCODE_PANEL_WIDTH - 200) / 3;
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "G") && !goToLineBlank && lines != null) {
                vOffset[0] = goToLine * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "F") &&
               renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                vOffset[0] = renderParameters.getFirstSelectedLine() * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
            nk_layout_row_push(ctx, bWidth);
            if(nk_button_label(ctx, "L") &&
               renderParameters.getFirstSelectedLine() != renderParameters.getLastSelectedLine()) {
                vOffset[0] = (renderParameters.getLastSelectedLine() - 1) * GUI_GCODE_PANEL_LINE_HEIGHT;
            }
        }
    }
    
    private void layoutProperty(NkContext ctx,
                                String label,
                                int width,
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
}