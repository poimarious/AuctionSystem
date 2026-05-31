package org.deptrai.auctionsystem.shared.models.auction;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AuctionSummary implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String auctionId;
  private final String itemName;
  private final String itemDescription;
  private final String category;
  private double currentPrice;
  private AuctionStatus status;
  private final LocalDateTime endTime;
  private final String imageUrl;
  private byte[] imageBytes;

  public AuctionSummary(String auctionId, String itemName, String itemDescription,
                        String category, double currentPrice, AuctionStatus status,
                        LocalDateTime endTime, String imageUrl, byte[] imageBytes) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.itemDescription = itemDescription;
    this.category = category;
    this.currentPrice = currentPrice;
    this.status = status;
    this.endTime = endTime;
    this.imageUrl = imageUrl;
    this.imageBytes = imageBytes;
  }

  // region Getters & Setters
  public String getAuctionId() {return auctionId;}
  public String getItemName() {return itemName;}
  public String getItemDescription() {return itemDescription;}
  public String getCategory() {return category;}
  public double getCurrentPrice() {return currentPrice;}
  public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}
  public AuctionStatus getStatus() {return status;}
  public void setStatus(AuctionStatus status) {this.status = status;}
  public LocalDateTime getEndTime() {return endTime;}
  public String getImageUrl() {return imageUrl;}
  public byte[] getImageBytes() {
    return imageBytes;
  }
  public void setImageBytes(byte[] imageBytes) {
    this.imageBytes = imageBytes;
  }
  // endregion
}