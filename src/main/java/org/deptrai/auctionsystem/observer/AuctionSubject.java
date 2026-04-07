package org.deptrai.auctionsystem.observer;

public interface AuctionSubject {

    void attach(BidObserver observer); // subscribe to the session
    void detach(BidObserver observer); // Unsubscribe to the Auction
    void notifyObservers(); // Notify to all Bidder
}
