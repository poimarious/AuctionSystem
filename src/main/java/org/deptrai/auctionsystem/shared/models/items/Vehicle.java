package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class Vehicle extends Item {
  private String make;
  private int mileage;

  public Vehicle(String name, String description, double startingPrice, Seller seller) {
    super(name, description, startingPrice, seller);
  }

  public Vehicle(
          String itemId,
          String name,
          String description,
          double startingPrice,
          Seller seller,
          String make,
          int mileage) {
    super(itemId, name, description, startingPrice, seller);
    this.make = make;
    this.mileage = mileage;
  }

  @Override
  public String getCategory() {
    return "Vehicle";
  }

  // region Getters and Setters
  public String getMake() {
    return make;
  }

  public Vehicle setMake(String make) {
    this.make = make;
    return this;
  }

  public int getMileage() {
    return mileage;
  }

  public Vehicle setMileage(int mileage) {
    this.mileage = mileage;
    return this;
  }

  // endregion
}
