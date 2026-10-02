package net.neverandy.vr.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neverandy.vr.Config;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer:
 * <ul>
 * <li>the block they look at gets a bright box when it carries a redstone signal: a pulsing translucent fill, a thick
 * dark border and a bright border on top of it. Stronger signals are brighter.</li>
 * <li>every powered block wired to it gets a thinner box in the path color, so the whole circuit shows.</li>
 * <li>redstone dust and powered blocks nearby show their signal strength, repeaters their delay and comparators their
 * mode.</li>
 * <li>pistons, dispensers and droppers that are quasi-powered but have not updated get a box and a hint.</li>
 * </ul>
 * Everything is drawn through other blocks, so it is never hidden. Colors, outline thickness and range are set in the
 * client config, config/vr-client.properties.
 */
public class HighlightRenderer implements ClientModInitializer {

    private static final float LABEL_SCALE = 0.025F;

    @Override
    public void onInitializeClient() {
        Config.load();
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HighlightRenderer::onRenderLevel);
    }

    private static void onRenderLevel(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getMainHandItem().is(VisualRedstone.REDSTONE_VISUALIZER)
            || player.getOffhandItem().is(VisualRedstone.REDSTONE_VISUALIZER))) {
            return;
        }
        Level level = player.level();
        BlockPos target = null;
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            target = ((BlockHitResult) hit).getBlockPos();
        }
        RedstoneScanner.Result scan = RedstoneScanner.scan(level, player.blockPosition(), target, Config.range);

        Camera camera = context.camera();
        Vec3 cam = camera.getPosition();
        PoseStack poseStack = context.matrixStack();
        float thickness = Config.thickness;
        int highlight = Config.highlightColor;
        int pathColor = Config.pathColor;
        int qcColor = Config.qcColor;
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

        begin();
        if (Config.powerPath) {
            for (BlockPos pos : scan.path) {
                if (!pos.equals(target)) {
                    box(poseStack, level, pos, cam, pathColor, 0.12F, 0.6F, thickness * 0.6F);
                }
            }
        }
        if (Config.qcHints) {
            for (RedstoneScanner.Mark mark : scan.marks.values()) {
                if (mark.qc) {
                    box(poseStack, level, mark.pos, cam, qcColor, 0.15F + 0.1F * pulse, 0.85F, thickness);
                }
            }
        }
        if (target != null && level.getWorldBorder().isWithinBounds(target)) {
            BlockState state = level.getBlockState(target);
            int power = state.isAir() ? 0 : RedstoneScanner.power(level, target, state);
            if (power > 0) {
                float strength = 0.55F + 0.45F * power / 15.0F;
                box(poseStack, level, target, cam, scale(highlight, strength), 0.18F + 0.12F * pulse, 1.0F, thickness);
            }
        }
        end();

        labels(minecraft, poseStack, camera, scan, highlight, qcColor);
    }

    private static void labels(Minecraft minecraft, PoseStack poseStack, Camera camera, RedstoneScanner.Result scan,
                               int highlight, int qcColor) {
        boolean numbers = Config.signalNumbers;
        boolean info = Config.componentInfo;
        boolean qc = Config.qcHints;
        Font font = minecraft.font;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        Vec3 cam = camera.getPosition();
        for (RedstoneScanner.Mark mark : scan.marks.values()) {
            String number = numbers && (mark.wire || mark.power > 0) ? Integer.toString(mark.power) : null;
            String text = mark.info != null && (mark.qc ? qc : info) ? mark.info : null;
            if (number == null && text == null) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(mark.pos.getX() + 0.5 - cam.x, mark.pos.getY() + (mark.wire ? 0.25 : 0.5) - cam.y,
                mark.pos.getZ() + 0.5 - cam.z);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            Matrix4f matrix = poseStack.last().pose();
            float y = text != null && number != null ? -font.lineHeight : -font.lineHeight / 2.0F;
            if (number != null) {
                int color = mark.power > 0 ? scale(highlight, 0.55F + 0.45F * mark.power / 15.0F) : 0xAAAAAA;
                font.drawInBatch(number, -font.width(number) / 2.0F, y, 0xFF000000 | color, false, matrix, buffers,
                    Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
                y += font.lineHeight;
            }
            if (text != null) {
                font.drawInBatch(text, -font.width(text) / 2.0F, y, 0xFF000000 | (mark.qc ? qcColor : 0xFFFFFF), false,
                    matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        // Drawn through other blocks, so the boxes are never hidden.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void end() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** A translucent fill, a dark border and a colored border on top, around the block's shape. */
    private static void box(PoseStack poseStack, Level level, BlockPos pos, Vec3 cam, int color, float fillAlpha,
                            float lineAlpha, float thickness) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        AABB bounds = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        AABB box = bounds.move(pos).inflate(0.004).move(-cam.x, -cam.y, -cam.z);
        float r = (color >> 16 & 0xFF) / 255.0F, g = (color >> 8 & 0xFF) / 255.0F, b = (color & 0xFF) / 255.0F;
        PoseStack.Pose pose = poseStack.last();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        BoxShapes.faces(buffer, pose, box, r, g, b, fillAlpha);
        tesselator.end();

        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(thickness * 2.0F);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        BoxShapes.edges(buffer, pose, box, 0.0F, 0.0F, 0.0F, 0.85F * lineAlpha);
        tesselator.end();

        RenderSystem.lineWidth(thickness);
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        BoxShapes.edges(buffer, pose, box, r, g, b, lineAlpha);
        tesselator.end();
    }

    /** The color with each channel multiplied by the factor. */
    private static int scale(int color, float factor) {
        int r = Math.round((color >> 16 & 0xFF) * factor);
        int g = Math.round((color >> 8 & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return r << 16 | g << 8 | b;
    }
}
