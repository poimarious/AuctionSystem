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
  public void printInfo() {
    System.out.println("[ART]");
    System.out.println("Name :" + super.getName());
    System.out.println("Description: " + super.getDescription());
    System.out.println("Starting price: " + super.getStartingPrice() + "$");
    System.out.println("Seller: " + super.getSeller().getUsername());
    System.out.println();
  }

  @Override
  public String getCategory() {
    return "Art";
  }

  // region Getters
  public String getArtist() {
    return artist;
  }

  // region Setters
  public Art setArtist(String artist) {
    this.artist = artist;
    return this;
  }

  // endregion

  public int getYearCreated() {
    return yearCreated;
  }

  public Art setYearCreated(int yearCreated) {
    this.yearCreated = yearCreated;
    return this;
  }
  // endregion
}
