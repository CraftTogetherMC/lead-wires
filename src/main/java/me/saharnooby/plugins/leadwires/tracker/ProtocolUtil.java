package me.saharnooby.plugins.leadwires.tracker;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerAttachEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import lombok.NonNull;
import me.saharnooby.plugins.leadwires.wire.Vector;
import me.saharnooby.plugins.leadwires.wire.Wire;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.entity.Player;

/**
 * @author saharNooby
 * @since 10:52 25.03.2020
 */
public final class ProtocolUtil {

	public static void spawn(@NonNull Player player, @NonNull SentWire sent) {
		Wire wire = sent.getWire();

		spawnEntity(player, sent.getIdA(), TrackerUtil.posA(wire));
		spawnEntity(player, sent.getIdB(), TrackerUtil.posB(wire));

		attachEntities(player, sent.getIdA(), sent.getIdB());
	}

	private static void spawnEntity(@NonNull Player player, int id, @NonNull Vector loc) {
		WrapperEntity wrapper = new WrapperEntity(id, EntityTypes.SILVERFISH);
		wrapper.getEntityMeta().setInvisible(true);
		wrapper.addViewer(player.getUniqueId());

		wrapper.spawn(new Location(loc.getX(), loc.getY(), loc.getZ(), 0, 0));

	}

	private static void attachEntities(@NonNull Player player, int attached, int holder) {
		WrapperPlayServerAttachEntity packet = new WrapperPlayServerAttachEntity(attached, holder, true);
		sendPacket(player, packet);
	}

	public static void despawn(@NonNull Player player, @NonNull SentWire wire) {
		WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(wire.getIdA(), wire.getIdB());
		sendPacket(player, packet);
	}


	private static void sendPacket(@NonNull Player player, @NonNull PacketWrapper<?> packet) {
		try {
			PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
		} catch (Exception e) {
			// ProtocolLib 5.0.0 will NOT throw InvocationTargetException
			throw new RuntimeException("Failed to send packet " + packet.getPacketTypeData().getPacketType() + " to " + player.getName(), e);
		}
	}

}
