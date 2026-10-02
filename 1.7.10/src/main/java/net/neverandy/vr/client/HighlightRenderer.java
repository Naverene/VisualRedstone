package net.neverandy.vr.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer, the block they look at gets a bright green box when it carries a
 * redstone signal: a translucent fill, a thick dark border and a bright border on top of it, drawn through other
 * blocks. Stronger signals are brighter.
 */
public class HighlightRenderer {

    @SubscribeEvent
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        EntityPlayer player = event.player;
        MovingObjectPosition target = event.target;
        if (target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || held.getItem() != VisualRedstone.redstoneVisualizer) {
            return;
        }
        World world = player.worldObj;
        int x = target.blockX, y = target.blockY, z = target.blockZ;
        if (world.isAirBlock(x, y, z)) {
            return;
        }
        int power = signal(world, x, y, z);
        if (power <= 0) {
            return;
        }

        Block block = world.getBlock(x, y, z);
        block.setBlockBoundsBasedOnState(world, x, y, z);
        AxisAlignedBB bounds = block.getSelectedBoundingBoxFromPool(world, x, y, z);
        if (bounds == null) {
            bounds = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
        }
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks;
        AxisAlignedBB box = bounds.expand(0.004, 0.004, 0.004).getOffsetBoundingBox(-px, -py, -pz);
        draw(box, power);
    }

    /** The strongest redstone signal reaching the block, or the power level of redstone dust. */
    private static int signal(World world, int x, int y, int z) {
        int power = world.getStrongestIndirectPower(x, y, z);
        if (world.getBlock(x, y, z) == Blocks.redstone_wire) {
            power = Math.max(power, world.getBlockMetadata(x, y, z));
        }
        return power;
    }

    private static void draw(AxisAlignedBB box, int power) {
        float strength = 0.55F + 0.45F * power / 15.0F;
        float r = 0.25F * strength, g = strength, b = 0.25F * strength;
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        // Drawn through other blocks, so the box is never hidden.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, 0.18F + 0.12F * pulse);
        quads(t, box);
        t.draw();

        GL11.glLineWidth(7.0F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.85F);
        edges(t, box);
        t.draw();

        GL11.glLineWidth(3.5F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(r, g, b, 1.0F);
        edges(t, box);
        t.draw();

        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }

    private static void quads(Tessellator t, AxisAlignedBB b) {
        t.addVertex(b.minX, b.minY, b.minZ);
        t.addVertex(b.maxX, b.minY, b.minZ);
        t.addVertex(b.maxX, b.minY, b.maxZ);
        t.addVertex(b.minX, b.minY, b.maxZ);

        t.addVertex(b.minX, b.maxY, b.minZ);
        t.addVertex(b.minX, b.maxY, b.maxZ);
        t.addVertex(b.maxX, b.maxY, b.maxZ);
        t.addVertex(b.maxX, b.maxY, b.minZ);

        t.addVertex(b.minX, b.minY, b.minZ);
        t.addVertex(b.minX, b.maxY, b.minZ);
        t.addVertex(b.maxX, b.maxY, b.minZ);
        t.addVertex(b.maxX, b.minY, b.minZ);

        t.addVertex(b.minX, b.minY, b.maxZ);
        t.addVertex(b.maxX, b.minY, b.maxZ);
        t.addVertex(b.maxX, b.maxY, b.maxZ);
        t.addVertex(b.minX, b.maxY, b.maxZ);

        t.addVertex(b.minX, b.minY, b.minZ);
        t.addVertex(b.minX, b.minY, b.maxZ);
        t.addVertex(b.minX, b.maxY, b.maxZ);
        t.addVertex(b.minX, b.maxY, b.minZ);

        t.addVertex(b.maxX, b.minY, b.minZ);
        t.addVertex(b.maxX, b.maxY, b.minZ);
        t.addVertex(b.maxX, b.maxY, b.maxZ);
        t.addVertex(b.maxX, b.minY, b.maxZ);
    }

    private static void edges(Tessellator t, AxisAlignedBB b) {
        double[] xs = { b.minX, b.maxX }, ys = { b.minY, b.maxY }, zs = { b.minZ, b.maxZ };
        for (double y : ys) {
            for (double z : zs) {
                t.addVertex(b.minX, y, z);
                t.addVertex(b.maxX, y, z);
            }
        }
        for (double x : xs) {
            for (double z : zs) {
                t.addVertex(x, b.minY, z);
                t.addVertex(x, b.maxY, z);
            }
        }
        for (double x : xs) {
            for (double y : ys) {
                t.addVertex(x, y, b.minZ);
                t.addVertex(x, y, b.maxZ);
            }
        }
    }
}
