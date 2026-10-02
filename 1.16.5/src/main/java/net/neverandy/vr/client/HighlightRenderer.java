package net.neverandy.vr.client;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || !(player.getHeldItemMainhand().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get()
            || player.getHeldItemOffhand().getItem() == VisualRedstone.REDSTONE_VISUALIZER.get())) {
            return;
        }
        World level = player.world;
        BlockPos pos = event.getTarget().getPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir(level, pos) || !level.getWorldBorder().contains(pos)) {
            return;
        }
        int power = signal(level, pos, state);
        if (power <= 0) {
            return;
        }
        VoxelShape shape = state.getShape(level, pos);
        AxisAlignedBB bounds = shape.isEmpty() ? new AxisAlignedBB(0, 0, 0, 1, 1, 1) : shape.getBoundingBox();
        Vector3d camera = event.getInfo().getProjectedView();
        AxisAlignedBB box = bounds.offset(pos).grow(0.004).offset(-camera.x, -camera.y, -camera.z);
        draw(event.getMatrix(), box, power);
    }

    /** The strongest redstone signal reaching the block, or the power level of redstone dust. */
    private static int signal(World level, BlockPos pos, BlockState state) {
        int power = level.getRedstonePowerFromNeighbors(pos);
        if (state.getBlock() instanceof RedstoneWireBlock) {
            power = Math.max(power, state.get(RedstoneWireBlock.POWER));
        }
        return power;
    }

    private static void draw(MatrixStack poseStack, AxisAlignedBB box, int power) {
        float strength = 0.55F + 0.45F * power / 15.0F;
        float r = 0.25F * strength, g = strength, b = 0.25F * strength;
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);
        Matrix4f matrix = poseStack.getLast().getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        // Drawn through other blocks, so the box is never hidden.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder buffer = tesselator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.faces(buffer, matrix, box, r, g, b, 0.18F + 0.12F * pulse);
        tesselator.draw();

        RenderSystem.lineWidth(7.0F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, 0.0F, 0.0F, 0.0F, 0.85F);
        tesselator.draw();

        RenderSystem.lineWidth(3.5F);
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        BoxShapes.edges(buffer, matrix, box, r, g, b, 1.0F);
        tesselator.draw();

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
