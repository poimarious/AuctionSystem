package org.deptrai.auctionsystem.shared.models.items;

import java.io.Serializable;
import org.deptrai.auctionsystem.shared.models.users.Seller;

public abstract class Item implements Serializable {
  private String itemId; // Database automatically assigns this
  private String name;
  private String description;
  private double startingPrice;
  private Seller seller;
  private String imageUrl;
  public Item() {}

  public Item(String name, String description, double startingPrice, Seller seller) {
    this.name = name;
    this.description = description;
    this.startingPrice =
        startingPrice; // currentPrice is the same as startingPrice when starting out
    this.seller = seller;
  } // For creating a new object

  public Item(
      String itemId,
      String name,
      String description,
      double startingPrice,
      double currentPrice,
      Seller seller) {
    this.itemId = itemId;
    this.name = name;
    this.description = description;
    this.startingPrice = startingPrice;
    this.seller = seller;
  } // For loading an object

  public abstract void printInfo();

  public abstract String getCategory();

  // region Getters
  public String getItemId() {
    return itemId;
  }

  // region Setters
  public Item setItemId(String itemId) {
    this.itemId = itemId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Item setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  // endregion

  public Item setDescription(String description) {
    this.description = description;
    return this;
  }

  public double getStartingPrice() {
    return startingPrice;
  }

  public Item setStartingPrice(double startingPrice) {
    this.startingPrice = startingPrice;
    return this;
  }

  public Seller getSeller() {
    return seller;
  }

  public Item setSeller(Seller seller) {
    this.seller = seller;
    return this;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
  // endregion\

}
