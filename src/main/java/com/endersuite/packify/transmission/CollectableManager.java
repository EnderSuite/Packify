package com.endersuite.packify.transmission;

import com.endersuite.packify.NetworkManager;
import com.endersuite.packify.packets.ACollectablePacket;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages {@link ACollectablePacket}s and pending transmissions.
 *
 * @author Maximilian Vincent Heidenreich
 * @since 11.05.21
 */
public class CollectableManager {

    // ======================   VARS

    @Getter
    private final NetworkManager networkManager;

    /**
     * Stores CompletableTransmissions that are not completable yet (-> waiting for more response packets).
     */
    @Getter
    private final Map<UUID, CompletableTransmission> pendingTransmissions;


    // ======================   CONSTRUCTOR

    public CollectableManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.pendingTransmissions = new ConcurrentHashMap<>();
    }

    // ======================   BUSINESS LOGIC

    /**
     * Handles a collectable packet by adding it to matching pending transmissions and
     * completing them if they are completable.
     *
     * @param packet
     *          The packet to handle
     */
    public void handleCollectablePacket(ACollectablePacket packet) {

        // RET: No pending transmission or the received collection id
        if (!getPendingTransmissions().containsKey(packet.getCollectionId())) return;

        CompletableTransmission transmission = getPendingTransmissions().get(packet.getCollectionId());
        transmission.addResponsePacket(packet);

        // RET: Waiting for more response packets
        if (!transmission.isCompletable()) return;

        transmission.complete();
    }

    /**
     * Iterates through all pending transmissions and completes them if they are completable.
     * <br><br><i>Note: Called after cluster changed. This prevents transmissions from never completing
     * due to mismatch from node count when created ({@link Transmission.TransmissionBuilder#collectAll()}) and now.</i>
     */
    public void completeCompletableTransmissions() {
        for (Iterator<CompletableTransmission> iterator = getPendingTransmissions().values().iterator(); getPendingTransmissions().values().iterator().hasNext(); ) {
            CompletableTransmission transmission = iterator.next();
            if (!transmission.isCompletable()) continue;

            iterator.remove();
            transmission.complete();
        }
    }

}
