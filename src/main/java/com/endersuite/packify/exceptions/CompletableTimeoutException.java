package com.endersuite.packify.exceptions;

import com.endersuite.packify.transmission.CompletableTransmission;
import lombok.Getter;

/**
 * TODO: Add docs
 *
 * @author Maximilian Vincent Heidenreich
 * @since 12.05.21
 */
public class CompletableTimeoutException extends PackifyException {

    @Getter
    private CompletableTransmission transmission;

    public CompletableTimeoutException(CompletableTransmission transmission) {
        this.transmission = transmission;
    }

}
