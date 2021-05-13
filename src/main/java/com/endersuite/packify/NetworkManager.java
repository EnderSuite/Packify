package com.endersuite.packify;

import com.endersuite.packify.events.PacketReceivedEvent;
import com.endersuite.packify.transmission.CollectableTransmission;
import de.maximilianheidenreich.jeventloop.EventLoop;
import lombok.Getter;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 09.05.21
 */
public class NetworkManager extends APacketDelegator {

    // ======================   VARS

    /**
     * The NetworkManager singleton.
     */
    private static NetworkManager instance;

    /**
     * The configured {@link JChannel} instance.
     */
    @Getter
    private final JChannel jChannel;

    //private Map<UUID, >


    // ======================   CONSTRUCTOR

    private NetworkManager(EventLoop eventLoop, String nodeName) throws Exception {
        super(eventLoop);

        eventLoop.removeEventHandler(PacketReceivedEvent.class, this::handlePacketReceivedEvent);   // Make sure packets are not handled twice or more times
        eventLoop.addEventHandler(PacketReceivedEvent.class, this::handlePacketReceivedEvent);

        this.jChannel = new JChannel();
        this.jChannel.setReceiver(new DefaultReceiver(eventLoop));
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
     * Sends a message over the active JChannel..
     *
     * @param message
     *          The message to send
     * @throws Exception
     */
    public void sendRaw(Message message) throws Exception {
        getJChannel().send(message);
    }


/*
    public void broadcastRaw(APacket packet, boolean loopback) throws Exception {
        Message message = new ObjectMessage(null, packet);
        if (!loopback)
            message.setFlag(Message.TransientFlag.DONT_LOOPBACK);
        sendRaw(message);
    }

    public <T extends ACollectableResponsePacket> CompletableFuture<List<? extends ACollectableResponsePacket>> broadcastCollect(ACollectableRequestPacket<T> packet, boolean loopback) throws Exception {
        CompletableFuture<List<? extends ACollectableResponsePacket>> callback = new CompletableFuture<>();
        CollectablePacketHandler.getCollectionCallbacks().put(packet.getCollectId(), callback);
        broadcastRaw(packet, loopback);
        return callback;
    }*/


    // ======================   GETTER & SETTER

    /**
     * Returns the NetworkManager singleton instance.
     * Note: Also creates one if none exists.
     *
     * @return The NetworkManager instance.
     */
    public static NetworkManager getInstance() {
        return NetworkManager.instance;

        /*try {
            NetworkManager.instance = new NetworkManager();

            // Reset stuff -> We do this because something like a server reload or somebody
            // could fuck it up and we would have dangling callbacks, packets etc.
            // Also, we need to initialize these utility classes!
            //CollectableTransmission.pendingTransmissions = new ConcurrentHashMap<>();

            return NetworkManager.instance;
        } catch (Exception exception) {
            exception.printStackTrace();
            new StrFmt("{prefix} Could not initialize JGroup networking! Database fallback will be used! This might affect performance!").setLevel(Level.ERROR).toConsole();
            return null;
        }*/

    }

    public static NetworkManager initialize(EventLoop eventloop, String nodeName) throws Exception {
        // Reset stuff -> We do this because something like a server reload or somebody
        // could fuck it up and we would have dangling callbacks, packets etc.
        // Also, we need to initialize these utility classes!
        CollectableTransmission.pendingTransmissions = new ConcurrentHashMap<>();

        return new NetworkManager(eventloop, nodeName);
    }

}
