/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
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

public class GCVControlPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_PANEL_X = 10;
    public static final int GUI_PANEL_WIDTH = 230;
    public static final int GUI_PANEL_Y = 10;
    public static final int GUI_PANEL_OPEN_HEIGHT = 331;
    public static final int GUI_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_TOOL_ROW_HEIGHT = 40;
    public static final int GUI_PANEL_CLOSED_HEIGHT = 30;

    private final IntBuffer topValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int topValue = 0;
    private final IntBuffer bottomValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int bottomValue = 0;
    private final IntBuffer firstValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int firstValue = 0;
    private final IntBuffer lastValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int lastValue = 0;
    private final IntBuffer showMovesBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private boolean showMovesValue = false;
    private final IntBuffer showOnlySelectedBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private boolean showOnlySelectedValue = false;
    private final IntBuffer showTool0Buffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private boolean showTool0Value = false;
    private final IntBuffer showTool1Buffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private boolean showTool1Value = false;
    private final IntBuffer colourAsTypeBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private boolean colourAsTypeValue = false;

    private final Map<Integer, IntBuffer> toolBuffers = new HashMap<>();
    private final Map<Integer, Boolean> toolValues = new HashMap<>();
    
    private boolean panelOpen = false;
    List<Integer>  toolList = new ArrayList<>();
    
    public GCVControlPanel() {
    }

    public boolean isPanelOpen() {
        return panelOpen;
    }
    
    public int getPanelHeight() {
        return (panelOpen ? GUI_PANEL_OPEN_HEIGHT + toolList.size() * GUI_TOOL_ROW_HEIGHT
                          : GUI_PANEL_CLOSED_HEIGHT);
    }
    
    public void setToolSet(Set<Integer> toolSet) {
        toolBuffers.clear();
        toolValues.clear();
        toolList = toolSet.stream()
                         .filter(ts -> (ts >= 0 && ts < 8))
                         .collect(Collectors.toList());
        Collections.sort(toolList);
       
       toolList.forEach(t -> {
            toolBuffers.put(t, BufferUtils.createIntBuffer(1).put(0, 0));
            toolValues.put(t, false);
       });
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            panelOpen = nk_begin(ctx,
                         "Control Panel",
                         nk_rect(x, y, GUI_PANEL_WIDTH, GUI_PANEL_OPEN_HEIGHT + toolList.size() * GUI_TOOL_ROW_HEIGHT, rect),
                         NK_WINDOW_MINIMIZABLE);
            if (panelOpen) {
                topValue = layoutPropertyRow(ctx, 
                                             "Top",
                                             renderParameters.getBottomLayerToRender(),
                                             topValueBuffer,
                                             renderParameters.getIndexOfTopLayer(),
                                             5,
                                             1,
                                             renderParameters.getTopLayerToRender(),
                                             topValue,
                                             renderParameters::setTopLayerToRender);
                bottomValue = layoutPropertyRow(ctx, 
                                                "Bottom",
                                                renderParameters.getIndexOfBottomLayer(),
                                                bottomValueBuffer,
                                                renderParameters.getTopLayerToRender(),
                                                5,
                                                1,
                                                renderParameters.getBottomLayerToRender(),
                                                bottomValue,
                                                renderParameters::setBottomLayerToRender);
                int step = 1000;
                if (renderParameters.getNumberOfLines() < 1000)
                    step = 10;
                else if (renderParameters.getNumberOfLines() < 10000)
                    step = 100;
                else if (renderParameters.getNumberOfLines() < 100000)
                    step = 5000;
                else
                    step = 1000;
                lastValue = layoutPropertyRow(ctx,
                                              "Last",
                                              renderParameters.getFirstSelectedLine(),
                                              lastValueBuffer,
                                              renderParameters.getNumberOfLines(),
                                              step,
                                              step,
                                              renderParameters.getLastSelectedLine(),
                                              lastValue,
                                              renderParameters::setLastSelectedLine);
                firstValue = layoutPropertyRow(ctx,
                                               "First",
                                               0,
                                               firstValueBuffer,
                                               renderParameters.getLastSelectedLine(),
                                               step,
                                               step,
                                               renderParameters.getFirstSelectedLine(),
                                               firstValue,
                                               renderParameters::setFirstSelectedLine);

                colourAsTypeValue = layoutCheckboxRow(ctx,
                                                   "Colour As Type",
                                                   colourAsTypeBuffer,
                                                   (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE),
                                                   colourAsTypeValue,
                                                   (f) -> { renderParameters.setColourMode(f ? RenderParameters.ColourMode.COLOUR_AS_TYPE 
                                                                                             : RenderParameters.ColourMode.COLOUR_AS_TOOL); });
                showMovesValue = layoutCheckboxRow(ctx,
                                                   "Show Moves",
                                                   showMovesBuffer,
                                                   renderParameters.getShowMoves(),
                                                   showMovesValue,
                                                   renderParameters::setShowMoves);

                showOnlySelectedValue = layoutCheckboxRow(ctx,
                                                   "Show Only Selected",
                                                   showOnlySelectedBuffer,
                                                   renderParameters.getShowOnlySelected(),
                                                   showOnlySelectedValue,
                                                   renderParameters::setShowOnlySelected);
                
                toolList.forEach(t -> {
                    String label = "Show Tool " + Integer.toString(t);
                    boolean previousValue = toolValues.get(t);
                    boolean currentValue = renderParameters.getShowFlagForTool(t);
                    IntBuffer toolBuffer = toolBuffers.get(t);
                    if (previousValue != currentValue) {
                        toolBuffer.put(0, (currentValue ? 1 : 0));
                    }
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_PANEL_ROW_HEIGHT, 1);
                    nk_layout_row_push(ctx, 200);
                    nk_checkbox_label(ctx, label, toolBuffer);
                    nk_layout_row_end(ctx);
                    boolean newValue = (toolBuffer.get(0) != 0);
                    renderParameters.setShowFlagForTool(t, newValue);
                    toolValues.put(t, newValue);
                });
//                showTool0Value = layoutCheckboxRow(ctx,
//                       "Show Tool 0",
//                       showTool0Buffer,
//                       renderParameters.getShowFlagForTool(0),
//                       showTool0Value,
//                       (f) -> { renderParameters.setShowFlagForTool(0, f); });
//                showTool1Value = layoutCheckboxRow(ctx,
//                                                   "Show Tool 1",
//                                                   showTool1Buffer,
//                                                   renderParameters.getShowFlagForTool(1),
//                                                   showTool1Value,
//                                                   (f) -> { renderParameters.setShowFlagForTool(1, f); });
            }
            nk_end(ctx);
        }
    }
    
    private int layoutPropertyRow(NkContext ctx,
                                  String label,
                                  int minValue,
                                  IntBuffer valueBuffer,
                                  int maxValue,
                                  int step,
                                  int incPerPixel,
                                  int currentValue,
                                  int previousValue,
                                  Consumer<Integer> setValue) {
        if (previousValue != currentValue) {
            valueBuffer.put(0, currentValue);
        }
        nk_layout_row_begin(ctx, NK_STATIC, GUI_PANEL_ROW_HEIGHT, 1);
        nk_layout_row_push(ctx, 200);
        nk_property_int(ctx, label, minValue, valueBuffer, maxValue, step, incPerPixel);
        nk_layout_row_end(ctx);
        int newValue = valueBuffer.get(0);
        setValue.accept(newValue);
        return newValue;
    }
    
    private boolean layoutCheckboxRow(NkContext ctx,
                                  String label,
                                  IntBuffer valueBuffer,
                                  boolean currentValue,
                                  boolean previousValue,
                                  Consumer<Boolean> setSelected) {

        if (previousValue != currentValue) {
            valueBuffer.put(0, (currentValue ? 1 : 0));
        }
        nk_layout_row_begin(ctx, NK_STATIC, GUI_PANEL_ROW_HEIGHT, 1);
        nk_layout_row_push(ctx, 200);
        nk_checkbox_label(ctx, label, valueBuffer);
        nk_layout_row_end(ctx);
        boolean newValue = (valueBuffer.get(0) != 0);
        setSelected.accept(newValue);
        return newValue;
    }
}