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
     * Store all registered callbacks for single packet collection.
     */
    private final Map<UUID, CompletableFuture<APacket>> callbacks;   // <packetId, <callback, timeout>>




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
        this.callbacks = new ConcurrentHashMap<>();
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


    // ======================   CALLBACK MANAGEMENT

    /**
     * Adds a callback.
     *
     * @param packet
     *          The packet associated with the callback
     * @return
     *          {@code true} if added | {@code false} if not added
     */
    /*public boolean addCallback(AbstractPacket packet, CompletableFuture<AbstractPacket> callback) {

        // RET: Callback already exists! This indicates a possible issue with packet id's and reusing ids to fast
        if (getCallbacks().containsKey(packet.getId()))
            return false;

        getCallbacks().put(packet.getId(), Pair.from(callback, packet.getTimout()));
        return true;

    }*/

    /**
     * Removed a callback.
     *
     * @param packet
     *          The packet associated with the callback
     * @return
     *          {@code true} if removed | {@code false} if not removed
     */
    /*public boolean removeCallback(AbstractPacket packet) {

        // RET: No registered callback!
        if (!getCallbacks().containsKey(packet.getId()))
            return false;

        getCallbacks().remove(packet.getId());
        return true;

    }*/

    /**
     * Removes all timed out callbacks.
     * Note: A timed out callback will automatically get removed if a timed out packet was received.
     *       But this prevents memory leaks if a packet never gets delivered!
     */
    /*private void cleanupTimeoutCallbacks() {
        List<UUID> timedOutPackets = getCallbacks().keySet().stream()
                .filter(uuid -> {
                    long timeout =getCallbacks().get(uuid).getB();
                    return (timeout != 0 && System.currentTimeMillis() > timeout);
                }).collect(Collectors.toList());

        log.debug("[JNet] Found " + timedOutPackets.size() + " timed out packets to remove!");
        timedOutPackets.forEach(p -> getCallbacks().remove(p));
    }*/


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

        // RET: Timeout!
        /*if (packet.isTimeout()) {
            exceptCallback(packet, new PacketTimeoutException(packet));
            return;
        }*/

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

        //if (packet instanceof ExceptionPacket) exceptCallback(packet, ((ExceptionPacket) packet).getException());
        //else completeCallback(packet);

    }


    // ======================   HELPERS

    /**
     * Completes a registered callback with the specified packet as data.
     *
     * @param packet
     *          The data to pass back to the callbacks
     */
    /*public void completeCallback(AbstractPacket packet) {

        // RET: No registered callbacks!
        if (!getCallbacks().containsKey(packet.getId())) return;

        getCallbacks().get(packet.getId()).getA().complete(packet);
        removeCallback(packet);
    }*/

    /**
     * Excepts a registered callback.
     *
     * @param throwable
     *          The reason why except was called
     */
    /*public void exceptCallback(AbstractPacket packet, Throwable throwable) {

        // RET: No registered callbacks!
        if (!getCallbacks().containsKey(packet.getId())) return;

        getCallbacks().get(packet.getId()).getA().completeExceptionally(throwable);
        removeCallback(packet);
    }*/


}
