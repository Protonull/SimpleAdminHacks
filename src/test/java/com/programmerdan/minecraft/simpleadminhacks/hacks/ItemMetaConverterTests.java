package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.hacks.basic.ItemMetaConverterHack;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.pseudo.PseudoServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vg.civcraft.mc.civmodcore.chat.ChatUtils;
import vg.civcraft.mc.civmodcore.inventory.items.ItemUtils;

public class ItemMetaConverterTests {

	private static final ItemStack TEMPLATE_ITEM = new ItemStack(Material.STICK);

	@BeforeAll
	public static void setupBukkit() {
		PseudoServer.setup();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testLegacyComponent() {
		// Setup
		final var formerItem = TEMPLATE_ITEM.clone();
		ItemUtils.handleItemMeta(formerItem, (ItemMeta meta) -> {
			meta.setDisplayName("Hello!");
			return true;
		});
		final var latterItem = TEMPLATE_ITEM.clone();
		ItemUtils.handleItemMeta(latterItem, (ItemMeta meta) -> {
			meta.displayName(Component.text("Hello!"));
			return true;
		});
		// Process
		ItemMetaConverterHack.processItem(formerItem);
		ItemMetaConverterHack.processItem(latterItem);
		// Check
		final var formerDisplayName = ItemUtils.getComponentDisplayName(formerItem);
		final var latterDisplayName = ItemUtils.getComponentDisplayName(latterItem);
		Assertions.assertTrue(ChatUtils.isBaseComponent(formerDisplayName));
		Assertions.assertTrue(ChatUtils.isBaseComponent(latterDisplayName));
		Assertions.assertTrue(ChatUtils.areComponentsEqual(formerDisplayName, latterDisplayName));
	}

}
