package net.neverandy.vr.client;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.DrawHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer, the block they look at gets a bright green box when it carries a
 * redstone signal: a translucent fill, a thick dark border and a bright border on top of it, drawn through other
 * blocks. Stronger signals are brighter.
 */
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Dist.CLIENT)
public class HighlightRenderer {

    @SubscribeEvent
    public static void onHighlightBlock(DrawHighlightEvent.HighlightBlock event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getMainHandItem().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get()
            || player.getOffhandItem().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get())) {
            return;
        }
        Level level = player.level;
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        int power = signal(level, pos, state);
        if (power <= 0) {
            return;
        }
        VoxelShape shape = state.getShape(level, pos);
        AABB bounds = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        Vec3 camera = event.getInfo().getPosition();
        AABB box = bounds.move(pos).inflate(0.004).move(-camera.x, -camera.y, -camera.z);
        draw(event.getMatrix(), box, power);
    }

    /** The strongest redstone signal reaching the block, or the power level of redstone dust. */
    private static int signal(Level level, BlockPos pos, BlockState state) {
        int power = level.getBestNeighborSignal(pos);
        if (state.getBlock() instanceof RedStoneWireBlock) {
            power = Math.max(power, state.getValue(RedStoneWireBlock.POWER));
        }
        return power;
    }

    private static void draw(PoseStack poseStack, AABB box, int power) {
        float strength = 0.55F + 0.45F * power / 15.0F;
        float r = 0.25F * strength, g = strength, b = 0.25F * strength;
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        // Drawn through other blocks, so the box is never hidden.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        BoxShapes.faces(buffer, matrix, box, r, g, b, 0.18F + 0.12F * pulse);
        tesselator.end();

        RenderSystem.lineWidth(7.0F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, 0.0F, 0.0F, 0.0F, 0.85F);
        tesselator.end();

        RenderSystem.lineWidth(3.5F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, r, g, b, 1.0F);
        tesselator.end();

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
