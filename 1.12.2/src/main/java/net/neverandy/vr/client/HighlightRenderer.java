package net.neverandy.vr.client;

import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.neverandy.vr.VisualRedstone;

/**
 * While the player holds a Redstone Visualizer, the block they look at gets a bright green box when it carries a
 * redstone signal: a translucent fill, a thick dark border and a bright border on top of it, drawn through other
 * blocks. Stronger signals are brighter.
 */
@Mod.EventBusSubscriber(modid = VisualRedstone.MOD_ID, value = Side.CLIENT)
public class HighlightRenderer
{
	@SubscribeEvent
	public static void onDrawBlockHighlight(DrawBlockHighlightEvent event)
	{
		EntityPlayer player = event.getPlayer();
		RayTraceResult target = event.getTarget();
		if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK)
		{
			return;
		}
		if (player.getHeldItemMainhand().getItem() != VisualRedstone.redstoneVisualizer
				&& player.getHeldItemOffhand().getItem() != VisualRedstone.redstoneVisualizer)
		{
			return;
		}
		World world = player.world;
		BlockPos pos = target.getBlockPos();
		IBlockState state = world.getBlockState(pos);
		if (state.getMaterial() == Material.AIR || !world.getWorldBorder().contains(pos))
		{
			return;
		}
		int power = signal(world, pos, state);
		if (power <= 0)
		{
			return;
		}

		float partialTicks = event.getPartialTicks();
		double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		AxisAlignedBB box = state.getSelectedBoundingBox(world, pos).grow(0.004).offset(-px, -py, -pz);
		draw(box, power);
	}

	/** The strongest redstone signal reaching the block, or the power level of redstone dust. */
	private static int signal(World world, BlockPos pos, IBlockState state)
	{
		int power = world.isBlockIndirectlyGettingPowered(pos);
		if (state.getBlock() instanceof BlockRedstoneWire)
		{
			power = Math.max(power, state.getValue(BlockRedstoneWire.POWER));
		}
		return power;
	}

	private static void draw(AxisAlignedBB box, int power)
	{
		float strength = 0.55F + 0.45F * power / 15.0F;
		float r = 0.25F * strength, g = strength, b = 0.25F * strength;
		// A slow pulse on the fill draws the eye without hiding the block.
		float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 250.0);

		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.disableAlpha();
		// Drawn through other blocks, so the box is never hidden.
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);

		RenderGlobal.renderFilledBox(box, r, g, b, 0.18F + 0.12F * pulse);
		GlStateManager.glLineWidth(7.0F);
		RenderGlobal.drawSelectionBoundingBox(box, 0.0F, 0.0F, 0.0F, 0.85F);
		GlStateManager.glLineWidth(3.5F);
		RenderGlobal.drawSelectionBoundingBox(box, r, g, b, 1.0F);

		GlStateManager.glLineWidth(1.0F);
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
		GlStateManager.enableCull();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}
}
