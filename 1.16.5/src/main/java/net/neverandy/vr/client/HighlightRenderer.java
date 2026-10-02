package net.neverandy.vr.client;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
 * client config.
 */
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Dist.CLIENT)
public class HighlightRenderer {

    private static final float LABEL_SCALE = 0.025F;
    private static final int FULL_BRIGHT = 0xF000F0;

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayerEntity player = minecraft.player;
        if (player == null || !(player.getHeldItemMainhand().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get()
            || player.getHeldItemOffhand().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get())) {
            return;
        }
        World level = player.world;
        BlockPos target = null;
        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit != null && hit.getType() == RayTraceResult.Type.BLOCK) {
            target = ((BlockRayTraceResult) hit).getPos();
        }
        RedstoneScanner.Result scan = RedstoneScanner.scan(level, player.getPosition(), target, Config.RANGE.get());

        ActiveRenderInfo camera = minecraft.gameRenderer.getActiveRenderInfo();
        Vector3d cam = camera.getProjectedView();
        MatrixStack poseStack = event.getMatrixStack();
        float thickness = Config.THICKNESS.get().floatValue();
        int highlight = Config.color(Config.HIGHLIGHT_COLOR, 0x40FF40);
        int pathColor = Config.color(Config.PATH_COLOR, 0x40C0FF);
        int qcColor = Config.color(Config.QC_COLOR, 0xFFAA00);
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

        begin();
        if (Config.POWER_PATH.get()) {
            for (BlockPos pos : scan.path) {
                if (!pos.equals(target)) {
                    box(poseStack, level, pos, cam, pathColor, 0.12F, 0.6F, thickness * 0.6F);
                }
            }
        }
        if (Config.QC_HINTS.get()) {
            for (RedstoneScanner.Mark mark : scan.marks.values()) {
                if (mark.qc) {
                    box(poseStack, level, mark.pos, cam, qcColor, 0.15F + 0.1F * pulse, 0.85F, thickness);
                }
            }
        }
        if (target != null && level.getWorldBorder().contains(target)) {
            BlockState state = level.getBlockState(target);
            int power = state.isAir(level, target) ? 0 : RedstoneScanner.power(level, target, state);
            if (power > 0) {
                float strength = 0.55F + 0.45F * power / 15.0F;
                box(poseStack, level, target, cam, scale(highlight, strength), 0.18F + 0.12F * pulse, 1.0F, thickness);
            }
        }
        end();

        labels(minecraft, poseStack, camera, scan, highlight, qcColor);
    }

    private static void labels(Minecraft minecraft, MatrixStack poseStack, ActiveRenderInfo camera,
                               RedstoneScanner.Result scan, int highlight, int qcColor) {
        boolean numbers = Config.SIGNAL_NUMBERS.get();
        boolean info = Config.COMPONENT_INFO.get();
        boolean qc = Config.QC_HINTS.get();
        FontRenderer font = minecraft.fontRenderer;
        IRenderTypeBuffer.Impl buffers = minecraft.getRenderTypeBuffers().getBufferSource();
        int background = (int) (minecraft.gameSettings.getTextBackgroundOpacity(0.25F) * 255.0F) << 24;
        Vector3d cam = camera.getProjectedView();
        for (RedstoneScanner.Mark mark : scan.marks.values()) {
            String number = numbers && (mark.wire || mark.power > 0) ? Integer.toString(mark.power) : null;
            String text = mark.info != null && (mark.qc ? qc : info) ? mark.info : null;
            if (number == null && text == null) {
                continue;
            }
            poseStack.push();
            poseStack.translate(mark.pos.getX() + 0.5 - cam.x, mark.pos.getY() + (mark.wire ? 0.25 : 0.5) - cam.y,
                mark.pos.getZ() + 0.5 - cam.z);
            poseStack.rotate(camera.getRotation());
            poseStack.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            Matrix4f matrix = poseStack.getLast().getMatrix();
            float y = text != null && number != null ? -font.FONT_HEIGHT : -font.FONT_HEIGHT / 2.0F;
            if (number != null) {
                int color = mark.power > 0 ? scale(highlight, 0.55F + 0.45F * mark.power / 15.0F) : 0xAAAAAA;
                font.renderString(number, -font.getStringWidth(number) / 2.0F, y, 0xFF000000 | color, false, matrix,
                    buffers, true, background, FULL_BRIGHT);
                y += font.FONT_HEIGHT;
            }
            if (text != null) {
                font.renderString(text, -font.getStringWidth(text) / 2.0F, y, 0xFF000000 | (mark.qc ? qcColor : 0xFFFFFF),
                    false, matrix, buffers, true, background, FULL_BRIGHT);
            }
            poseStack.pop();
        }
        buffers.finish();
    }

    private static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        // Drawn through other blocks, so the boxes are never hidden.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void end() {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /** A translucent fill, a dark border and a colored border on top, around the block's shape. */
    private static void box(MatrixStack poseStack, World level, BlockPos pos, Vector3d cam, int color, float fillAlpha,
                            float lineAlpha, float thickness) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        AxisAlignedBB bounds = shape.isEmpty() ? new AxisAlignedBB(0, 0, 0, 1, 1, 1) : shape.getBoundingBox();
        AxisAlignedBB box = bounds.offset(pos).grow(0.004).offset(-cam.x, -cam.y, -cam.z);
        float r = (color >> 16 & 0xFF) / 255.0F, g = (color >> 8 & 0xFF) / 255.0F, b = (color & 0xFF) / 255.0F;
        Matrix4f matrix = poseStack.getLast().getMatrix();

        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder buffer = tesselator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.faces(buffer, matrix, box, r, g, b, fillAlpha);
        tesselator.draw();

        RenderSystem.lineWidth(thickness * 2.0F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, 0.0F, 0.0F, 0.0F, 0.85F * lineAlpha);
        tesselator.draw();

        RenderSystem.lineWidth(thickness);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, r, g, b, lineAlpha);
        tesselator.draw();
    }

    /** The color with each channel multiplied by the factor. */
    private static int scale(int color, float factor) {
        int r = Math.round((color >> 16 & 0xFF) * factor);
        int g = Math.round((color >> 8 & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return r << 16 | g << 8 | b;
    }
}
