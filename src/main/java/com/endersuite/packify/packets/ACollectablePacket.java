package com.endersuite.packify.packets;

import lombok.Getter;

import java.util.UUID;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 13.05.21
 */
public abstract class ACollectablePacket extends APacket {

    public enum Type {
        REQUEST,        // Request type packets follow the default handler pattern
        RESPONSE        // Response type packets will be handled internally by Packify
    }


    // ======================   VARS

    /**
     * The type of the collectable packet (Used by internal handler).
     */
    @Getter
    private final Type type;

    /**
     * A unique id identifying the collection this packet is bound to.
     */
    @Getter
    private final UUID collectionId;


    // ======================   CONSTRUCTOR

    public ACollectablePacket() {
        super();
        this.type = Type.REQUEST;
        this.collectionId = UUID.randomUUID();
    }

    public ACollectablePacket(ACollectablePacket requestPacket) {
        super();
        this.type = Type.RESPONSE;
        this.collectionId = requestPacket.getCollectionId();
    }


    // ======================   BUSINESS LOGIC

    // ======================   HELPERS

    /**
     * Returns the string representation of the packet containing
     * the class name, id, type and sender/receiver if present..
     *
     * @return String representation
     */
    @Override
    public String toString() {
        if (getSender() != null && getRecipient() != null)
            return String.format("%s(%s)_c(%s)-s(%s)-r(%s)", this.getClass().getSimpleName(), getId().toString().split("-")[0], getType().toString(), getSender(), getRecipient());
        else
            return String.format("%s(%s)_c(%s)", this.getClass().getSimpleName(), getId().toString().split("-")[0], getType().toString());
    }

}
