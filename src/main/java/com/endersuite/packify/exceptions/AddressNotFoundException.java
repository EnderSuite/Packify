package com.endersuite.packify.exceptions;

import lombok.Getter;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class AddressNotFoundException extends PackifyException {

    @Getter
    private final String address;

    public AddressNotFoundException(String address) {
        this.address = address;
    }

}
