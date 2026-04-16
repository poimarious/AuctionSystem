package org.deptrai.auctionsystem.models;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.users.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder;
    private Auction auction;
    private double amount;
    private AuctionStatus status;
    private LocalDateTime timestamp;

    public BidTransaction(Bidder bidder, Auction auction, double amount) {
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
    }

    public void markOutbid(){

    }
    public void markRejected(){

    }
    public void markWinning(){

    }
}
