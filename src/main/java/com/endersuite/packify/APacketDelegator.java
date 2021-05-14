package com.endersuite.packify;

import com.endersuite.libcore.strfmt.Level;
import com.endersuite.libcore.strfmt.StrFmt;
import com.endersuite.packify.events.PacketReceivedEvent;
import com.endersuite.packify.packets.APacket;
import de.maximilianheidenreich.jeventloop.EventLoop;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A wrapper class that stores registered packet handlers & callbacks.
 */
@Getter
public abstract class APacketDelegator {

    // ======================   VARS

    /**
     * A queue containing unhandled packets.
     */
    private final BlockingQueue<APacket> packetQueue;

    /**
     * All registered handlers which will be executed if an event with matching class is dequeued.
     */
    private final Map<Class<? extends APacket>, List<Consumer<? extends APacket>>> handlers;


    /**
     * A reference to an event loop that gets used for packet handling.
     */
    @Setter
    private EventLoop eventLoop;


    // ======================   CONSTRUCTOR

    /**
     * Creates a new AbstractPacketManager with a default singleThreadExecutor and new default EventLoop.
     */
    public APacketDelegator() {
        this(new EventLoop());
    }

    /**
     * Creates a new AbstractPacketManager with a custom executor.
     */
    public APacketDelegator(EventLoop eventLoop) {
        this.packetQueue = new LinkedBlockingDeque<>();
        this.handlers = new ConcurrentHashMap<>();
        this.eventLoop = eventLoop;
    }


    // ======================   HANDLER MANAGEMENT

    /**
     * Adds a handler function which will get executed once a Packet with the matching clazz is received.
     *
     * @param clazz
     *          The class identifying the  for which the handler will be executed
     * @param handler
     *          The handler function
     */
    public <P extends APacket> void addPacketHandler(Class<P> clazz, Consumer<P> handler) {

        if (!getHandlers().containsKey(clazz))
            getHandlers().put(clazz, new ArrayList<>());

        getHandlers().get(clazz).add(handler);
    }

    /**
     * Removed a handler.
     *
     * @param clazz
     *          The class associated the handler is associated with
     * @param handler
     *          The handler function
     * @return
     *          {@code true} if the handler was actually removed | {@code false} if no matching handler was registered
     */
    public <P extends APacket> boolean removePacketHandler(Class<P> clazz, Consumer<P> handler) {

        // RET: No handlers for class
        if (!getHandlers().containsKey(clazz))
            return false;

        return getHandlers().get(clazz).remove(handler);

    }


    // ======================   EVENT HANDLERS

    /**
     * Calls all registered packet handlers.
     *
     * @param event
     *          The handled event
     */
    //@Synchronized
    public void handlePacketReceivedEvent(PacketReceivedEvent event) {
        APacket packet = event.getPacket();

        // RET: No handlers for abstractEvent!
        if (!getHandlers().containsKey(event.getPacket().getClass()))
            return;

        for (Consumer<? extends APacket> rawHandler : getHandlers().get(packet.getClass())) {
            Consumer<APacket> handler = (Consumer<APacket>) rawHandler;

            try { handler.accept(packet); }
            catch (Exception e) {
                new StrFmt("{prefix} Could not handle " + packet + "! Dropping it", e).setLevel(Level.WARN).toLog();
                //TODO FIX log.error(ExceptionUtils.getStackTraceAsString(e));
            }

        }

    }


    // ======================   HELPERS




}
