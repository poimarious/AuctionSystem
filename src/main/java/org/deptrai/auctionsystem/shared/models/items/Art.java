package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class Art extends Item {
  private String artist;
  private int yearCreated;

  public Art(String name, String description, double startingPrice, Seller seller) {
    super(name, description, startingPrice, seller);
  }

  public Art(
          String itemId,
          String name,
          String description,
          double startingPrice,
          Seller seller,
          String artist,
          int yearCreated) {
    super(itemId, name, description, startingPrice, seller);
    this.artist = artist;
    this.yearCreated = yearCreated;
  }

  @Override
  public String getCategory() {
    return "Art";
  }

  // region Getters and Setters
  public int getYearCreated() {
    return yearCreated;
  }

  public Art setYearCreated(int yearCreated) {
    this.yearCreated = yearCreated;
    return this;
  }

  public String getArtist() {
    return artist;
  }

  public Art setArtist(String artist) {
    this.artist = artist;
    return this;
  }

  // endregion


}
