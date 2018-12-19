/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

public class GCVControlPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_CONTROL_PANEL_WIDTH = 260;
    public static final int GUI_CONTROL_PANEL_OPEN_HEIGHT = 200;
    public static final int GUI_CONTROL_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT = 40;
    public static final int GUI_CONTROL_PANEL_CLOSED_HEIGHT = 30;

    private boolean panelOpen = false;
    List<Integer>  toolList = new ArrayList<>();
    
    public GCVControlPanel() {
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }
    
    public int getWidth() {
        return GUI_CONTROL_PANEL_WIDTH;
    }

    public int getHeight() {
        return (panelOpen ? GUI_CONTROL_PANEL_OPEN_HEIGHT + toolList.size() * GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT
                          : GUI_CONTROL_PANEL_CLOSED_HEIGHT);
    }

    public void setToolSet(Set<Integer> toolSet) {
        toolList = toolSet.stream()
                         .filter(ts -> (ts >= 0 && ts < 8))
                         .collect(Collectors.toList());
        Collections.sort(toolList);
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            panelOpen = nk_begin(ctx,
                         "Control Panel",
                         nk_rect(x, y, GUI_CONTROL_PANEL_WIDTH, GUI_CONTROL_PANEL_OPEN_HEIGHT + toolList.size() * GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT, rect),
                         NK_WINDOW_MINIMIZABLE | NK_WINDOW_NO_SCROLLBAR);
            if (panelOpen) {
                float w = rect.w() - 4.0f * ctx.style().window().padding().x();
                nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                nk_layout_row_push(ctx, w);
                if(nk_button_label(ctx, "Show all layers")) {
                    renderParameters.setTopLayerToRender(renderParameters.getIndexOfTopLayer());
                    renderParameters.setBottomLayerToRender(renderParameters.getIndexOfBottomLayer());
                }
                layoutCheckboxRow(ctx,
                                  w,
                                  "Colour As Type",
                                  (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE),
                                  (f) -> { renderParameters.setColourMode(f ? RenderParameters.ColourMode.COLOUR_AS_TYPE 
                                                                            : RenderParameters.ColourMode.COLOUR_AS_TOOL); });
                layoutCheckboxRow(ctx,
                                  w,
                                  "Show Moves",
                                  renderParameters.getShowMoves(),
                                  renderParameters::setShowMoves);

                layoutCheckboxRow(ctx,
                                  w,
                                  "Show Only Selected",
                                  renderParameters.getShowOnlySelected(),
                                  renderParameters::setShowOnlySelected);
                
                IntBuffer toolBuffer = stack.mallocInt(1);
                toolList.forEach(t -> {
                    String label = "Show Tool " + Integer.toString(t);
                    boolean currentValue = renderParameters.getShowFlagForTool(t);
                    toolBuffer.put(0, (currentValue ? 1 : 0));
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                    nk_layout_row_push(ctx, w);
                    nk_checkbox_label(ctx, label, toolBuffer);
                    nk_layout_row_end(ctx);
                    renderParameters.setShowFlagForTool(t, (toolBuffer.get(0) != 0));
                });
            }
            nk_end(ctx);
        }
    }

    private void layoutCheckboxRow(NkContext ctx,
                                   float width,
                                   String label,
                                   boolean currentValue,
                                   Consumer<Boolean> setSelected) {

        try (MemoryStack stack = stackPush()) {
            IntBuffer valueBuffer = stack.mallocInt(1);
            valueBuffer.put(0, currentValue ? 1 : 0);
            nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
            nk_layout_row_push(ctx, width);
            nk_checkbox_label(ctx, label, valueBuffer);
            nk_layout_row_end(ctx);
            boolean newValue = (valueBuffer.get(0) != 0);
            setSelected.accept(newValue);
        }
    }
}