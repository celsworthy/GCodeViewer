package celuk.gcodeviewer.gui;

import celuk.language.I18n;
import celuk.gcodeviewer.engine.RenderParameters;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.joml.Vector3f;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import org.lwjgl.nuklear.NkColor;

public class GCVControlPanel extends GCVPanel {

    // These values are used GUI GCVControlPanel.
    public static final int GUI_CONTROL_PANEL_WIDTH = 260;
    public static final int GUI_CONTROL_PANEL_OPEN_HEIGHT = 265;
    public static final int GUI_CONTROL_PANEL_ROW_HEIGHT = 35;
    public static final int GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT = 40;
    public static final int GUI_CONTROL_PANEL_CLOSED_HEIGHT = 30;
    public static final int GUI_CONTROL_PANEL_SIDE_WIDTH = 10;

    private String resetViewMsg = "controlPanel.resetView";
    private String loadGCodeMsg = "controlPanel.loadGCode";
    private String showAnglesMsg = "controlPanel.showAngles";
    private String reloadGCodeMsg = "controlPanel.reloadGCode";
    private String showMovesMsg = "controlPanel.showMoves";
    private String showOnlySelectedMsg = "controlPanel.showOnlySelected";
    private String showToolNMsg = "controlPanel.showToolN";
    private String colourAsTypeMsg = "controlPanel.colourAsType";
    private String frameTimeMsg = "controlPanel.frameTime";

    private List<Integer> toolList = new ArrayList<>();
    private List<String> typeList = new ArrayList<>();
    private Map<String, String> typeI18nMap = new HashMap<>();
    private boolean showAdvanceOptions = false;
    
    public GCVControlPanel(boolean showAdvanceOptions) {
        this.showAdvanceOptions = showAdvanceOptions;
    }

    public void loadMessages() {
        resetViewMsg = I18n.t(resetViewMsg);
        loadGCodeMsg = I18n.t(loadGCodeMsg);
        showAnglesMsg = I18n.t(showAnglesMsg);
        reloadGCodeMsg = I18n.t(reloadGCodeMsg);
        showMovesMsg = I18n.t(showMovesMsg);
        showOnlySelectedMsg = I18n.t(showOnlySelectedMsg);
        showToolNMsg = I18n.t(showToolNMsg);
        colourAsTypeMsg = I18n.t(colourAsTypeMsg);
        frameTimeMsg = I18n.t(frameTimeMsg);
    }
    
    public void setToolList(List<Integer> toolList) {
        this.toolList = toolList;
    }
    
