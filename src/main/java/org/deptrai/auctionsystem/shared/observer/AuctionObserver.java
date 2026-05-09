package org.deptrai.auctionsystem.shared.observer;

import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.bid.Bid;

public interface AuctionObserver {
    void onBidPlaced(Auction a, Bid b);

    void onAuctionStatusChanged(Auction a);
}
