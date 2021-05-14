package com.endersuite.packify;

import com.endersuite.libcore.strfmt.Level;
import com.endersuite.libcore.strfmt.StrFmt;
import com.endersuite.packify.events.PacketReceivedEvent;
import com.endersuite.packify.handlers.CollectablePacketHandler;
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
    private final CollectablePacketHandler collectablePacketHandler;

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
        this.collectablePacketHandler = new CollectablePacketHandler(this);
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
     * Sends a message over the active JChannel.
     *
     * @param message
     *          The message to send
     * @throws Exception
     */
    public void sendRaw(Message message) throws Exception {
        new StrFmt("{prefix} Sending: " + message.getObject()).setLevel(Level.TRACE).toLog();
        getJChannel().send(message);
    }

}
