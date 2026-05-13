package org.deptrai.auctionsystem.shared.models.auction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

public class Auction implements Serializable {

  private String auctionId;
  private Item item;
  private double currentPrice;
  private AuctionStatus status;
  private LocalDateTime endTime;
  private List<Bid> bids;

  public Auction(Item item, LocalDateTime endTime) {
    this.item = item;
    this.currentPrice = item.getStartingPrice();
    this.status = AuctionStatus.OPEN;
    this.endTime = endTime;
    this.bids = new CopyOnWriteArrayList<>();
  }

  public Auction(
      String auctionId,
      Item item,
      double currentPrice,
      AuctionStatus status,
      LocalDateTime endTime,
      List<Bid> bids) {
    this.auctionId = auctionId;
    this.item = item;
    this.currentPrice = currentPrice;
    this.status = status;
    this.endTime = endTime;
    this.bids = bids;
  }

  public synchronized void startAuction() {
    if (this.status == AuctionStatus.OPEN) {
      this.status = AuctionStatus.RUNNING;
      System.out.println("Auction for " + item.getName() + " is now RUNNING.");
    }
  }

  public synchronized void closeAuction() {
    if (this.status == AuctionStatus.RUNNING) {
      this.status = AuctionStatus.FINISHED;
      System.out.println("Auction for " + item.getName() + " has FINISHED.");

      Bidder winner = getWinner();
      if (winner != null) {
        System.out.println("Winner is: " + winner.getUsername() + " with $" + currentPrice);
      } else {
        System.out.println("No bids placed. Auction CANCELED.");
        this.status = AuctionStatus.CANCELED;
      }
    }
  }

  public synchronized boolean placeBid(Bid bid) {
    if (bid.validate()) {
      this.currentPrice = bid.getAmount();
      this.bids.add(bid);
      return true;
    }
    return false;
  }

  public synchronized Bidder getWinner() {
    if (bids.isEmpty()) return null;
    return bids.get(bids.size() - 1).getBidder();
  }

  // region Getters & Setters
  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public void setStatus(AuctionStatus status) {
    this.status = status;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public List<Bid> getBids() {
    return bids;
  }

  public void setBids(List<Bid> bids) {
    this.bids = bids;
  }
  // endregion
}