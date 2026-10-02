package net.neverandy.vr.client;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
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
 * Everything is drawn through other blocks, so it is never hidden. Colors, outline thickness and range are set in the
 * client config.
 */
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Side.CLIENT)
public class HighlightRenderer
{
	private static final float LABEL_SCALE = 0.025F;

	@SubscribeEvent
	public static void onRenderWorldLast(RenderWorldLastEvent event)
	{
		Minecraft minecraft = Minecraft.getMinecraft();
		EntityPlayer player = minecraft.player;
		if (player == null || player.getHeldItemMainhand().getItem() != VisualRedstone.redstoneVisualizer
				&& player.getHeldItemOffhand().getItem() != VisualRedstone.redstoneVisualizer)
		{
			return;
		}
		World world = player.world;
		BlockPos target = null;
		RayTraceResult hit = minecraft.objectMouseOver;
		if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK)
		{
			target = hit.getBlockPos();
		}
		RedstoneScanner.Result scan = RedstoneScanner.scan(world, new BlockPos(player), target, ClientConfig.range);

		Entity viewer = minecraft.getRenderViewEntity() != null ? minecraft.getRenderViewEntity() : player;
		float partialTicks = event.getPartialTicks();
		double px = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
		double py = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
		double pz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
		float thickness = (float) ClientConfig.thickness;
		int highlight = ClientConfig.color(ClientConfig.highlightColor, 0x40FF40);
		int pathColor = ClientConfig.color(ClientConfig.pathColor, 0x40C0FF);
		int qcColor = ClientConfig.color(ClientConfig.qcColor, 0xFFAA00);
		// A slow pulse on the fill draws the eye without hiding the block.
		float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

		begin();
		if (ClientConfig.powerPath)
		{
			for (BlockPos pos : scan.path)
			{
				if (!pos.equals(target))
				{
					box(world, pos, px, py, pz, pathColor, 0.12F, 0.6F, thickness * 0.6F);
				}
			}
		}
		if (ClientConfig.qcHints)
		{
			for (RedstoneScanner.Mark mark : scan.marks.values())
			{
				if (mark.qc)
				{
					box(world, mark.pos, px, py, pz, qcColor, 0.15F + 0.1F * pulse, 0.85F, thickness);
				}
			}
		}
		if (target != null && world.getWorldBorder().contains(target))
		{
			IBlockState state = world.getBlockState(target);
			int power = state.getMaterial() == Material.AIR ? 0 : RedstoneScanner.power(world, target, state);
			if (power > 0)
			{
				float strength = 0.55F + 0.45F * power / 15.0F;
				box(world, target, px, py, pz, scale(highlight, strength), 0.18F + 0.12F * pulse, 1.0F, thickness);
			}
		}
		labels(minecraft, scan, px, py, pz, highlight, qcColor);
		end();
	}

	private static void labels(Minecraft minecraft, RedstoneScanner.Result scan, double px, double py, double pz,
			int highlight, int qcColor)
	{
		FontRenderer font = minecraft.fontRenderer;
		RenderManager renderManager = minecraft.getRenderManager();
		boolean frontView = minecraft.gameSettings.thirdPersonView == 2;
		for (RedstoneScanner.Mark mark : scan.marks.values())
		{
			String number = ClientConfig.signalNumbers && (mark.wire || mark.power > 0) ? Integer.toString(mark.power) : null;
			String text = mark.info != null && (mark.qc ? ClientConfig.qcHints : ClientConfig.componentInfo) ? mark.info : null;
			if (number == null && text == null)
			{
				continue;
			}
			GlStateManager.pushMatrix();
			GlStateManager.translate(mark.pos.getX() + 0.5 - px, mark.pos.getY() + (mark.wire ? 0.25 : 0.5) - py,
					mark.pos.getZ() + 0.5 - pz);
			GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate((frontView ? -1 : 1) * renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
			GlStateManager.scale(-LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
			float y = text != null && number != null ? -font.FONT_HEIGHT : -font.FONT_HEIGHT / 2.0F;
			if (number != null)
			{
				int color = mark.power > 0 ? scale(highlight, 0.55F + 0.45F * mark.power / 15.0F) : 0xAAAAAA;
				label(font, number, y, color);
				y += font.FONT_HEIGHT;
			}
			if (text != null)
			{
				label(font, text, y, mark.qc ? qcColor : 0xFFFFFF);
			}
			GlStateManager.popMatrix();
		}
	}

	/** Centered text on a dark backing, like a name tag. */
	private static void label(FontRenderer font, String text, float y, int color)
	{
		float half = font.getStringWidth(text) / 2.0F;
		GlStateManager.disableTexture2D();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(-half - 1, y - 1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
		buffer.pos(-half - 1, y + font.FONT_HEIGHT - 1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
		buffer.pos(half + 1, y + font.FONT_HEIGHT - 1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
		buffer.pos(half + 1, y - 1, 0.0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
		tessellator.draw();
		GlStateManager.enableTexture2D();
		font.drawString(text, -half, y, 0xFF000000 | color, false);
	}

	private static void begin()
	{
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		// Drawn through other blocks, so the boxes are never hidden.
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
	}

	private static void end()
	{
		GlStateManager.glLineWidth(1.0F);
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	/** A translucent fill, a dark border and a colored border on top, around the block's shape. */
	private static void box(World world, BlockPos pos, double px, double py, double pz, int color, float fillAlpha,
			float lineAlpha, float thickness)
	{
		IBlockState state = world.getBlockState(pos);
		AxisAlignedBB box = state.getSelectedBoundingBox(world, pos).grow(0.004).offset(-px, -py, -pz);
		float r = (color >> 16 & 0xFF) / 255.0F, g = (color >> 8 & 0xFF) / 255.0F, b = (color & 0xFF) / 255.0F;

		GlStateManager.disableTexture2D();
		RenderGlobal.renderFilledBox(box, r, g, b, fillAlpha);
		GlStateManager.glLineWidth(thickness * 2.0F);
		RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.85F * lineAlpha);
		GlStateManager.glLineWidth(thickness);
		RenderGlobal.drawSelectionBoundingBox(box, r, g, b, lineAlpha);
	}

	/** The color with each channel multiplied by the factor. */
	private static int scale(int color, float factor)
	{
		int r = Math.round((color >> 16 & 0xFF) * factor);
		int g = Math.round((color >> 8 & 0xFF) * factor);
		int b = Math.round((color & 0xFF) * factor);
		return r << 16 | g << 8 | b;
	}
}
