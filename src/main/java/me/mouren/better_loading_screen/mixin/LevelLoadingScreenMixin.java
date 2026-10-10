package me.mouren.better_loading_screen.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    private static final int   BAR_HEIGHT                = 4;
    private static final float TEXT_SCALE                = 3.0F;
    private static final int   TEXT_COLOR                = 0xFFFFFFFF;
    private static final int   PROGRESS_BAR_COLOR        = 0xFF00FF00;
    private static final boolean DRAW_MINECRAFT_LOGO     = true;
    private static final Component DEFAULT_LOADING_TEXT  = Component.literal("§lLOADING...");

    protected LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY,
                          float partialTick, CallbackInfo ci) {
        ci.cancel();

        float progress = getProgress();

        int width  = this.width;
        int height = this.height;
        int bottom = height;
        int top    = bottom - BAR_HEIGHT;

        boolean hasProgress = progress > 0.0F;
        if (hasProgress) renderProgressBar(graphics, width, top, bottom, progress);

        int scaledTextHeight = (int) (9 * TEXT_SCALE);
        int textY            = top - scaledTextHeight - 5;

        renderLoadingText(graphics, textY);
        if (hasProgress) renderProgressPercentage(graphics, width, textY, progress);

        if (DRAW_MINECRAFT_LOGO) renderMinecraftLogo(graphics, width);
    }

    private float getProgress() {
        try {
            for (java.lang.reflect.Field f : LevelLoadingScreen.class.getDeclaredFields()) {
                if (f.getType() == StoringChunkProgressListener.class) {
                    f.setAccessible(true);
                    StoringChunkProgressListener listener = (StoringChunkProgressListener) f.get(this);
                    if (listener == null) return 0.0F;
                    return Mth.clamp(listener.getProgress() / 100.0F, 0.0F, 1.0F);
                }
            }
        } catch (Exception ignored) {}
        return 0.0F;
    }

    private void renderProgressBar(GuiGraphics graphics, int width, int top, int bottom, float progress) {
        int right = Mth.clamp((int) (progress * width), 0, width);
        if (right <= 0) return;
        graphics.fill(0, top, right, bottom, PROGRESS_BAR_COLOR);
    }

    private void renderLoadingText(GuiGraphics graphics, int textY) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(6.0F, textY, 0);
        pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);

        graphics.drawString(this.font, DEFAULT_LOADING_TEXT, 0, 0, TEXT_COLOR, true);

        pose.popPose();
    }

    private void renderProgressPercentage(GuiGraphics graphics,
                                          int width, int textY, float progress) {
        int percent = Mth.floor(progress * 100.0F);
        String percentText = percent + "%";
        int textWidth = this.font.width(percentText);
        int percentX  = width - textWidth - 6;
        int percentY  = textY + 18;

        graphics.drawString(this.font, percentText, percentX, percentY, TEXT_COLOR, true);
    }

    private void renderMinecraftLogo(GuiGraphics graphics, int width) {
        int logoWidth  = 256;
        int logoHeight = 64;
        int logoX = (width - logoWidth) / 2;
        int logoY = 25;

        graphics.blit(
                ResourceLocation.withDefaultNamespace("textures/gui/title/minecraft.png"),
                logoX, logoY,
                logoX + logoWidth, logoY + logoHeight,
                0, 1, 0, 1);
    }
}