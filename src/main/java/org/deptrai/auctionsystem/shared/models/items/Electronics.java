package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class Electronics extends Item {
  private String brand;
  private int warrantyMonths;

  public Electronics(String name, String description, double startingPrice, Seller seller) {
    super(name, description, startingPrice, seller);
  }

  public Electronics(
          String itemId,
          String name,
          String description,
          double startingPrice,
          Seller seller,
          String brand,
          int warrantyMonths) {
    super(itemId, name, description, startingPrice, seller);
    this.brand = brand;
    this.warrantyMonths = warrantyMonths;
  }

  @Override
  public String getCategory() {
    return "Electronics";
  }

  // region Getters and Setters
  public String getBrand() {
    return brand;
  }

  public Electronics setBrand(String brand) {
    this.brand = brand;
    return this;
  }

  public int getWarrantyMonths() {
    return warrantyMonths;
  }

  public Electronics setWarrantyMonths(int warrantyMonths) {
    this.warrantyMonths = warrantyMonths;
    return this;
  }

  // endregion

}
