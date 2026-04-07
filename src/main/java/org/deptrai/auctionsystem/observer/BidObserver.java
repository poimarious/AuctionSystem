package org.deptrai.auctionsystem.observer;

public interface BidObserver {
    void update(double newPrice, String topBidderName);
}
