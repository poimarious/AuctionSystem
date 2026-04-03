package org.deptrai.auctionsystem.models.bid;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.users.Bidder;

import java.time.LocalDateTime;

public class Bid {
    private String bidId; // Database automatically assign this
    private Bidder bidder;
    private Auction auction;
    private double amount;
    private LocalDateTime timestamp;
    private boolean isAutoBid;

    public Bid(Bidder bidder, Auction auction, double amount, LocalDateTime timestamp){
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = timestamp;
        isAutoBid = false;
    }// For creating a new object

    public Bid(String bidId, Bidder bidder, Auction auction, double amount, LocalDateTime timestamp){
        this.bidId = bidId;
        this.bidder = bidder;
        this.auction = auction;
        this.amount = amount;
        this.timestamp = timestamp;
        isAutoBid = false;
    }// For loading from database

    public boolean validate(){
        return false;
    }

    public String getDetails(){
        return null;
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

    public boolean isAutoBid() {
        return isAutoBid;
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

    public void setAutoBid(boolean autoBid) {
        isAutoBid = autoBid;
    }
    //endregion
}
