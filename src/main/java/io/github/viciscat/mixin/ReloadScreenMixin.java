package io.github.viciscat.mixin;

import com.google.common.primitives.Floats;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Tessellator;
import net.modificationstation.stationapi.api.client.resource.ReloadScreenManager;
import net.modificationstation.stationapi.api.resource.ResourceReload;
import lombok.val;
import org.spongepowered.asm.mixin.*;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static net.modificationstation.stationapi.api.StationAPI.LOGGER;
import static net.modificationstation.stationapi.api.util.math.MathHelper.ceil;
import static org.lwjgl.opengl.GL11.*;

@Mixin(targets = "net.modificationstation.stationapi.api.client.resource.ReloadScreen")
public abstract class ReloadScreenMixin extends Screen {

    @Shadow protected abstract void fill(int startX, int startY, int endX, int endY, int color);

    @Shadow(remap = false) private boolean exceptionThrown;

    @Shadow(remap = false) private boolean finished;

    @Shadow(remap = false) private Exception exception;

    @Shadow(remap = false) @Final private Runnable done;

    @Shadow(remap = false) @Final private Screen parent;

    @Shadow(remap = false) private float progress;
    
    @Shadow @Final private static int BACKGROUND_COLOR_DEFAULT_RED;
    @Shadow @Final private static int BACKGROUND_COLOR_DEFAULT_GREEN;
    @Shadow @Final private static int BACKGROUND_COLOR_DEFAULT_BLUE;
    @Shadow @Final private static int BACKGROUND_COLOR_EXCEPTION_RED;
    @Shadow @Final private static int BACKGROUND_COLOR_EXCEPTION_GREEN;
    @Shadow @Final private static int BACKGROUND_COLOR_EXCEPTION_BLUE;
    @Shadow @Final private Tessellator tessellator;
    @Shadow @Final private String logo;

    @Unique
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        super.render(mouseX, mouseY, delta);
        if (parent == null) renderEarly();
        else renderNormal(delta);

