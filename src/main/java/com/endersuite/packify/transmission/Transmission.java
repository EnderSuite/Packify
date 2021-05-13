package com.endersuite.packify.transmission;

import com.endersuite.packify.NetworkManager;
import com.endersuite.packify.exceptions.AddressNotFoundException;
import com.endersuite.packify.packets.APacket;
import org.jgroups.Address;
import org.jgroups.Message;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class Transmission {

    // ======================   VARS

    private final Message message;

    // ======================   CONSTRUCTOR

    protected Transmission(Message message) {
        this.message = message;
    }

    // ======================   BUSINESS LOGIC

    public Transmission to(Address address) {
        message.setDest(address);
        return this;
    }

    public Transmission to(String nodeName) throws AddressNotFoundException {
        Address address = NetworkManager.getInstance().getJChannel().getView().getMembers().stream()
                .filter(a -> a.toString().equals(nodeName)).findFirst().orElse(null);

        // THROW: Address is not known in cluster
        if (address == null)
            throw new AddressNotFoundException(nodeName);

        return to(address);
    }

    public Transmission broadcast(boolean loopback) {
        message.setDest(null);

        if (!loopback)
            message.setTransientFlag(Message.TransientFlag.DONT_LOOPBACK);

        return this;
    }

    public Transmission relay(boolean relay) {
        if (relay)
            message.clearFlag(Message.Flag.NO_RELAY);
        else
            message.setFlag(Message.Flag.NO_RELAY);
        return this;
    }

    public CollectableTransmission toCollectable() {
        return new CollectableTransmission(message);
    }

    public void transmit() throws Exception {
        NetworkManager.getInstance().sendRaw(message);
    }

    public boolean sneakyTransmit() {
        try {
            NetworkManager.getInstance().sendRaw(message);
        } catch (Exception exception) {
            return false;
        }
        return true;
    }


    // ======================   HELPERS

    public static Transmission fromPacket(APacket packet) {
        Message message = new Message(null, packet);
        return new Transmission(message);
    }

    public static Transmission fromMessage(Message message) {
        return new Transmission(message);
    }

}
