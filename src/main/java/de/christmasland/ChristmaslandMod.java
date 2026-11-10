package de.christmasland;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ChristmaslandMod implements ModInitializer {
	public static final String MOD_ID = "christmasland";

	/** Dimension: data/christmasland/dimension/christmasland.json */
	public static final ResourceKey<Level> CHRISTMASLAND = ResourceKey.create(
			Registries.DIMENSION, Identifier.fromNamespaceAndPath(MOD_ID, "christmasland"));

	public static Block ICE_PORTAL;

	@Override
	public void onInitialize() {
		ResourceKey<Block> key = ResourceKey.create(
				Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "ice_portal"));
		ICE_PORTAL = Registry.register(BuiltInRegistries.BLOCK, key, new IcePortalBlock(
				BlockBehaviour.Properties.of()
						.setId(key)
						.noCollision()
						.noOcclusion()
						.strength(-1.0F)
						.lightLevel(state -> 11)
						.noLootTable()));

		// Feuerzeug auf einen Eisrahmen -> Portal zuenden
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(Items.FLINT_AND_STEEL)) {
				return InteractionResult.PASS;
			}
			if (!PortalFrames.isFrame(level.getBlockState(hit.getBlockPos()))) {
				return InteractionResult.PASS;
			}
			BlockPos inside = hit.getBlockPos().relative(hit.getDirection());
			if (PortalFrames.tryLight(level, inside)) {
				level.playSound(null, inside, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
						1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
				stack.hurtAndBreak(1, player, hand);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		// Rahmenblock abgebaut -> Portal verschwindet
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (PortalFrames.isFrame(state)) {
				PortalFrames.removeConnected(level, pos);
			}
		});

		// Teleport, wenn man im Portal steht
		ServerTickEvents.END_SERVER_TICK.register(PortalTravel::tick);
	}
}
