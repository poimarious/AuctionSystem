package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

import java.io.Serializable;

public abstract class Item implements Serializable {
  private String itemId; // Database automatically assigns this
  private String name;
  private String description;
  private double startingPrice;
  private Seller seller;
  private String imageUrl;
  private byte[] imageBytes;

  public Item() {}

  public Item(String name, String description, double startingPrice, Seller seller) {
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.seller = seller;
  }

  public Item(
          String itemId,
          String name,
          String description,
          double startingPrice,
          Seller seller) {
    this.itemId = itemId;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.seller = seller;
  }

  public abstract String getCategory();

  // region Getters
  public String getItemId() {
    return itemId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public Seller getSeller() {
    return seller;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public byte[] getImageBytes() {return imageBytes;}

  // endregion

  // region Setters
  public Item setItemId(String itemId) {
    this.itemId = itemId;
    return this;
  }


  public Item setName(String name) {
    this.name = name;
    return this;
  }

  public Item setDescription(String description) {
    this.description = description;
    return this;
  }

  public Item setStartingPrice(double startingPrice) {
    this.startingPrice = startingPrice;
    return this;
  }

  public Item setSeller(Seller seller) {
    this.seller = seller;
    return this;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public void setImageBytes(byte[] imageBytes) {this.imageBytes = imageBytes;}

  // endregion
}