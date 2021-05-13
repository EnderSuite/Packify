package com.endersuite.packify.events;

import com.endersuite.packify.packets.APacket;
import de.maximilianheidenreich.jeventloop.events.AbstractEvent;
import lombok.Getter;
import org.jgroups.Address;

/**
 * Gets dispatched whenever an {@link APacket} was received over the network.
 *
 * @author Maximilian Vincent Heidenreich
 * @since 09.05.21
 */
public class PacketReceivedEvent extends AbstractEvent<Void> {

    // ======================   VARS

    /**
     * The received packet.
     */
    @Getter
    private final APacket packet;

    /**
     * The sender of the packet.
     */
    @Getter
    private final Address sender;

    /**
     * The recipient of the packet.
     */
    @Getter
    private final Address recipient;

    // ======================   CONSTRUCTOR

    public PacketReceivedEvent(APacket packet) {
        super((byte) 1);
        this.packet = packet;
        this.sender = packet.getSender();
        this.recipient = packet.getRecipient();
    }


    // ======================   HELPERS

    /**
     * Returns the string representation of the packet containing the class name and id.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return String.format("%s(%s)-s(%s)-r(%s)", this.getClass().getSimpleName(), getId().toString().split("-")[0], sender, recipient);
    }

}
