package net.neverandy.vr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer, the block they look at gets a bright green box when it carries a
 * redstone signal: a translucent fill, a thick dark border and a bright border on top of it. Stronger signals are
 * brighter.
 */
@EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Dist.CLIENT)
public class HighlightRenderer {

    @SubscribeEvent
    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get())
            || player.getOffhandItem().is(VisualRedstone.REDSTONE_VISUALIZER.get()))) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getBlockPos();
        BlockState state = event.getBlockState();
        int power = signal(level, pos, state);
        if (power <= 0) {
            return;
        }
        VoxelShape found = state.getShape(level, pos, event.getCollisionContext());
        VoxelShape shape = found.isEmpty() ? Shapes.block() : found;
        AABB box = shape.bounds().inflate(0.004);
        Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(event.getCamera().position());

        float strength = 0.55F + 0.45F * power / 15.0F;
        int bright = color(0.25F * strength, strength, 0.25F * strength, 1.0F);
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);
        int fill = color(0.25F * strength, strength, 0.25F * strength, 0.18F + 0.12F * pulse);
        int dark = color(0.0F, 0.0F, 0.0F, 0.85F);

        event.addCustomRenderer((renderState, collector, poseStack, levelRenderState) -> {
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(),
                (pose, buffer) -> faces(buffer, pose, box, fill));
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), dark, 7.0F, false);
            collector.submitShapeOutline(poseStack, shape, RenderTypes.linesTranslucent(), bright, 3.5F, false);
            poseStack.popPose();
            // Keep vanilla's thin outline too.
            return false;
        });
    }

    /** The strongest redstone signal reaching the block, or the power level of redstone dust. */
    private static int signal(Level level, BlockPos pos, BlockState state) {
        int power = level.getBestNeighborSignal(pos);
        if (state.getBlock() instanceof RedStoneWireBlock) {
            power = Math.max(power, state.getValue(RedStoneWireBlock.POWER));
        }
        return power;
    }

    private static int color(float r, float g, float b, float a) {
        return (Math.round(a * 255) << 24) | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
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
