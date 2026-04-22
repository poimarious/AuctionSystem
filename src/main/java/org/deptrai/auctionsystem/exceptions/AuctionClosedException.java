package org.deptrai.auctionsystem.exceptions;

public class AuctionClosedException extends RuntimeException {

    public AuctionClosedException() {
        super("Auction has closed");
    }

    public AuctionClosedException(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
