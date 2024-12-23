package io.github.viciscat.mixin;

import com.google.common.primitives.Floats;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Tessellator;
import net.modificationstation.stationapi.api.client.resource.ReloadScreenManager;
import net.modificationstation.stationapi.api.resource.ResourceReload;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static net.modificationstation.stationapi.api.StationAPI.LOGGER;

@Mixin(targets = "net.modificationstation.stationapi.api.client.resource.ReloadScreen")
public abstract class ReloadScreenMixin extends Screen {

    @Shadow protected abstract void fill(int startX, int startY, int endX, int endY, int color);

    @Shadow(remap = false) private boolean exceptionThrown;

    @Shadow(remap = false) private boolean finished;

    @Shadow(remap = false) private Exception exception;

    @Shadow(remap = false) @Final private Runnable done;

    @Shadow(remap = false) @Final private Screen parent;

    @Shadow(remap = false) private float progress;
    @Shadow @Final private Tessellator tessellator;
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
        GL11.glBindTexture(3553, minecraft.textureManager.getTextureId("/title/mojang.png"));
        fill(0, 0, width, height, 0xFFFFFFFF);
        drawMojangLogoQuad((width-256)/2, (height-256)/2);
        GL11.glEnable(GL11.GL_BLEND);
        renderText(Color.BLACK, false);
        GL11.glDisable(GL11.GL_BLEND);
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
