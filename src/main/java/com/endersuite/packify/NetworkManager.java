package com.endersuite.packify;

import com.endersuite.libcore.strfmt.Level;
import com.endersuite.libcore.strfmt.StrFmt;
import com.endersuite.packify.events.PacketReceivedEvent;
import com.endersuite.packify.transmission.CollectableManager;
import com.endersuite.packify.packets.ACollectablePacket;
import com.endersuite.packify.packets.APacket;
import com.endersuite.packify.transmission.Transmission;
import de.maximilianheidenreich.jeventloop.EventLoop;
import lombok.Getter;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 09.05.21
 */
public class NetworkManager extends APacketDelegator {

    // ======================   VARS

    /**
     * The configured {@link JChannel} instance.
     */
    @Getter
    private final JChannel jChannel;

    @Getter
    private final CollectableManager collectableManager;

    @Getter
    private final ScheduledExecutorService scheduler;


    // ======================   CONSTRUCTOR

    public NetworkManager(EventLoop eventLoop, String nodeName) throws Exception {
        super(eventLoop);

        eventLoop.removeEventHandler(PacketReceivedEvent.class, this::handlePacketReceivedEvent);   // Make sure packets are not handled twice or more times
        eventLoop.addEventHandler(PacketReceivedEvent.class, this::handlePacketReceivedEvent);

        Transmission.setDefaultNetworkManager(this);
        this.jChannel = new JChannel();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.jChannel.setReceiver(new DefaultReceiver(this));
        this.collectableManager = new CollectableManager(this);
        //this.jChannel.setDiscardOwnMessages(true);

        if ("default".equalsIgnoreCase(nodeName))
            nodeName = UUID.randomUUID().toString().split("-")[0];

        getJChannel().name(nodeName);

    }


    // ======================   BUSINESS LOGIC

    /**
     * Connects to an existing cluster or creates a new one if none exist already.
     *
     * @param clusterName
     *          The cluster name
     * @throws Exception
     */
    public void connect(String clusterName) throws Exception {
        getJChannel().connect(clusterName);
    }

    /**
     * Closes the connection to the cluster.
     */
    public void disconnect() {
        getJChannel().close();
    }

    // ======================   HELPERS

    /**
     * Returns the current number of nodes in the cluster.
     *
     * @return
     */
    public int getNodeCount() {
        return getJChannel().getView().getMembers().size();
    }

    /**
     * Sends a message over the active JChannel.
     *
     * @param message
     *          The message to send
     * @throws Exception
     */
    public void sendRaw(Message message) throws Exception {
        new StrFmt("{prefix} Sending: ", message.getObject().toString()).setLevel(Level.TRACE).toLog();
        getJChannel().send(message);
    }


    // ======================   EVENT HANDLERS

    @Override
    public void handlePacketReceivedEvent(PacketReceivedEvent event) {
        APacket packet = event.getPacket();

        if (packet instanceof ACollectablePacket && ((ACollectablePacket) packet).getType().equals(ACollectablePacket.Type.RESPONSE)) {
            try { getCollectableManager().handleCollectablePacket((ACollectablePacket) packet); }
            catch (Exception e) {
                e.printStackTrace();
                new StrFmt("{prefix} A handle threw an error for collectable " + packet + "!", e)
                        .setLevel(Level.ERROR).toLog();
            }
            return;
        }

        super.handlePacketReceivedEvent(event);
    }

}
