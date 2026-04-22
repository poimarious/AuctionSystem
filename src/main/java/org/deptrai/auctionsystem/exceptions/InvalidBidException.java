package org.deptrai.auctionsystem.exceptions;

public class InvalidBidException extends RuntimeException {
    public InvalidBidException() {
        super("Error: Invalid Bid.");
    }
    public InvalidBidException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
