package com.endersuite.packify.packets;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 11.05.21
 */
public abstract class ACollectableRequestPacket<P extends ACollectableResponsePacket> extends APacket {

    // ======================   VARS

    /**
     * A unique id identifying the collection this packet is bound to.
     */
    @Getter
    @Setter
    private UUID collectId;


    // ======================   CONSTRUCTOR

    public ACollectableRequestPacket() {
        super();
        this.collectId = UUID.randomUUID();
    }

    // ======================   BUSINESS LOGIC

    // ======================   HELPERS

}
