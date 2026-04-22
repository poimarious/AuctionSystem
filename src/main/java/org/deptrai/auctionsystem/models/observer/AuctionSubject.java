package org.deptrai.auctionsystem.models.observer;

import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.observer.AuctionObserver;

public interface AuctionSubject {
    void attach(AuctionObserver observer);

    void detach(AuctionObserver observer);

    void notifyBidPlaced(Bid bid);

    void notifyStatusChanged();
}