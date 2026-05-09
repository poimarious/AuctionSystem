package org.deptrai.auctionsystem.shared.observer;

import org.deptrai.auctionsystem.shared.models.bid.Bid;

public interface AuctionSubject {
    void attach(AuctionObserver observer);

    void detach(AuctionObserver observer);

    void notifyBidPlaced(Bid bid);

    void notifyStatusChanged();
}