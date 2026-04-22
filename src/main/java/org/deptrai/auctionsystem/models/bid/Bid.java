package org.deptrai.auctionsystem.models.bid;

import org.deptrai.auctionsystem.exceptions.AuctionClosedException;
import org.deptrai.auctionsystem.exceptions.InvalidBidException;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.users.Bidder;

import java.time.LocalDateTime;

public class Bid {
    private String bidId; // Database automatically assign this
    private Bidder bidder;
    private Auction auction;
    private double amount;
    private LocalDateTime timestamp;

    public Bid(Bidder bidder, Auction auction, double amount, LocalDateTime timestamp) {
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = timestamp;
    }// For creating a new object

    public Bid(String bidId, Bidder bidder, Auction auction, double amount, LocalDateTime timestamp) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = timestamp;
    }// For loading from database

    public boolean validate() {
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Fail when place bid: Auction is closed.");
        } else if (amount < 0) {
            throw new InvalidBidException("Fail when place bid: amount is lower then zero.");
        } else if (amount <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Fail when place bid: amount is lower than or equal current price.");
        }
        return true;
    }

    public String getDetails() {
        return String.format("[%s] %s bid $%.2f", timestamp, bidder.getUsername(), amount);
    }

    //region Getters
    public String getBidId() {
        return bidId;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public Auction getAuction() {
        return auction;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    //endregion

    //region Setters
    public void setBidId(String bidId) {
        this.bidId = bidId;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    //endregion
}
