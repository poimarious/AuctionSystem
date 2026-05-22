package org.deptrai.auctionsystem.shared.models.auction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    this.bids = new ArrayList<>();
  }

  public Auction(
      String auctionId, Item item, double currentPrice,
      AuctionStatus status, LocalDateTime endTime, List<Bid> bids) {
    this.auctionId = auctionId;
    this.item = item;
    this.currentPrice = currentPrice;
    this.status = status;
    this.endTime = endTime;
    this.bids = bids;
  }

  public Bidder getWinner() {
    if (bids == null || bids.isEmpty()) return null;
    return bids.getLast().getBidder();
  }

  // region Getters & Setters
  public String getAuctionId() { return auctionId; }
  public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

  public Item getItem() { return item; }
  public void setItem(Item item) { this.item = item; }

  public double getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

  public AuctionStatus getStatus() { return status; }
  public void setStatus(AuctionStatus status) { this.status = status; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public List<Bid> getBids() { return bids; }
  public void setBids(List<Bid> bids) { this.bids = bids; }
  // endregion
}