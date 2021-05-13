package com.endersuite.packify.packets;

import lombok.Getter;
import lombok.Setter;
import org.jgroups.Address;

import java.io.Serializable;
import java.util.UUID;

/**
 * A abstract packet implementation that can be sent over the network.
 *
 * @author Maximilian Vincent Heidenreich
 * @since 09.05.21
 */
@Getter
public abstract class APacket implements Serializable {

    // ======================   VARS

    /**
     * A unique id identifying the packet.
     */
    @Setter
    private final UUID id;

    /**
     * Timestamp at which the packet instance was created.
     */
    private final long createdAt;

    /**
     * The sender of the packet (Only populated when received).
     */
    @Setter
    private Address sender;

    /**
     * The recipient of the packet (Only populated when received).
     */
    @Setter
    private Address recipient;


    // ======================   CONSTRUCTOR

    /**
     * Create a new AbstractPacket with a random id.
     */
    public APacket() {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new AbstractPacket with a specified id.
     *
     * @param id
     *          The id to use.
     */
    public APacket(UUID id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
    }


    // ======================   HELPERS

    /**
     * Returns the string representation of the packet containing the class name, id and sender/receiver if present..
     *
     * @return String representation
     */
    @Override
    public String toString() {
        if (sender != null && recipient != null)
            return String.format("%s(%s)_s(%s)-r(%s)", this.getClass().getSimpleName(), getId().toString().split("-")[0], sender, recipient);
        else
            return String.format("%s(%s)", this.getClass().getSimpleName(), getId().toString().split("-")[0]);
    }


}
