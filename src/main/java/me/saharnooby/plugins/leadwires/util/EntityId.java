package me.saharnooby.plugins.leadwires.util;

import com.github.retrooper.packetevents.PacketEvents;
import me.saharnooby.plugins.leadwires.LeadWires;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import me.tofaa.entitylib.EntityLib;

/**
 * @author saharNooby
 * @since 20:46 24.03.2020
 */
public final class EntityId {

	private static final AtomicInteger nextId = new AtomicInteger(20_000_000);

	private static boolean errorPrinted;

	public static int next() {
		try {
			return nextId.incrementAndGet();
		} catch (Exception e) {
			if (!errorPrinted) {
				LeadWires.getInstance().getLogger().log(Level.SEVERE, "Failed to generate entity ID", e);
				errorPrinted = true;
			}
			return -1;
		}
	}

}
