package celtech.gcodeviewer.gui;

import celtech.gcodeviewer.engine.RenderParameters;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.BufferUtils.createByteBuffer;

import celtech.gcodeviewer.engine.renderers.GUIRenderer;
import celtech.gcodeviewer.shaders.GUIShader;

import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.NkAllocator;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkMouse;
import org.lwjgl.nuklear.NkStyleItem;
import org.lwjgl.nuklear.NkStyleProperty;
import org.lwjgl.nuklear.NkUserFont;
import org.lwjgl.nuklear.NkUserFontGlyph;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;


/**
 *
 * @author George Salter
 */
public class GUIManager {
    
    private static final NkAllocator ALLOCATOR;
    
    static {
        ALLOCATOR = NkAllocator.create()
            .alloc((handle, old, size) -> nmemAllocChecked(size))
            .mfree((handle, ptr) -> nmemFree(ptr));
    }
    
    private final GUIShader guiShader = new GUIShader();
    private final GUIRenderer guiRenderer;
    
    private final ByteBuffer ttf;
    
    private NkContext nkContext = NkContext.create();
    private NkUserFont default_font = NkUserFont.create();
    
    public GUIManager(long windowId, int windowWidth, int windowHeight, RenderParameters renderParameters) {
        try {
            this.ttf = ioResourceToByteBuffer("/celtech/gcodeviewer/resources/FiraSans-Regular.ttf", 512 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setup(windowId);
        guiRenderer = new GUIRenderer(nkContext, guiShader, windowWidth, windowHeight, renderParameters);
        setupFont();
        setupStyle();
    }
    
    public final NkContext setup(long windowId) {
        glfwSetCursorPosCallback(windowId, (window, xpos, ypos) -> nk_input_motion(nkContext, (int)xpos, (int)ypos));
      
        glfwSetMouseButtonCallback(windowId, (window, button, action, mods) -> {
            try (MemoryStack stack = stackPush()) {
                DoubleBuffer cx = stack.mallocDouble(1);
                DoubleBuffer cy = stack.mallocDouble(1);

                glfwGetCursorPos(window, cx, cy);

                int x = (int)cx.get(0);
                int y = (int)cy.get(0);
                
                int nkButton;
                
                switch (button) {
                    case GLFW_MOUSE_BUTTON_RIGHT:
                        nkButton = NK_BUTTON_RIGHT;
                        break;
                    case GLFW_MOUSE_BUTTON_MIDDLE:
                        nkButton = NK_BUTTON_MIDDLE;
                        break;
                    default:
                        nkButton = NK_BUTTON_LEFT;
                }

                nk_input_button(nkContext, nkButton, x, y, action == GLFW_PRESS);
            }
        });

        nk_init(nkContext, ALLOCATOR, null);
        
        nkContext.clip(it -> it
            .copy((handle, text, len) -> {
                if (len == 0) {
                    return;
                }

                try (MemoryStack stack = stackPush()) {
                    ByteBuffer str = stack.malloc(len + 1);
                    memCopy(text, memAddress(str), len);
                    str.put(len, (byte)0);

                    glfwSetClipboardString(windowId, str);
                }
            })
            .paste((handle, edit) -> {
                long text = nglfwGetClipboardString(windowId);
                if (text != NULL) {
                    nnk_textedit_paste(edit, text, nnk_strlen(text));
                }
            }));

        return nkContext;
    }
    
    private void setupFont() {
        int BITMAP_W = 1024;
        int BITMAP_H = 1024;

        int FONT_HEIGHT = 18;
        int fontTexID   = glGenTextures();

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(95);

        float scale;
        float descent;

        try (MemoryStack stack = stackPush()) {
            stbtt_InitFont(fontInfo, ttf);
            scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);

            IntBuffer d = stack.mallocInt(1);
            stbtt_GetFontVMetrics(fontInfo, null, d, null);
            descent = d.get(0) * scale;

            ByteBuffer bitmap = memAlloc(BITMAP_W * BITMAP_H);

            STBTTPackContext pc = STBTTPackContext.mallocStack(stack);
            stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
            stbtt_PackSetOversampling(pc, 4, 4);
            stbtt_PackFontRange(pc, ttf, 0, FONT_HEIGHT, 32, cdata);
            stbtt_PackEnd(pc);

            // Convert R8 to RGBA8
            ByteBuffer texture = memAlloc(BITMAP_W * BITMAP_H * 4);

            for (int i = 0; i < bitmap.capacity(); i++) {
                texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
            }

            texture.flip();

            glBindTexture(GL_TEXTURE_2D, fontTexID);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            memFree(texture);
            memFree(bitmap);
        }

        default_font
            .width((handle, h, text, len) -> {
                float text_width = 0;
                try (MemoryStack stack = stackPush()) {
                    IntBuffer unicode = stack.mallocInt(1);

                    int glyph_len = nnk_utf_decode(text, memAddress(unicode), len);
                    int text_len  = glyph_len;

                    if (glyph_len == 0) {
                        return 0;
                    }

                    IntBuffer advance = stack.mallocInt(1);
                    while (text_len <= len && glyph_len != 0) {
                        if (unicode.get(0) == NK_UTF_INVALID) {
                            break;
                        }

                        /* query currently drawn glyph information */
                        stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
                        text_width += advance.get(0) * scale;

                        /* offset next glyph */
                        glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len);
                        text_len += glyph_len;
                    }
                }
                
                return text_width;
            })
            .height(FONT_HEIGHT)
            .query((handle, font_height, glyph, codepoint, next_codepoint) -> {
                try (MemoryStack stack = stackPush()) {
                    FloatBuffer x = stack.floats(0.0f);
                    FloatBuffer y = stack.floats(0.0f);

                    STBTTAlignedQuad q       = STBTTAlignedQuad.mallocStack(stack);
                    IntBuffer        advance = stack.mallocInt(1);

                    stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
                    stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);

                    NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);

                    ufg.width(q.x1() - q.x0());
                    ufg.height(q.y1() - q.y0());
                    ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent));
                    ufg.xadvance(advance.get(0) * scale);
                    ufg.uv(0).set(q.s0(), q.t0());
                    ufg.uv(1).set(q.s1(), q.t1());
                }
            })
            .texture(it -> it
                .id(fontTexID));

        nk_style_set_font(nkContext, default_font);    
    }
    
    public void setupStyle() {
        try (MemoryStack stack = stackPush()) {
            NkColor normalColour = NkColor.mallocStack(stack);
            normalColour.r((byte)0).g((byte)0).b((byte)0).a((byte)128);
            NkColor hoverColour = NkColor.mallocStack(stack);
            hoverColour.r((byte)128).g((byte)128).b((byte)128).a((byte)255);
            NkColor activeColour = NkColor.mallocStack(stack);
            activeColour.r((byte)255).g((byte)255).b((byte)255).a((byte)255);

            NkStyleProperty propertyStyle = nkContext.style().property();
            propertyStyle.label_normal().set(normalColour);
            propertyStyle.label_hover().set(hoverColour);
            propertyStyle.label_active().set(activeColour);
            propertyStyle.edit().text_normal().set(normalColour);
            propertyStyle.edit().text_hover().set(hoverColour);
            propertyStyle.edit().text_active().set(activeColour);
            propertyStyle.sym_left(NK_SYMBOL_TRIANGLE_UP);
            propertyStyle.sym_right(NK_SYMBOL_TRIANGLE_DOWN);
            propertyStyle.dec_button().normal().data().color().set(normalColour);
            propertyStyle.dec_button().hover().data().color().set(hoverColour);
            propertyStyle.inc_button().active().data().color().set(activeColour);
            propertyStyle.inc_button().normal().data().color().set(normalColour);
            propertyStyle.inc_button().hover().data().color().set(hoverColour);
            propertyStyle.inc_button().active().data().color().set(activeColour);
        }
    }
    
    public void render() {
        guiShader.start();
        guiRenderer.render();
        guiShader.stop();
    }
    
    public void cleanUp() {
        guiRenderer.cleanUp();
        guiShader.cleanUp();
    }
    
    public void onWindowResize(int windowWidth, int windowHeight) {
        guiRenderer.onWindowResize(windowWidth, windowHeight);
    }
    
    public void onScroll(long window, double xoffset, double yoffset) {
        try (MemoryStack stack = stackPush()) {
            NkVec2 scroll = NkVec2.mallocStack(stack)
                .x((float)xoffset)
                .y((float)yoffset);
            nk_input_scroll(nkContext, scroll);
        }
    }

    public void onChar(long window, int codePoint) {
        nk_input_unicode(nkContext, codePoint);
    }

    public void onKey(long window, int key, int scancode, int action, int mods) {
        boolean press = (action == GLFW_PRESS);
        switch (key) {
            case GLFW_KEY_DELETE:
                nk_input_key(nkContext, NK_KEY_DEL, press);
                break;
            case GLFW_KEY_ENTER:
                nk_input_key(nkContext, NK_KEY_ENTER, press);
                break;
            case GLFW_KEY_TAB:
                nk_input_key(nkContext, NK_KEY_TAB, press);
                break;
            case GLFW_KEY_BACKSPACE:
                nk_input_key(nkContext, NK_KEY_BACKSPACE, press);
                break;
            case GLFW_KEY_UP:
                nk_input_key(nkContext, NK_KEY_UP, press);
                break;
            case GLFW_KEY_DOWN:
                nk_input_key(nkContext, NK_KEY_DOWN, press);
                break;
            case GLFW_KEY_HOME:
                nk_input_key(nkContext, NK_KEY_TEXT_START, press);
                nk_input_key(nkContext, NK_KEY_SCROLL_START, press);
                break;
            case GLFW_KEY_END:
                nk_input_key(nkContext, NK_KEY_TEXT_END, press);
                nk_input_key(nkContext, NK_KEY_SCROLL_END, press);
                break;
            case GLFW_KEY_PAGE_DOWN:
                nk_input_key(nkContext, NK_KEY_SCROLL_DOWN, press);
                break;
            case GLFW_KEY_PAGE_UP:
                nk_input_key(nkContext, NK_KEY_SCROLL_UP, press);
                break;
            case GLFW_KEY_LEFT_SHIFT:
            case GLFW_KEY_RIGHT_SHIFT:
                nk_input_key(nkContext, NK_KEY_SHIFT, press);
                break;
            case GLFW_KEY_LEFT_CONTROL:
            case GLFW_KEY_RIGHT_CONTROL:
                if (press) {
                    nk_input_key(nkContext, NK_KEY_COPY, glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_PASTE, glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_CUT, glfwGetKey(window, GLFW_KEY_X) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_UNDO, glfwGetKey(window, GLFW_KEY_Z) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_REDO, glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_LINE_START, glfwGetKey(window, GLFW_KEY_B) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_TEXT_LINE_END, glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS);
                } else {
                    nk_input_key(nkContext, NK_KEY_LEFT, glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_RIGHT, glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS);
                    nk_input_key(nkContext, NK_KEY_COPY, false);
                    nk_input_key(nkContext, NK_KEY_PASTE, false);
                    nk_input_key(nkContext, NK_KEY_CUT, false);
                    nk_input_key(nkContext, NK_KEY_SHIFT, false);
                }
                break;
        }
    }

    public void onCursorPos(long window, double x, double y) {
        nk_input_motion(nkContext, (int)x, (int)y);
    }
    
    public void onMouseButton(long window, double x, double y, int mouseButton, int action, int mods) {
        int nkButton;
        switch (mouseButton) {
            case GLFW_MOUSE_BUTTON_RIGHT:
                nkButton = NK_BUTTON_RIGHT;
                break;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                nkButton = NK_BUTTON_MIDDLE;
                break;
            default:
                nkButton = NK_BUTTON_LEFT;
        }

        nk_input_button(nkContext, nkButton, (int)x, (int)y, action == GLFW_PRESS);
    }
    
    public void pollEvents(long windowId) {
        nk_input_begin(nkContext);
        glfwPollEvents();
        
        // This is copied from the LWJGL demo and seems to be a bit
        // of boiler plate code, although I'm not sure what it does!
        NkMouse mouse = nkContext.input().mouse();
        if (mouse.grab()) {
            glfwSetInputMode(windowId, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        } else if (mouse.grabbed()) {
            float prevX = mouse.prev().x();
            float prevY = mouse.prev().y();
            glfwSetCursorPos(windowId, prevX, prevY);
            mouse.pos().x(prevX);
            mouse.pos().y(prevY);
        } else if (mouse.ungrab()) {
            glfwSetInputMode(windowId, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        nk_input_end(nkContext);
    }
    
    /**
     * Reads the specified resource and returns the raw data as a ByteBuffer.
     *
     * @param resource   the resource to read
     * @param bufferSize the initial buffer size
     *
     * @return the resource data
     *
     * @throws IOException if an IO error occurs
     */
    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;

        Path path = Paths.get(resource);
        if (Files.isReadable(path)) {
            try (SeekableByteChannel fc = Files.newByteChannel(path)) {
                buffer = BufferUtils.createByteBuffer((int)fc.size() + 1);
                while (fc.read(buffer) != -1) {
                    ;
                }
            }
        } else {
            try (
                InputStream source = GUIManager.class.getResourceAsStream(resource);
                ReadableByteChannel rbc = Channels.newChannel(source)
            ) {
                buffer = createByteBuffer(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);

                    if (bytes == -1) {
                        break;
                    }

                    if (buffer.remaining() == 0) {
                        buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2); // 50%
                    }
                }
            }
        }

        buffer.flip();

        return buffer.slice();
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);

        return newBuffer;
    }
}
