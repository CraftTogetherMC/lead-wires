package me.saharnooby.plugins.leadwires.chunk;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkDataBulk;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import lombok.NonNull;
import me.saharnooby.plugins.leadwires.chunk.event.ChunkSentEvent;
import me.saharnooby.plugins.leadwires.chunk.event.ChunkUnloadSentEvent;
import me.saharnooby.plugins.leadwires.chunk.event.RespawnSentEvent;
import me.saharnooby.plugins.leadwires.util.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * @author saharNooby
 * @since 10:35 25.03.2020
 */
public final class LoadedChunkTracker implements Listener, PacketListener {

    private final Thread mainThread;

    private final Map<Player, Set<ChunkCoord>> loaded = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LoadedChunkTracker(@NonNull Plugin plugin) {
        this.mainThread = Thread.currentThread();

		PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.HIGHEST);

        // Add all online players guessing their loaded chunks.
        // This is needed in order to handle /reload correctly.
        addOnlinePlayers();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        PacketTypeCommon type = event.getPacketType();

        List<ChunkCoord> loaded = null;
        ChunkCoord unloaded = null;

        this.lock.writeLock().lock();
        try {
            Set<ChunkCoord> set = this.loaded.computeIfAbsent(player, k -> new HashSet<>());

            switch (type) {
                case Server.CHUNK_DATA -> {
                    WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
                    ChunkCoord coord = new ChunkCoord(packet.getColumn().getX(), packet.getColumn().getZ());

                    loaded = Collections.singletonList(coord);
                    set.add(coord);

                }
                case Server.MAP_CHUNK_BULK -> {
                    WrapperPlayServerChunkDataBulk packet = new WrapperPlayServerChunkDataBulk(event);
                    int[] xs = packet.getX();
                    int[] zs = packet.getZ();
                    loaded = new ArrayList<>(xs.length);
                    for (int i = 0; i < xs.length; i++) {
                        ChunkCoord coord = new ChunkCoord(xs[i], zs[i]);
                        loaded.add(coord);
                        set.add(coord);
                    }
                }
                case Server.RESPAWN -> set.clear();
                case Server.UNLOAD_CHUNK -> {
                    WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
                    ChunkCoord coord = new ChunkCoord(packet.getChunkX(), packet.getChunkZ());

                    unloaded = coord;
                    set.remove(coord);
                }
                case null, default -> {
                    return;
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }

        boolean isAsync = Thread.currentThread() != this.mainThread;

        if (loaded != null) {
            loaded.forEach(coord -> Bukkit.getPluginManager().callEvent(new ChunkSentEvent(isAsync, player, coord)));
        }

        if (unloaded != null) {
            Bukkit.getPluginManager().callEvent(new ChunkUnloadSentEvent(isAsync, player, unloaded));
        }

        if (type == PacketType.Play.Server.RESPAWN) {
            Bukkit.getPluginManager().callEvent(new RespawnSentEvent(isAsync, player));
        }

    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.lock.writeLock().lock();
        try {
            this.loaded.remove(e.getPlayer());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public Set<ChunkCoord> getLoaded(@NonNull Player player) {
        this.lock.readLock().lock();
        try {
            return new HashSet<>(this.loaded.getOrDefault(player, Collections.emptySet()));
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private void addOnlinePlayers() {
        this.lock.writeLock().lock();
        try {
            int dist = Math.max(1, Bukkit.getViewDistance());

            for (Player player : Bukkit.getOnlinePlayers()) {
                Set<ChunkCoord> set = this.loaded.computeIfAbsent(player, k -> new HashSet<>());

                Chunk center = player.getLocation().getChunk();
                for (int x = -dist; x <= dist; x++) {
                    for (int z = -dist; z <= dist; z++) {
                        set.add(new ChunkCoord(center.getX() + x, center.getZ() + z));
                    }
                }
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

}
