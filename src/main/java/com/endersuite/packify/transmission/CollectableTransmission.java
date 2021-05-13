package com.endersuite.packify.transmission;

import com.endersuite.packify.packets.ACollectableResponsePacket;
import lombok.Getter;
import org.jgroups.Message;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class CollectableTransmission<T extends ACollectableResponsePacket> extends Transmission {

    // ======================   VARS

    // TODO: Add classes
    public static Map<UUID, CollectableTransmission<?>> pendingTransmissions;        // Will get initialized from NetworkManager.getInstance()

    @Getter
    private final UUID transmissionId;

    @Getter
    private int minReplies;

    @Getter
    private Duration timeout;


    // ======================   CONSTRUCTOR

    protected CollectableTransmission(Message message) {
        super(message);
        this.transmissionId = UUID.randomUUID();
        this.minReplies = -1;
        this.timeout = Duration.ZERO;
    }


    // ======================   BUSINESS LOGIC
    // TODO: update state if a node disconnects

    public CollectableTransmission<T> timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    public CompletableFuture<List<T>> collectAll() throws Exception {
        return null;
    }

    public CompletableFuture<List<T>> collectMultiple(int minReplies) throws Exception {
        // TODO: Print warn if minReplies > nodes count
        CompletableFuture<List<T>> callback = new CompletableFuture<>();
        this.minReplies = minReplies;
        transmit();
        return null;
    }

    public CompletableFuture<T> collectOne() throws Exception {
        return null;
    }

    // ======================   HELPERS

    private void startTimeout() {

    }

}
