package org.deptrai.auctionsystem.models.observer;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.bid.Bid;

public interface AuctionObserver {
    void onBidPlaced(Auction a, Bid b);

    void onAuctionStatusChanged(Auction a);
}
