package org.deptrai.auctionsystem.shared.models.bid;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.deptrai.auctionsystem.shared.exceptions.AuctionClosedException;
import org.deptrai.auctionsystem.shared.exceptions.AuthenticationException;
import org.deptrai.auctionsystem.shared.exceptions.InvalidBidException;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

public class Bid implements Serializable {
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
  } // For creating a new object

  public Bid(String bidId, Bidder bidder, Auction auction, double amount, LocalDateTime timestamp) {
    this.bidId = bidId;
    this.bidder = bidder;
    this.auction = auction;
    this.amount = amount;
    this.timestamp = timestamp;
  } // For loading from database

  public boolean validate() {
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new AuctionClosedException("Failed when place bid: Auction is closed.");
    } else if (amount < 0) {
      throw new InvalidBidException("Failed when place bid: amount is lower then zero.");
    } else if (amount <= auction.getCurrentPrice()) {
      throw new InvalidBidException(
          "Failed when place bid: amount is lower than or equal current price.");
    } else if (bidder.getUserId().equals(auction.getItem().getSeller().getUserId())) {
      throw new AuthenticationException("Failed when place bid: the seller cannot place bid.");
    }
    return true;
  }

  public String getDetails() {
    return String.format("[%s] %s bid $%.2f", timestamp, bidder.getUsername(), amount);
  }

  // region Getters
  public String getBidId() {
    return bidId;
  }

  // region Setters
  public void setBidId(String bidId) {
    this.bidId = bidId;
  }

  public Bidder getBidder() {
    return bidder;
  }

  public void setBidder(Bidder bidder) {
    this.bidder = bidder;
  }

  public Auction getAuction() {
    return auction;
  }

  // endregion

  public void setAuction(Auction auction) {
    this.auction = auction;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  // endregion
}