    public void setTypeList(List<String> typeList) {
        this.typeList = typeList;
        typeI18nMap.clear();
        typeList.forEach(t -> typeI18nMap.put(t, I18n.t(t)));
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            float windowPaddingX = ctx.style().window().padding().x();
            float windowPaddingY = ctx.style().window().padding().y();
            float groupPaddingX = ctx.style().window().group_padding().x();
            float groupPaddingY = ctx.style().window().group_padding().y();
            boolean colourAsType = (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE);
            boolean colourAsData = (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_DATA);
            
            if (panelExpanded) {
                panelWidth = GUI_CONTROL_PANEL_WIDTH;
                panelHeight = GUI_CONTROL_PANEL_OPEN_HEIGHT + toolList.size() * GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT;
                if (showAdvanceOptions)
                    panelHeight += GUI_CONTROL_PANEL_TOOL_ROW_HEIGHT;
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
                        nk_layout_row_dynamic(ctx, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                        if(nk_button_label(ctx, loadGCodeMsg)) {
                            renderParameters.setLoadGCodeRequested();
                        }
                        nk_layout_row_dynamic(ctx, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                        if(nk_button_label(ctx, reloadGCodeMsg)) {
                            renderParameters.setReloadGCodeRequested();
                        }
                        layoutCheckboxRow(ctx,
                                          w,
                                          showMovesMsg,
                                          renderParameters.getShowMoves(),
                                          renderParameters::setShowMoves);

                        if (showAdvanceOptions) {
                            layoutCheckboxRow(ctx,
                                          w,
                                          showAnglesMsg,
                                          renderParameters.getShowAngles(),
                                          renderParameters::setShowAngles);
                        }

                        layoutCheckboxRow(ctx,
                                          w,
                                          showOnlySelectedMsg,
                                          renderParameters.getShowOnlySelected(),
                                          renderParameters::setShowOnlySelected);

                        IntBuffer checkBuffer = stack.mallocInt(1);
                        NkStyleToggle checkboxStyle = ctx.style().checkbox();
                        NkColor cbnc = NkColor.mallocStack();
                        NkColor cbhc = NkColor.mallocStack();
                        cbnc.set(checkboxStyle.cursor_normal().data().color());
                        cbhc.set(checkboxStyle.cursor_hover().data().color());
                        NkColor tc = NkColor.mallocStack();
                        toolList.forEach(t -> {
                            String label = showToolNMsg.replaceAll("#1", Integer.toString(t));
                            boolean currentValue = renderParameters.getShowFlagForTool(t);
                            checkBuffer.put(0, (currentValue ? 1 : 0));
                            if (!colourAsType && !colourAsData)
                            {
                                Vector3f c = renderParameters.getColourForTool(t);
                                tc.set((byte)(255.0f * c.x() + 0.5f), (byte)(255.0f * c.y() + 0.5f), (byte)(255.0f * c.z() + 0.5f), (byte)255);
                                checkboxStyle.cursor_normal().data().color().set(tc);
                                checkboxStyle.cursor_hover().data().color().set(tc);
                            }
                            nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                            nk_layout_row_push(ctx, w);
                            nk_checkbox_label(ctx, label, checkBuffer);
                            nk_layout_row_end(ctx);
                            renderParameters.setShowFlagForTool(t, (checkBuffer.get(0) != 0));
                        });

                        checkboxStyle.cursor_normal().data().color().set(cbhc);
                        checkboxStyle.cursor_hover().data().color().set(cbhc);
                        layoutCheckboxRow(ctx,
                                          w,
                                          colourAsTypeMsg,
                                          (renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_TYPE ||
                                           renderParameters.getColourMode() == RenderParameters.ColourMode.COLOUR_AS_DATA),
                                          (f) -> {
                                              if (f) {
                                                  if (colourAsData)
                                                      renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_DATA);
                                                  else
                                                      renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TYPE);
                                              }
                                              else
                                                  renderParameters.setColourMode(RenderParameters.ColourMode.COLOUR_AS_TOOL);
                                          });
                        if (colourAsType) {
                            typeList.forEach(t -> {
                                Vector3f c = renderParameters.getColourForType(t);
                                tc.set((byte)(255.0f * c.x() + 0.5f), (byte)(255.0f * c.y() + 0.5f), (byte)(255.0f * c.z() + 0.5f), (byte)255);
                                checkboxStyle.cursor_normal().data().color().set(tc);
                                checkboxStyle.cursor_hover().data().color().set(tc);
                                nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                                nk_layout_row_push(ctx, w);
                                boolean currentValue = renderParameters.getShowFlagForType(t);
                                checkBuffer.put(0, (currentValue ? 1 : 0));
                                
                                nk_checkbox_label(ctx, typeI18nMap.get(t), checkBuffer);
                                nk_layout_row_end(ctx);
                                renderParameters.setShowFlagForType(t, (checkBuffer.get(0) != 0));
                            });
                        }

                        checkboxStyle.cursor_normal().data().color().set(cbhc);
                        checkboxStyle.cursor_hover().data().color().set(cbhc);
 
                        // Show the frame rate.
                        nk_layout_row_dynamic(ctx, GUI_CONTROL_PANEL_ROW_HEIGHT, 1);
                        double frameTime = renderParameters.getFrameTime();
                        DecimalFormat ftFormat = new DecimalFormat("0.0"); 
                        nk_label(ctx, frameTimeMsg.replaceAll("#1", ftFormat.format(1000.0 * frameTime)), NK_TEXT_ALIGN_LEFT);

                        nk_group_end(ctx);
                    }
                    nk_layout_row_push(ctx, GUI_CONTROL_PANEL_SIDE_WIDTH);
                    layoutSideButton(ctx, renderParameters); 
                    nk_layout_row_end(ctx);
                }
                else {
                    nk_layout_row_begin(ctx, NK_STATIC, GUI_CONTROL_PANEL_CLOSED_HEIGHT - 2.0f * windowPaddingY, 1);
                    nk_layout_row_push(ctx, GUI_CONTROL_PANEL_SIDE_WIDTH);
                    layoutSideButton(ctx, renderParameters); 
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