        Optional<ResourceReload> reload;
        progress = Floats.constrainToRange(progress * .95F + (isReloadStarted() && (reload = ReloadScreenManager.getCurrentReload()).isPresent() ? reload.orElse(null).getProgress() : 0) * .05F, 0, 1);
        if (Float.isNaN(progress)) progress = 0;
        if (!exceptionThrown && !finished && ReloadScreenManager.isReloadComplete()) {
            try {
                ReloadScreenManager.getCurrentReload().stream().peek(ResourceReload::throwException);
                finished = true;
            } catch (CompletionException e) {
                exceptionThrown = true;
                exception = e;
                LOGGER.error("An exception occurred during resource loading", e);
            }
        }
        if (finished) {
            ReloadScreenManagerAccessor.onFinish();
            done.run();
        }
    }

    @Unique
    private void renderEarly() {
        val color_black = (int) (0xFF) << 24 | (0x000000);
        val color_white = (int) (0xFF) << 24 | (0xFFFFFF);
        val color_mojang_red = (int) (0xFF) << 24 | (0xDD4F3B);
        val color_mojang_orange = (int) (0xFF) << 24 | (0xF6883E);
        val color_stationapi_default = (int) (0xFF) << 24 | (BACKGROUND_COLOR_DEFAULT_RED << 16) | (BACKGROUND_COLOR_DEFAULT_GREEN << 8)  | BACKGROUND_COLOR_DEFAULT_BLUE;
        val color_stationapi_exception = (int) (0xFF) << 24 | (BACKGROUND_COLOR_EXCEPTION_RED << 16) | (BACKGROUND_COLOR_EXCEPTION_GREEN << 8)  | BACKGROUND_COLOR_EXCEPTION_BLUE;

        if (exceptionThrown) {
            fill(0, 0, width, height, color_stationapi_exception);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 40), color_black);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 50), color_black);
            drawVerticalLine(40, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_black);
            drawVerticalLine(width - 40 - 1, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_black);
            fill(43, (int) ((height / 2) + 43), ceil((width - (43) * 2) * progress + 43), (int) ((height / 2) + 50 - 2), color_black);

            minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(logo));
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            tessellator.startQuads();
            tessellator.color(color_black);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 - 20, 0, 0, 0);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 + 20, 0, 0, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 + 20, 0, 1, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 - 20, 0, 1, 0);
            tessellator.draw();
            glDisable(GL_BLEND);
        } else if (logo.equals("_dimando")) {
            fill(0, 0, width, height, color_white);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 40), color_stationapi_default);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 50), color_stationapi_default);
            drawVerticalLine(40, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_stationapi_default);
            drawVerticalLine(width - 40 - 1, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_stationapi_default);
            fill(43, (int) ((height / 2) + 43), ceil((width - (43) * 2) * progress + 43), (int) ((height / 2) + 50 - 2), color_mojang_orange);

            minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(logo));
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            tessellator.startQuads();
            tessellator.color(color_stationapi_default);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 - 20, 0, 0, 0);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 + 20, 0, 0, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 + 20, 0, 1, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 - 20, 0, 1, 0);
            tessellator.draw();
            glDisable(GL_BLEND);
        } else if (logo.equals("_old")) {
            fill(0, 0, width, height, color_white);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 40), color_mojang_orange);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 50), color_mojang_orange);
            drawVerticalLine(40, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_mojang_orange);
            drawVerticalLine(width - 40 - 1, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_mojang_orange);
            fill(43, (int) ((height / 2) + 43), ceil((width - (43) * 2) * progress + 43), (int) ((height / 2) + 50 - 2), color_mojang_red);

            minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(logo));
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            tessellator.startQuads();
            tessellator.color(color_mojang_orange);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 - 20, 0, 0, 0);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 + 20, 0, 0, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 + 20, 0, 1, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 - 20, 0, 1, 0);
            tessellator.draw();
            glDisable(GL_BLEND);
        } else {
            fill(0, 0, width, height, color_white);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 40), color_mojang_red);
            drawHorizontalLine(40, width - 40 - 1, (int) ((height / 2) + 50), color_mojang_red);
            drawVerticalLine(40, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_mojang_red);
            drawVerticalLine(width - 40 - 1, (int) ((height / 2) + 50), (int) ((height / 2) + 40), color_mojang_red);
            fill(43, (int) ((height / 2) + 43), ceil((width - (43) * 2) * progress + 43), (int) ((height / 2) + 50 - 2), color_mojang_orange);

            minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(logo));
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            tessellator.startQuads();
            tessellator.color(color_mojang_red);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 - 20, 0, 0, 0);
            tessellator.vertex(width / 2D - 120, (height - 50D) / 2 + 20, 0, 0, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 + 20, 0, 1, 1);
            tessellator.vertex(width / 2D + 120, (height - 50D) / 2 - 20, 0, 1, 0);
            tessellator.draw();
            glDisable(GL_BLEND);
        }
    }

    /**
     * See {@link net.minecraft.client.Minecraft#method_2109(int, int, int, int, int, int)}
     */
    @Unique
    private void drawMojangLogoQuad(int i, int j) {
        float f = 0.00390625f;
        float f2 = 0.00390625f;
        tessellator.startQuads();
        tessellator.vertex(i, j + 256, 0.0, 0, 256 * f2);
        tessellator.vertex(i + 256, j + 256, 0.0, 256 * f, 256 * f2);
        tessellator.vertex(i + 256, j, 0.0, 256 * f, 0);
        tessellator.vertex(i, j, 0.0, 0, 0);
        tessellator.draw();
    }

    @Unique
    private void renderNormal(float delta) {
        parent.render(-1, -1, delta);
        this.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        renderText(Color.WHITE, true);
    }

    @Unique
    private void renderText(Color textColor, boolean shadow) {
        if (exceptionThrown) textRenderer.draw("Oh noes! An error occurred, check your logs.", 0,0, textColor.getRGB(), shadow);
        else textRenderer.draw("Loading resources...", 0, 0, textColor.getRGB(), shadow);
        List<String> locations = ReloadScreenManagerAccessor.getLocations();
        String s = locations.isEmpty() ? "Doing the do": locations.get(locations.size() - 1);
        textRenderer.draw(s, 5, height-10, textColor.getRGB(), shadow);
        String text = NUMBER_FORMAT.format(progress*100f) + "%";
        int textRendererWidth = textRenderer.getWidth(text);
        textRenderer.draw(text, width-textRendererWidth-5, height-10, textColor.getRGB(), shadow);
    }

    /**
     * @author Vic is a Cat
     * @reason no animation so no need to wait
     */
    @Overwrite(remap = false)
    public boolean isReloadStarted() {
        return true;
    }
}
