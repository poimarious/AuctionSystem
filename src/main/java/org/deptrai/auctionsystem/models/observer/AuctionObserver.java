package org.deptrai.auctionsystem.models.observer;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.bid.BidEvent;

public interface AuctionObserver {
    void onBidPlaced(BidEvent event);
    void onAuctionStatusChanged(Auction a);
}
