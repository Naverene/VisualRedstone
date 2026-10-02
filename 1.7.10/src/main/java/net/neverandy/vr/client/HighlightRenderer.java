package net.neverandy.vr.client;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.neverandy.vr.ClientConfig;
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
 * Everything is drawn through other blocks, so it is never hidden. Colors, outline thickness and range are set in
 * config/vr-client.cfg.
 */
public class HighlightRenderer {

    private static final float LABEL_SCALE = 0.025F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.thePlayer;
        if (player == null) {
            return;
        }
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || held.getItem() != VisualRedstone.redstoneVisualizer) {
            return;
        }
        World world = player.worldObj;
        RedstoneScanner.Pos target = null;
        MovingObjectPosition hit = minecraft.objectMouseOver;
        if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            target = new RedstoneScanner.Pos(hit.blockX, hit.blockY, hit.blockZ);
        }
        RedstoneScanner.Pos center = new RedstoneScanner.Pos(
            MathHelper.floor_double(player.posX),
            MathHelper.floor_double(player.posY),
            MathHelper.floor_double(player.posZ));
        RedstoneScanner.Result scan = RedstoneScanner.scan(world, center, target, ClientConfig.range);

        EntityLivingBase viewer = minecraft.renderViewEntity != null ? minecraft.renderViewEntity : player;
        float partialTicks = event.partialTicks;
        double px = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double py = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double pz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        float thickness = ClientConfig.thickness;
        // A slow pulse on the fill draws the eye without hiding the block.
        float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        // Drawn through other blocks, so the boxes are never hidden.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        if (ClientConfig.powerPath) {
            for (RedstoneScanner.Pos pos : scan.path) {
                if (!pos.equals(target)) {
                    box(world, pos, px, py, pz, ClientConfig.pathColor, 0.12F, 0.6F, thickness * 0.6F);
                }
            }
        }
        if (ClientConfig.qcHints) {
            for (RedstoneScanner.Mark mark : scan.marks.values()) {
                if (mark.qc) {
                    box(world, mark.pos, px, py, pz, ClientConfig.qcColor, 0.15F + 0.1F * pulse, 0.85F, thickness);
                }
            }
        }
        if (target != null && world.getBlock(target.x, target.y, target.z)
            .getMaterial() != Material.air) {
            int power = RedstoneScanner.power(world, target.x, target.y, target.z);
            if (power > 0) {
                float strength = 0.55F + 0.45F * power / 15.0F;
                box(
                    world,
                    target,
                    px,
                    py,
                    pz,
                    scale(ClientConfig.highlightColor, strength),
                    0.18F + 0.12F * pulse,
                    1.0F,
                    thickness);
            }
        }
        labels(minecraft, scan, px, py, pz);

        GL11.glDepthMask(true);
        GL11.glPopAttrib();
    }

    private static void labels(Minecraft minecraft, RedstoneScanner.Result scan, double px, double py, double pz) {
        FontRenderer font = minecraft.fontRenderer;
        RenderManager renderManager = RenderManager.instance;
        boolean frontView = minecraft.gameSettings.thirdPersonView == 2;
        for (RedstoneScanner.Mark mark : scan.marks.values()) {
            String number = ClientConfig.signalNumbers && (mark.wire || mark.power > 0) ? Integer.toString(mark.power)
                : null;
            String text = mark.info != null && (mark.qc ? ClientConfig.qcHints : ClientConfig.componentInfo)
                ? mark.info
                : null;
            if (number == null && text == null) {
                continue;
            }
            GL11.glPushMatrix();
            GL11.glTranslated(
                mark.pos.x + 0.5 - px,
                mark.pos.y + (mark.wire ? 0.25 : 0.5) - py,
                mark.pos.z + 0.5 - pz);
            GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef((frontView ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            int y = text != null && number != null ? -font.FONT_HEIGHT : -font.FONT_HEIGHT / 2;
            if (number != null) {
                int color = mark.power > 0 ? scale(ClientConfig.highlightColor, 0.55F + 0.45F * mark.power / 15.0F)
                    : 0xAAAAAA;
                label(font, number, y, color);
                y += font.FONT_HEIGHT;
            }
            if (text != null) {
                label(font, text, y, mark.qc ? ClientConfig.qcColor : 0xFFFFFF);
            }
            GL11.glPopMatrix();
        }
    }

    /** Centered text on a dark backing, like a name tag. */
    private static void label(FontRenderer font, String text, int y, int color) {
        int half = font.getStringWidth(text) / 2;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        t.addVertex(-half - 1, y - 1, 0.0);
        t.addVertex(-half - 1, y + font.FONT_HEIGHT - 1, 0.0);
        t.addVertex(half + 1, y + font.FONT_HEIGHT - 1, 0.0);
        t.addVertex(half + 1, y - 1, 0.0);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.drawString(text, -half, y, 0xFF000000 | color);
    }

    /** A translucent fill, a dark border and a colored border on top, around the block's shape. */
    private static void box(World world, RedstoneScanner.Pos pos, double px, double py, double pz, int color,
        float fillAlpha, float lineAlpha, float thickness) {
        Block block = world.getBlock(pos.x, pos.y, pos.z);
        block.setBlockBoundsBasedOnState(world, pos.x, pos.y, pos.z);
        AxisAlignedBB bounds = block.getSelectedBoundingBoxFromPool(world, pos.x, pos.y, pos.z);
        if (bounds == null) {
            bounds = AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1);
        }
        AxisAlignedBB box = bounds.expand(0.004, 0.004, 0.004)
            .getOffsetBoundingBox(-px, -py, -pz);
        float r = (color >> 16 & 0xFF) / 255.0F, g = (color >> 8 & 0xFF) / 255.0F, b = (color & 0xFF) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(r, g, b, fillAlpha);
        quads(t, box);
        t.draw();

        GL11.glLineWidth(thickness * 2.0F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.85F * lineAlpha);
        edges(t, box);
        t.draw();

        GL11.glLineWidth(thickness);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(r, g, b, lineAlpha);
        edges(t, box);
        t.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** The color with each channel multiplied by the factor. */
    private static int scale(int color, float factor) {
        int r = Math.round((color >> 16 & 0xFF) * factor);
        int g = Math.round((color >> 8 & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return r << 16 | g << 8 | b;
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
