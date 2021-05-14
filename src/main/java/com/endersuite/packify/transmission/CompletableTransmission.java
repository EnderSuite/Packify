package com.endersuite.packify.transmission;

import com.endersuite.packify.exceptions.CompletableTimeoutException;
import com.endersuite.packify.packets.ACollectablePacket;
import lombok.Getter;
import org.jgroups.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A CompletableTransmission stores a {@link Message}, can be send (transmitted) to other nodes in the cluster
 * and collects response packets.
 * A CompletableTransmission can only be created using a {@link CompletableTransmissionBuilder}.
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class CompletableTransmission extends Transmission {

    // ======================   VARS

    /**
     * The unique id used to match request & response packets.
     */
    @Getter
    private final UUID collectionId;

    /**
     * The minimum amount of reply packets which need to be received before the callback is triggered.
     */
    @Getter
    private final int minReplies;

    /**
     * An optional timeout after which the timeoutConsumer will be called.
     */
    @Getter
    private final Duration timeout;

    // Internal storage of received packets & callback / consumer stuff.
    private final List<ACollectablePacket> receivedResponsePackets;
    private final CompletableFuture<List<ACollectablePacket>> callback;
    private final Consumer<CompletableTimeoutException> timeoutConsumer;
    private final Consumer<Throwable> errorConsumer;


    // ======================   CONSTRUCTOR

    /**
     * Creates a new CompletableTransmission with the given parameters.
     * It will also setup the {@code exception} channel of the callback to distinguish timeout and other errors.
     * @param message
     * @param collectionId
     * @param minReplies
     * @param timeout
     * @param callback
     * @param timeoutConsumer
     * @param errorConsumer
     */
    protected CompletableTransmission(
            Message message,
            UUID collectionId,
            int minReplies,
            Duration timeout,
            CompletableFuture<List<ACollectablePacket>> callback,
            Consumer<CompletableTimeoutException> timeoutConsumer,
            Consumer<Throwable> errorConsumer
    ) {
        super(message);
        this.collectionId = collectionId;
        this.minReplies = minReplies;
        this.timeout = timeout;
        this.receivedResponsePackets = new ArrayList<>();
        this.callback = callback;
        this.timeoutConsumer = timeoutConsumer;
        this.errorConsumer = errorConsumer;

        // Sets timeout / err handling
        this.callback
                .exceptionally((Throwable throwable) -> {
                    if (throwable instanceof CompletableTimeoutException)
                        this.timeoutConsumer.accept((CompletableTimeoutException) throwable);
                    else
                        this.errorConsumer.accept(throwable);
                    return null;
                });
    }


    // ======================   BUSINESS LOGIC
    // TODO: update state if a node disconnects

    /**
     * Adds a packet to the internal list.
     *
     * @param responsePacket
     *          The packet to add
     */
    public void addResponsePacket(ACollectablePacket responsePacket) {
        this.receivedResponsePackets.add(responsePacket);
    }

    /**
     * Calls the done consumer with the internal store of received response packets.
     */
    public void complete() {
        this.callback.complete(this.receivedResponsePackets);
    }

    /**
     * Calls the error consumer.
     *
     * @param throwable
     *          The reason
     */
    public void error(Throwable throwable) {
        this.callback.completeExceptionally(throwable);
    }

    /**
     * Cancels the CompletableTransmission.
     * <br><br><i>Note: This will also call the error consumer with a {@link java.util.concurrent.CancellationException}!</i>
     */
    public void cancel() {
        getDefaultNetworkManager().getCollectablePacketHandler().getPendingTransmissions().remove(this.collectionId);
        this.callback.cancel(true);
    }

    @Override
    public void transmit() throws Exception {

        // Store pending transmission & Start timeout task
        getDefaultNetworkManager().getCollectablePacketHandler().getPendingTransmissions().put(collectionId, this);
        if (this.timeout != null) {
            getDefaultNetworkManager().getScheduler().schedule(() -> {
                this.callback.completeExceptionally(new CompletableTimeoutException(this));
            }, getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }

        super.transmit();
    }


    // ======================   HELPERS

    /**
     * Returns whether the CompletableTransmission's requirement of minimum replies is satisfied.
     *
     * @return
     */
    public boolean isCompletable() {
        return true;
    }


    // ======================   BUILDER

    /**
     * A builder that abstracts the utility methods to construct a {@link CompletableTransmission}.
     */
    public static class CompletableTransmissionBuilder {

        // ======================   VARS

        // Builder state
        private final Message message;
        private final UUID collectionId;
        private final int minReplies;
        private Duration timeout;
        private final CompletableFuture<List<ACollectablePacket>> callback;
        private Consumer<CompletableTimeoutException> timeoutConsumer;
        private Consumer<Throwable> errorConsumer;


        // ======================   CONSTRUCTOR

        protected CompletableTransmissionBuilder(Message message, int minReplies) {
            this.message = message;

            ACollectablePacket collectablePacket = message.getObject();
            this.collectionId = collectablePacket.getCollectionId();
            this.minReplies = minReplies;
            this.timeout = null;
            this.callback = new CompletableFuture<>();
        }


        // ======================   BUSINESS LOGIC

        /**
         * Sets the timeout after which the timeout consumer will be called.
         *
         * @param duration
         *          Teh duration
         * @return
         */
        public CompletableTransmissionBuilder timeout(Duration duration) {
            this.timeout = duration;
            return this;
        }

        /**
         * Specified the consumer that will be called after all required response packets
         * (as specified by {@code collectAll()} / {@code collectMultiple()} / {@code collectOne()}) have been received.
         *
         * @param consumer
         *          The consumer to use.
         * @return
         */
        public CompletableTransmissionBuilder onDone(Consumer<List<ACollectablePacket>> consumer) {
            this.callback.thenAccept(consumer);
            return this;
        }

        /**
         * Specifies the consumer that will be called on a {@link CompletableTimeoutException}.
         *
         * @param consumer
         *          The consumer to use
         * @return
         */
        public CompletableTransmissionBuilder onTimeout(Consumer<CompletableTimeoutException> consumer) {
            this.timeoutConsumer = consumer;
            return this;
        }

        /**
         * Specifies the consumer that will be called on any (including timeout) exceptions.
         *
         * @param consumer
         *          The consumer to use
         * @return
         */
        public CompletableTransmissionBuilder onError(Consumer<Throwable> consumer) {
            this.errorConsumer = consumer;
            return this;
        }

        /**
         * Builds a CollectableTransmission object based on the previous configuration.
         *
         * @return
         */
        public CompletableTransmission build() {
            return new CompletableTransmission(
                this.message,
                this.collectionId,
                this.minReplies,
                this.timeout,
                this.callback,
                this.timeoutConsumer,
                this.errorConsumer
            );
        }

    }

}
