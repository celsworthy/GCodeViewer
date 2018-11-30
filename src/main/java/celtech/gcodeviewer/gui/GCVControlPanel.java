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
import java.util.function.Consumer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GCVControlPanel {

    private final IntBuffer topValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int topValue = 0;
    private final IntBuffer bottomValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int bottomValue = 0;
    private final IntBuffer firstValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int firstValue = 0;
    private final IntBuffer lastValueBuffer = BufferUtils.createIntBuffer(1).put(0, 0);
    private int lastValue = 0;

    public GCVControlPanel() {
    }

    public void layout(NkContext ctx, int x, int y, RenderParameters renderParameters) {
        try (MemoryStack stack = stackPush()) {
            NkRect rect = NkRect.mallocStack(stack);
            if (nk_begin(ctx,
                         "ControlPanel",
                         nk_rect(x, y, 220, 165, rect),
                         0)) {
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
                    step = 1000;
                else
                    step = 10000;
                lastValue = layoutPropertyRow(ctx,
                                              "Last",
                                              renderParameters.getFirstLineToRender(),
                                              lastValueBuffer,
                                              renderParameters.getNumberOfLines(),
                                              step,
                                              step,
                                              renderParameters.getLastLineToRender(),
                                              lastValue,
                                              renderParameters::setLastLineToRender);
                firstValue = layoutPropertyRow(ctx,
                                               "First",
                                               0,
                                               firstValueBuffer,
                                               renderParameters.getNumberOfLines(),
                                               step,
                                               step,
                                               renderParameters.getFirstLineToRender(),
                                               firstValue,
                                               renderParameters::setFirstLineToRender);
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
            previousValue = currentValue;
            valueBuffer.put(0, currentValue);
        }
        nk_layout_row_begin(ctx, NK_STATIC, 35, 4);
        nk_layout_row_push(ctx, 200);
        nk_property_int(ctx, label, minValue, valueBuffer, maxValue, step, incPerPixel);
        nk_layout_row_end(ctx);
        int newValue = valueBuffer.get(0);
        setValue.accept(newValue);
        return newValue;
    }
}