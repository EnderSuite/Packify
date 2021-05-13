package com.endersuite.packify;

import com.endersuite.libcore.strfmt.Level;
import com.endersuite.libcore.strfmt.StrFmt;
import com.endersuite.packify.events.PacketReceivedEvent;
import com.endersuite.packify.handlers.CollectablePacketHandler;
import com.endersuite.packify.packets.ACollectableResponsePacket;
import com.endersuite.packify.packets.APacket;
import de.maximilianheidenreich.jeventloop.EventLoop;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.util.MessageBatch;

/**
 * Handles incoming {@link Message}'s and dispatches {@link PacketReceivedEvent}s.
 *
 * @author Maximilian Vincent Heidenreich
 * @since 09.05.21
 */
public class DefaultReceiver implements Receiver {

    // ======================   VARS

    /**
     * The EventLoop used to dispatch {@link PacketReceivedEvent} events to.
     */
    private final EventLoop eventLoop;


    // ======================   CONSTRUCTOR

    public DefaultReceiver(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }


    // ======================   BUSINESS LOGIC

    /**
     * TODO: Add docs -> What changes inside the plugin?
     *
     * @param new_view
     *          The new membership state
     */
    @Override
    public void viewAccepted(View new_view) {
        new StrFmt("{prefix} Cluster members updated: §e" + new_view)
                .setLevel(Level.DEBUG)
                .toLog();
    }

    /**
     * Executes {@link DefaultReceiver#processSingleMessage(Message)} for the received {@link Message}.
     *
     * @param msg
     *          The received message
     */
    @Override
    public void receive(Message msg) {
        processSingleMessage(msg);
    }

    /**
     * Executes {@link DefaultReceiver#processSingleMessage(Message)} for each {@link Message} inside of the batch.
     *
     * @param batch
     *          The received batch
     */
    @Override
    public void receive(MessageBatch batch) {
        batch.stream().forEach(this::processSingleMessage);
    }

    /**
     * Sets the sender & recipient fields and dispatches {@link PacketReceivedEvent}.
     *
     * @param msg
     *          The Message instance from JGroup
     */
    private void processSingleMessage(Message msg) {
        if (msg.getObject() instanceof APacket) {
            APacket packet = (APacket) msg.getObject();
            packet.setSender(msg.getSrc());
            packet.setRecipient(msg.getDest());

            new StrFmt("{prefix} Received packet: " + packet + " in " + (System.currentTimeMillis() - packet.getCreatedAt()) + "ms")
                    .setLevel(Level.DEBUG)
                    .toLog();

            // COLLECT RESPONSE PACKET
            // Cast, add to collect, call callback if every node has responded
            if (packet instanceof ACollectableResponsePacket) {
                CollectablePacketHandler.handleCollectablePacket((ACollectableResponsePacket) packet);
            }

            // DEFAULT PACKET
            else eventLoop.dispatch(new PacketReceivedEvent(packet));

        }
    }

}
