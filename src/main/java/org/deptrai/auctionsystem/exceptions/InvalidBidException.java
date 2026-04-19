package org.deptrai.auctionsystem.exceptions;

public class InvalidBidException extends Exception {
    public InvalidBidException() {
        super("Error: Invalid Bid.");
    }
    public InvalidBidException(String message) {
        super(message);
    }
}
