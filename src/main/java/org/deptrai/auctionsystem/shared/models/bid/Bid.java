package org.deptrai.auctionsystem.shared.models.bid;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

public class Bid implements Serializable {
  private String bidId;
  private Bidder bidder;
  private Auction auction;
  private double amount;
  private LocalDateTime timestamp;

  public Bid(Bidder bidder, Auction auction, double amount, LocalDateTime timestamp) {
    this.bidder = bidder;
    this.auction = auction;
    this.amount = amount;
    this.timestamp = timestamp;
  }

  public Bid(String bidId, Bidder bidder, Auction auction, double amount, LocalDateTime timestamp) {
    this.bidId = bidId;
    this.bidder = bidder;
    this.auction = auction;
    this.amount = amount;
    this.timestamp = timestamp;
  }

  // region Getters & Setters
  public String getBidId() { return bidId; }
  public void setBidId(String bidId) { this.bidId = bidId; }

  public Bidder getBidder() { return bidder; }
  public void setBidder(Bidder bidder) { this.bidder = bidder; }

  public Auction getAuction() { return auction; }
  public void setAuction(Auction auction) { this.auction = auction; }

  public double getAmount() { return amount; }
  public void setAmount(double amount) { this.amount = amount; }

  public LocalDateTime getTimestamp() { return timestamp; }
  public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
  // endregion
}