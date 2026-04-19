package org.deptrai.auctionsystem.exceptions;

public class AuctionClosedException extends Exception {

    public AuctionClosedException() {
        super("Auction has closed");
    }

    public AuctionClosedException(String msg) {
        super(msg);
    }
}
