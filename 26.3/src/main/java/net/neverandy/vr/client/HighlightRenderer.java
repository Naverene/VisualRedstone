package net.neverandy.vr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neverandy.vr.Config;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer:
 * <ul>
 * <li>the block they look at gets a bright box when it carries a redstone signal: a pulsing translucent fill, a thick
 * dark border and a bright border on top of it. Stronger signals are brighter.</li>
 * <li>every powered block wired to it gets a thinner box in the path color, so the whole circuit shows.</li>
 * <li>redstone dust and powered blocks nearby show their signal strength, repeaters their delay and comparators their
 * mode, drawn through other blocks.</li>
 * <li>pistons, dispensers and droppers that are quasi-powered but have not updated get a box and a hint.</li>
 * </ul>
 * Colors, outline thickness and range are set in the client config.
 */
@EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Dist.CLIENT)
public class HighlightRenderer {

    private static final float LABEL_SCALE = 0.025F;

    @SubscribeEvent
    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        if (!holdingVisualizer()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getBlockPos();
        BlockState state = event.getBlockState();
        int power = RedstoneScanner.power(level, pos, state);
        if (power <= 0) {
            return;
        }
        VoxelShape found = state.getShape(level, pos, event.getCollisionContext());
        VoxelShape shape = found.isEmpty() ? Shapes.block() : found;
        AABB box = shape.bounds().inflate(0.004);
        Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(event.getCamera().position());

        float strength = 0.55F + 0.45F * power / 15.0F;
        int color = scale(Config.color(Config.HIGHLIGHT_COLOR, 0x40FF40), strength);
        float width = Config.THICKNESS.get().floatValue();
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);
        int fill = argb(0.18F + 0.12F * pulse, color);
        int bright = argb(1.0F, color);
        int dark = argb(0.85F, 0);

        event.addCustomRenderer((renderState, collector, poseStack, levelRenderState) -> {
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> faces(buffer, pose, box, fill));
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), dark, width * 2.0F, false);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), bright, width, false);
            poseStack.popPose();
            // Keep vanilla's thin outline too.
            return false;
        });
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !holdingVisualizer()) {
            return;
        }
        Level level = player.level();
        BlockPos target = null;
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            target = ((BlockHitResult) hit).getBlockPos();
        }
        RedstoneScanner.Result scan = RedstoneScanner.scan(level, player.blockPosition(), target, Config.RANGE.get());

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        float width = Config.THICKNESS.get().floatValue();
        int highlight = Config.color(Config.HIGHLIGHT_COLOR, 0x40FF40);
        int pathColor = Config.color(Config.PATH_COLOR, 0x40C0FF);
        int qcColor = Config.color(Config.QC_COLOR, 0xFFAA00);
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

        if (Config.POWER_PATH.get()) {
            for (BlockPos pos : scan.path) {
                // The looked-at block has its own, brighter box.
                if (!pos.equals(target)) {
                    box(collector, poseStack, level, pos, camera.pos, pathColor, 0.12F, 0.6F, width * 0.6F);
                }
            }
        }
        boolean qc = Config.QC_HINTS.get();
        if (qc) {
            for (RedstoneScanner.Mark mark : scan.marks.values()) {
                if (mark.qc) {
                    box(collector, poseStack, level, mark.pos, camera.pos, qcColor, 0.15F + 0.1F * pulse, 0.85F, width);
                }
            }
        }

        boolean numbers = Config.SIGNAL_NUMBERS.get();
        boolean info = Config.COMPONENT_INFO.get();
        Font font = minecraft.font;
        int background = argb(0.25F, 0);
        for (RedstoneScanner.Mark mark : scan.marks.values()) {
            String number = numbers && (mark.wire || mark.power > 0) ? Integer.toString(mark.power) : null;
            String text = mark.info != null && (mark.qc ? qc : info) ? mark.info : null;
            if (number == null && text == null) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(mark.pos.getX() + 0.5 - camera.pos.x, mark.pos.getY() + (mark.wire ? 0.25 : 0.5) - camera.pos.y,
                mark.pos.getZ() + 0.5 - camera.pos.z);
            poseStack.rotate(camera.orientation);
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            float y = text != null && number != null ? -font.lineHeight : -font.lineHeight / 2.0F;
            if (number != null) {
                int color = mark.power > 0 ? scale(highlight, 0.55F + 0.45F * mark.power / 15.0F) : 0xAAAAAA;
                label(collector, poseStack, font, number, y, color, background);
                y += font.lineHeight;
            }
            if (text != null) {
                label(collector, poseStack, font, text, y, mark.qc ? qcColor : 0xFFFFFF, background);
            }
            poseStack.popPose();
        }
    }

    private static boolean holdingVisualizer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get())
            || player.getOffhandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get()));
    }

    private static void label(SubmitNodeCollector collector, PoseStack poseStack, Font font, String text, float y, int color,
                              int background) {
        collector.submitText(poseStack, -font.width(text) / 2.0F, y, FormattedCharSequence.forward(text, Style.EMPTY), false,
            Font.DisplayMode.SEE_THROUGH, LightCoordsUtil.FULL_BRIGHT, argb(1.0F, color), background, 0);
    }

    /** A translucent fill, a dark border and a colored border on top, around the block's shape. */
    private static void box(SubmitNodeCollector collector, PoseStack poseStack, Level level, BlockPos pos, Vec3 camera,
                            int color, float fillAlpha, float lineAlpha, float width) {
        VoxelShape found = level.getBlockState(pos).getShape(level, pos);
        VoxelShape shape = found.isEmpty() ? Shapes.block() : found;
        AABB box = shape.bounds().inflate(0.004);
        int fill = argb(fillAlpha, color);
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> faces(buffer, pose, box, fill));
        collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), argb(0.85F * lineAlpha, 0), width * 2.0F,
            false);
        collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), argb(lineAlpha, color), width, false);
        poseStack.popPose();
    }

    /** The color with each channel multiplied by the factor. */
    private static int scale(int color, float factor) {
        int r = Math.round((color >> 16 & 0xFF) * factor);
        int g = Math.round((color >> 8 & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return r << 16 | g << 8 | b;
    }

    private static int argb(float alpha, int rgb) {
        return Math.round(alpha * 255) << 24 | (rgb & 0xFFFFFF);
    }

    private static void faces(VertexConsumer buffer, PoseStack.Pose pose, AABB b, int color) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        quad(buffer, pose, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(buffer, pose, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(buffer, pose, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(buffer, pose, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(buffer, pose, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(buffer, pose, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.addVertex(pose, ax, ay, az).setColor(color);
        buffer.addVertex(pose, bx, by, bz).setColor(color);
        buffer.addVertex(pose, cx, cy, cz).setColor(color);
        buffer.addVertex(pose, dx, dy, dz).setColor(color);
    }
}
