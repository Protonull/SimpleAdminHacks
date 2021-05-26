package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacksConfig;
import com.programmerdan.minecraft.simpleadminhacks.configs.CTAnnounceConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.utilities.BroadcastLevel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minelink.ctplus.event.PlayerCombatTagEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CTAnnounceTests {

	public CTAnnounce hackInstance;
	public CTAnnounceConfig hackConfig;
	public SimpleAdminHacks pluginInstance;
	public SimpleAdminHacksConfig pluginConfig;

	@BeforeEach
	public void setUp() throws Exception {
		this.hackConfig = Mockito.mock(CTAnnounceConfig.class);
		this.pluginInstance = Mockito.mock(SimpleAdminHacks.class);
		this.pluginConfig = Mockito.mock(SimpleAdminHacksConfig.class);
		Mockito.when(this.pluginInstance.serverHasPlugin("CombatTagPlus")).thenReturn(true);
		Mockito.when(this.pluginInstance.config()).thenReturn(this.pluginConfig);
		Mockito.when(this.pluginConfig.getBroadcastPermission()).thenReturn("simpleadmin.broadcast");
	}

	@AfterEach
	public void tearDown() throws Exception {
		pluginInstance = null;
		hackConfig = null;
		hackInstance = null;
	}

	@Test
	public void testStatus() {
		this.hackInstance = new CTAnnounce(this.pluginInstance, this.hackConfig);
		Mockito.when(this.hackConfig.isEnabled()).thenReturn(true, false);
		Assertions.assertEquals("CombatTagPlus.PlayerCombatTagEvent monitoring active", this.hackInstance.status());
		Assertions.assertEquals("CombatTagPlus.PlayerCombatTagEvent monitoring not active", this.hackInstance.status());
	}

	@Test
	public void testCTEventQuickFail() {
		this.hackInstance = new CTAnnounce(this.pluginInstance, this.hackConfig);
		Mockito.when(this.hackConfig.isEnabled()).thenReturn(true);
		final var event = new PlayerCombatTagEvent(null, null, 30);
		try {
			this.hackInstance.ctEvent(event);
			Assertions.assertTrue(true);
		}
		catch(final NullPointerException npe) {
			Assertions.fail("Check failed to prevent NPE by fast-failing on null attacker/victim.");
		}
	}

	private List<BroadcastLevel> allLevels() {
		return List.of(BroadcastLevel.values());
	}

	private Set<OfflinePlayer> fakeOperators() {
		final var operators = new HashSet<OfflinePlayer>(1);
		final var operator = Mockito.mock(OfflinePlayer.class);
		Mockito.when(operator.isOnline()).thenReturn(true);
		operators.add(operator);
		return operators;
	}

	/**
	 * If I've done this right, should validate that all broadcast types (minus the general broadcast
	 *   method -- I'm not testing that Bukkit works, here) function, that CTAnnounce.cleanMessage()
	 *   works, and that throttling works.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testCTEvent() {
		this.hackInstance = new CTAnnounce(this.pluginInstance, this.hackConfig);
		this.hackInstance.dataBootstrap();

		Mockito.when(this.hackConfig.isEnabled()).thenReturn(true);
		Mockito.when(this.hackConfig.getBroadcastDelay()).thenReturn(250L);
		Mockito.when(this.hackConfig.getBroadcast()).thenReturn(allLevels());
		Mockito.when(this.hackConfig.getBroadcastMessage()).thenReturn("%Victim% struck by %Attacker%");

		final Set<OfflinePlayer> operators = fakeOperators();
		for (final OfflinePlayer operator : fakeOperators()) {
			final var softOperator = Mockito.mock(SoftPlayer.class);
			Mockito.when(operator.getPlayer()).thenReturn(softOperator);
		}
		Mockito.when(this.pluginInstance.serverOperatorBroadcast(Mockito.anyString())).thenReturn(operators.size());
		Mockito.when(this.pluginInstance.serverBroadcast(Mockito.anyString())).thenReturn(4);

		/** This doubles as a check on hidden method {@link CTAnnounce#cleanMessage()} */
		Mockito.doAnswer(invocation -> {
			String toSend = invocation.getArgument(0, String.class);
			Assertions.assertEquals("Victim struck by Attacker", toSend);
			return null;
		}).when(this.pluginInstance).serverSendConsoleMessage(Mockito.anyString());

		final var victim = Mockito.mock(SoftPlayer.class);
		final var victimName = "Victim";
		Mockito.when(victim.getName()).thenReturn(victimName);
		Mockito.when(victim.getDisplayName()).thenReturn(victimName);
		final var victimUUID = UUID.randomUUID();
		Mockito.when(victim.getUniqueId()).thenReturn(victimUUID);
		Mockito.when(victim.isOnline()).thenReturn(true);

		final var attacker = Mockito.mock(SoftPlayer.class);
		final var attackerName = "Attacker";
		Mockito.when(attacker.getName()).thenReturn(attackerName);
		Mockito.when(attacker.getDisplayName()).thenReturn(attackerName);
		final var attackerUUID = UUID.randomUUID();
		Mockito.when(attacker.getUniqueId()).thenReturn(attackerUUID);
		Mockito.when(attacker.isOnline()).thenReturn(true);

		final var cte = new PlayerCombatTagEvent(victim, attacker, 30);

		Mockito.when(this.pluginInstance.serverOnlineBroadcast(Mockito.anyString())).thenReturn(2);

		this.hackInstance.ctEvent(cte);
		// Now we make sure everyone got notified, and only once.

		// OPs got notified
		Mockito.verify(this.pluginInstance).serverOperatorBroadcast(Mockito.anyString());
		// Console got notified
		Mockito.verify(this.pluginInstance).serverSendConsoleMessage(Mockito.anyString());
		// All Players got notified
		Mockito.verify(this.pluginInstance).serverOnlineBroadcast(Mockito.anyString());
		// Broadcast holders got notified
		Mockito.verify(this.pluginInstance).serverBroadcast(Mockito.anyString());

		try {
			Thread.sleep(10L);
		}
		catch (final InterruptedException ignored) { }

		// This one should get throttled right away.
		this.hackInstance.ctEvent(cte);

		// verify that console was _not_ alerted again (e.g. still only one message)
		Mockito.verify(this.pluginInstance).serverSendConsoleMessage(Mockito.anyString());

		try {
			Thread.sleep(400L);
		}
		catch (final InterruptedException ignored) { }

		// This one should not get throttled.
		this.hackInstance.ctEvent(cte);

		// verify that console was alerted again (e.g. second throttled, third succeeded)
		Mockito.verify(this.pluginInstance, Mockito.times(2)).serverSendConsoleMessage(Mockito.anyString());
	}

	interface SoftPlayer extends Player {}
}
