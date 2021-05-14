package com.endersuite.packify.transmission;

import com.endersuite.packify.NetworkManager;
import com.endersuite.packify.exceptions.AddressNotFoundException;
import com.endersuite.packify.packets.APacket;
import lombok.Getter;
import lombok.Setter;
import org.jgroups.Address;
import org.jgroups.Message;

/**
 * A Transmission stores a {@link Message} and can be send (transmitted) to other nodes in the cluster.
 * A Transmission can only be created using a {@link TransmissionBuilder}.
 *
 * <br><br><i>Note: If you want to receive and collect responses from your transmission,
 * use one of the {@code collectXXX()} methods!</i>
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class Transmission {

    @Getter @Setter
    private static NetworkManager defaultNetworkManager;


    // ======================   VARS

    /**
     * The message that will be sent.
     */
    private final Message message;


    // ======================   CONSTRUCTOR

    /**
     * Creates a simple transmission for a given message.
     *
     * @param message
     *          The message to transmit
     */
    protected Transmission(Message message) {
        this.message = message;
    }


    // ======================   BUSINESS LOGIC

    /**
     * Sends the message to other nodes in the cluster.
     * <br><br><i>Note: To specify a receiver or advanced broadcast options,
     * use {@link TransmissionBuilder#to(Address)} or {@link TransmissionBuilder#broadcast(boolean)}!</i>
     *
     * @throws Exception
     *          Any possible exceptions whilst transmitting
     */
    public void transmit() throws Exception {
        getDefaultNetworkManager().sendRaw(message);
    }

    /**
     * Wrapper around {@link Transmission#transmit()} that catches all Exceptions
     * and just returns a boolean instead.
     *
     * @return {@code true} if no exception occurred | {@code false} if not
     */
    public boolean sneakyTransmit() {
        try {
            transmit();
        } catch (Exception exception) {
            return false;
        }
        return true;
    }


    // ======================   HELPERS

    /**
     * Creates a new TransmissionBuilder from a raw packet.
     *
     * @param packet
     *          The packet to send
     * @return
     */
    public static TransmissionBuilder newBuilder(APacket packet) {
        Message message = new Message(null, packet);
        return new TransmissionBuilder(message);
    }

    /**
     * Creates a new TransmissionBuilder from any JGroup {@link Message} object.
     *
     * @param message
     *          The message object to send
     * @return
     */
    public static TransmissionBuilder newBuilder(Message message) {
        return new TransmissionBuilder(message);
    }


    // ======================   BUILDER

    /**
     * A builder that abstracts the utility methods to construct a {@link Transmission}.
     */
    public static class TransmissionBuilder {

        // ======================   VARS

        /**
         * The message to configure.
         */
        private final Message message;


        // ======================   CONSTRUCTOR

        protected TransmissionBuilder(Message message) {
            this.message = message;
        }


        // ======================   BUSINESS LOGIC

        /**
         * Sets the recipient of the message.
         *
         * @param address
         *          The raw JGroups address object
         * @return
         */
        public TransmissionBuilder to(Address address) {
            this.message.setDest(address);
            return this;
        }

        /**
         * Sets the recipient of the message by its specified node name.
         *
         * @param nodeName
         *          The name of the recipient node inside the cluster
         * @return
         * @throws AddressNotFoundException
         *          If the given nodeName could not be matched with an Address inside the cluster
         */
        public TransmissionBuilder to(String nodeName) throws AddressNotFoundException {
            Address address = getDefaultNetworkManager().getJChannel().getView().getMembers().stream()
                    .filter(a -> a.toString().equals(nodeName)).findFirst().orElse(null);

            // THROW: Address is not known in cluster
            if (address == null)
                throw new AddressNotFoundException(nodeName);

            return to(address);
        }

        /**
         * Sets the message to be broadcast to all nodes inside the cluster.
         *
         * @param loopback
         *          Whether the loopback node (the sender) should receive this message
         * @return
         */
        public TransmissionBuilder broadcast(boolean loopback) {
            this.message.setDest(null);
            if (!loopback)
                this.message.setTransientFlag(Message.TransientFlag.DONT_LOOPBACK);
            return this;
        }

        /**
         * Sets whether or not to relay the message to other connected JGroup clusters.
         *
         * @param relay
         *          Relay to other clusters if {@code true} otherwise not.
         * @return
         */
        public TransmissionBuilder relay(boolean relay) {
            if (relay)
                this.message.clearFlag(Message.Flag.NO_RELAY);
            else
                this.message.setFlag(Message.Flag.NO_RELAY);
            return this;
        }

        /**
         * Specifies that the done consumer (as specified by {@code onDone()}) should be called after receiving
         * at least one response packet from all nodes in the cluster.
         * <br><br><i>Note: If a node disconnects whilst a CompletableTransmission is pending,
         * it will automatically detect it and call the done consumer if the amount of already received
         * response packets exceeds the new cluster size.</i>
         * <br><br><i>Note: This also transforms the builder into a {@link CompletableTransmission.CompletableTransmissionBuilder}</i>
         *
         * @return
         */
        public CompletableTransmission.CompletableTransmissionBuilder collectAll() {
            return collectExact(getDefaultNetworkManager().getJChannel().getView().getMembers().size());
        }

        /**
         * Specifies that the done consumer (as specified by {@code onDone()}) should be called after receiving
         * at least {@code minReplies} amount of response packet.
         * <br><br><i>Note: This also transforms the builder into a {@link CompletableTransmission.CompletableTransmissionBuilder}</i>
         *
         * @param minReplies
         *          The minimum amount of response packets
         * @return
         */
        public CompletableTransmission.CompletableTransmissionBuilder collectExact(int minReplies) {
            return new CompletableTransmission.CompletableTransmissionBuilder(this.message, minReplies);
        }

        /**
         * Specifies that the done consumer (as specified by {@code onDone()}) should be called after receiving
         * at least one response packet.
         * <br><br><i>Note: This also transforms the builder into a {@link CompletableTransmission.CompletableTransmissionBuilder}</i>
         *
         * @return
         */
        public CompletableTransmission.CompletableTransmissionBuilder collectOne() {
            return collectExact(1);
        }

        /**
         * Builds a Transmission object based on the previous configuration.
         *
         * @return
         */
        public Transmission build() {
            return new Transmission(this.message);
        }

    }

}
