/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.i18n.MessageLookup;
import celtech.gcodeviewer.engine.RenderParameters;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Vector3f;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import org.lwjgl.nuklear.NkColor;

public class GCVControlPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_CONTROL_PANEL_WIDTH = 260;
    public static final int GUI_CONTROL_PANEL_OPEN_HEIGHT = 185;
    public static final int GUI_CONTROL_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT = 40;
    public static final int GUI_CONTROL_PANEL_CLOSED_HEIGHT = 30;
    public static final int GUI_CONTROL_PANEL_SIDE_WIDTH = 10;

    private String resetViewMsg = "controlPanel.resetView";
    private String showMovesMsg = "controlPanel.showMoves";
    private String showOnlySelectedMsg = "controlPanel.showOnlySelected";
    private String showToolNMsg = "controlPanel.showToolN";
    private String colourAsTypeMsg = "controlPanel.colourAsType";
    private String frameRateMsg = "controlPanel.frameRate";

    private boolean panelExpanded = false;
    private float panelX = 0.0f;
    private float panelY = 0.0f;
    private float panelWidth = 0.0f;
    private float panelHeight = 0.0f;
    
    List<Integer> toolList = new ArrayList<>();
    List<String> typeList = new ArrayList<>();
    
    public GCVControlPanel() {
    }

    public void loadMessages() {
        resetViewMsg = MessageLookup.i18n(resetViewMsg);
        showMovesMsg = MessageLookup.i18n(showMovesMsg);
        showOnlySelectedMsg = MessageLookup.i18n(showOnlySelectedMsg);
        showToolNMsg = MessageLookup.i18n(showToolNMsg);
        colourAsTypeMsg = MessageLookup.i18n(colourAsTypeMsg);
        frameRateMsg = MessageLookup.i18n(frameRateMsg);
    }
    
    public void setPanelExpanded(boolean panelExpanded) {
        this.panelExpanded = panelExpanded;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }
    
    public int getPanelX() {
        return (int)panelX;
    }
    
    public int getPanelY() {
        return (int)panelY;
    }

    public int getPanelWidth() {
        return (int)panelWidth;
    }

    public int getPanelHeight() {
        return (int)panelHeight;
    }

    public void setToolList(List<Integer> toolList) {
        this.toolList = toolList;
    }
    
    public void setTypeList(List<String> typeList) {
        this.typeList = typeList;
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            float windowPaddingX = ctx.style().window().padding().x();
            float windowPaddingY = ctx.style().window().padding().y();
            float groupPaddingX = ctx.style().window().group_padding().x();
            float groupPaddingY = ctx.style().window().group_padding().y();
            boolean colourAsType = (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE);
            
            if (panelExpanded) {
                panelWidth = GUI_CONTROL_PANEL_WIDTH;
                panelHeight = GUI_CONTROL_PANEL_OPEN_HEIGHT + toolList.size() * GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT;
                if (colourAsType)
                    panelHeight += typeList.size() * GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT;
            }
            else {
                panelWidth = GUI_CONTROL_PANEL_SIDE_WIDTH + 4.0f * windowPaddingX;
                panelHeight = GUI_CONTROL_PANEL_CLOSED_HEIGHT;
            }
            panelX = x;
            panelY = y;
            nk_rect(panelX, panelY, panelWidth, panelHeight, rect);
                
            if (nk_begin(ctx, "Control Panel", rect, NK_WINDOW_NO_SCROLLBAR)) {
                if (panelExpanded) {
                    float w = rect.w() - 4.0f * windowPaddingX - 4.0f * groupPaddingX - GUI_CONTROL_PANEL_SIDE_WIDTH;
                    nk_layout_row_begin(ctx, NK_STATIC, rect.h() - 2.0f * windowPaddingY, 2);
                    nk_layout_row_push(ctx, w);
                    if (nk_group_begin(ctx, "ControlGroup", NK_WINDOW_NO_SCROLLBAR)) {
                        nk_layout_row_dynamic(ctx, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                        if(nk_button_label(ctx, resetViewMsg)) {
                            renderParameters.setViewResetRequired();
                        }
                        layoutCheckboxRow(ctx,
                                          w,
                                          showMovesMsg,
                                          renderParameters.getShowMoves(),
                                          renderParameters::setShowMoves);

                        layoutCheckboxRow(ctx,
                                          w,
                                          showOnlySelectedMsg,
                                          renderParameters.getShowOnlySelected(),
                                          renderParameters::setShowOnlySelected);

                        IntBuffer toolBuffer = stack.mallocInt(1);
                        NkStyleToggle checkboxStyle = ctx.style().checkbox();
                        NkColor cbnc = NkColor.mallocStack();
                        NkColor cbhc = NkColor.mallocStack();
                        cbnc.set(checkboxStyle.cursor_normal().data().color());
                        cbhc.set(checkboxStyle.cursor_hover().data().color());
                        NkColor tc = NkColor.mallocStack();
                        toolList.forEach(t -> {
                            String label = showToolNMsg.replaceAll("#1", Integer.toString(t));
                            boolean currentValue = renderParameters.getShowFlagForTool(t);
                            toolBuffer.put(0, (currentValue ? 1 : 0));
                            if (!colourAsType)
                            {
                                Vector3f c = renderParameters.getColourForTool(t);
                                tc.set((byte)(255.0f * c.x() + 0.5f), (byte)(255.0f * c.y() + 0.5f), (byte)(255.0f * c.z() + 0.5f), (byte)255);
                                checkboxStyle.cursor_normal().data().color().set(tc);
                                checkboxStyle.cursor_hover().data().color().set(tc);
                            }
                            nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                            nk_layout_row_push(ctx, w);
                            nk_checkbox_label(ctx, label, toolBuffer);
                            nk_layout_row_end(ctx);
                            renderParameters.setShowFlagForTool(t, (toolBuffer.get(0) != 0));
                        });

                        checkboxStyle.cursor_normal().data().color().set(cbhc);
                        checkboxStyle.cursor_hover().data().color().set(cbhc);
                        layoutCheckboxRow(ctx,
                                          w,
                                          colourAsTypeMsg,
                                          (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE),
                                          (f) -> { renderParameters.setColourMode(f ? RenderParameters.ColourMode.COLOUR_AS_TYPE 
                                                                                    : RenderParameters.ColourMode.COLOUR_AS_TOOL); });

                        if (colourAsType) {
                            float cb = checkboxStyle.border();
                            NkColor cbta = NkColor.mallocStack();
                            cbta.set(checkboxStyle.text_active());
                            NkColor cbth = NkColor.mallocStack();
                            cbth.set(checkboxStyle.text_hover());
                            NkColor cbtn = NkColor.mallocStack();
                            cbtn.set(checkboxStyle.text_normal());
                            NkColor cbbc = NkColor.mallocStack();
                            cbbc.set(checkboxStyle.border_color());
                            NkColor cba = NkColor.mallocStack();
                            cba.set(checkboxStyle.active().data().color());
                            NkColor cbh = NkColor.mallocStack();
                            cbh.set(checkboxStyle.hover().data().color());
                            NkColor cbn = NkColor.mallocStack();
                            cbn.set(checkboxStyle.normal().data().color());
                            
                            checkboxStyle.border(0.0f);
                            checkboxStyle.text_active().set(cbtn);
                            checkboxStyle.text_hover().set(cbtn);
                            checkboxStyle.active().data().color().set(cbn);
                            checkboxStyle.hover().data().color().set(cbn);
                            
                            typeList.forEach(t -> {
                                Vector3f c = renderParameters.getColourForType(t);
                                tc.set((byte)(255.0f * c.x() + 0.5f), (byte)(255.0f * c.y() + 0.5f), (byte)(255.0f * c.z() + 0.5f), (byte)255);
                                checkboxStyle.cursor_normal().data().color().set(tc);
                                checkboxStyle.cursor_hover().data().color().set(tc);
                                checkboxStyle.border_color().set(tc);
                                nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                                nk_layout_row_push(ctx, w);
                                toolBuffer.put(0, 1);
                                nk_checkbox_label(ctx, t, toolBuffer);
                                nk_layout_row_end(ctx);
                            });
                            checkboxStyle.border(cb);
                            checkboxStyle.border_color().set(cbbc);
                            checkboxStyle.text_active().set(cbta);
                            checkboxStyle.text_hover().set(cbth);
                            checkboxStyle.active().data().color().set(cba);
                            checkboxStyle.hover().data().color().set(cbh);
                        }

                        checkboxStyle.cursor_normal().data().color().set(cbhc);
                        checkboxStyle.cursor_hover().data().color().set(cbhc);
 
                        // Show the frame rate.
                        nk_layout_row_dynamic(ctx, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                        double frameTime = renderParameters.getFrameTime();
                        double fps = 0.0;
                        if (frameTime > 0.0)
                            fps = 1.0 / frameTime;
                        DecimalFormat fpsFormat = new DecimalFormat("0.#"); 
                        DecimalFormat ftFormat = new DecimalFormat("0.00"); 
                        nk_label(ctx, frameRateMsg.replaceAll("#1", fpsFormat.format(fps)).replaceAll("#2", ftFormat.format(frameTime)), NK_TEXT_ALIGN_LEFT);

                        nk_group_end(ctx);
                    }
                    nk_layout_row_push(ctx, GUI_CONTROL_PANEL_SIDE_WIDTH);
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                        renderParameters.setRenderRequired();
                    }
                    nk_layout_row_end(ctx);
                }
                else {
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_CLOSED_HEIGHT - 2.0f * windowPaddingY, 1);
                    nk_layout_row_push(ctx, GUI_CONTROL_PANEL_SIDE_WIDTH);
                    if(nk_button_label(ctx, "")) {
                        panelExpanded = !panelExpanded;
                        renderParameters.setRenderRequired();
                    }
                    nk_layout_row_end(ctx);
                }
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