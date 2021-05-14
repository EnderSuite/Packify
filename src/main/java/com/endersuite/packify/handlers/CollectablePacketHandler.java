package com.endersuite.packify.handlers;

import com.endersuite.packify.NetworkManager;
import com.endersuite.packify.packets.ACollectablePacket;
import com.endersuite.packify.packets.APacket;
import com.endersuite.packify.transmission.CompletableTransmission;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 11.05.21
 */
public class CollectablePacketHandler {

    // ======================   VARS

    @Getter
    private final NetworkManager networkManager;

    @Getter
    private final Map<UUID, CompletableTransmission> pendingTransmissions;

    /**
     * Store all received collection packets.
     * -> Will be passed into a collectionCallback once all nodes have responded / timeout.
     */
    @Getter
    private final Map<UUID, List<ACollectablePacket>> collectionPackets;

    /**
     * Stores response packets after a request has been sent using {@link NetworkManager#broadcastCollect(ACollectableRequestPacket)}.
     * -> After all packets have been received call callback identified by same id.
     */
    @Getter
    private final Map<UUID, List<APacket>> broadcastResponseCollections;


    public CollectablePacketHandler(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.pendingTransmissions = new ConcurrentHashMap<>();
        this.collectionPackets = new ConcurrentHashMap<>();
        this.broadcastResponseCollections = new ConcurrentHashMap<>();

        networkManager.getScheduler().schedule(this::completeCompletableTransmissions, 3, TimeUnit.MINUTES);
    }

    // ======================   BUSINESS LOGIC

    public void handleCollectablePacket(ACollectablePacket packet) {

        // RET: No pending transmission or the received collection id
        if (!getPendingTransmissions().containsKey(packet.getCollectionId())) return;
        CompletableTransmission transmission = getPendingTransmissions().get(packet.getCollectionId());

        transmission.addResponsePacket(packet);

        // RET: Waiting for more response packets
        if (!transmission.isCompletable()) return;

        getPendingTransmissions().remove(transmission.getCollectionId());
        transmission.complete();

    }
    // TODO: Synchronize on collection & add method to add pending transmission
    // TODO switch to event dispatch -> handler calls complete

    public void completeCompletableTransmissions() {
        for (Iterator<CompletableTransmission> iterator = getPendingTransmissions().values().iterator(); getPendingTransmissions().values().iterator().hasNext(); ) {
            CompletableTransmission transmission = iterator.next();
            if (!transmission.isCompletable()) continue;

            iterator.remove();
            transmission.complete();
        }
    }

}
