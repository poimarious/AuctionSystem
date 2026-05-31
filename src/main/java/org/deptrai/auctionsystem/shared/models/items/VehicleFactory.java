package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class VehicleFactory extends ItemFactory {

  @Override
  public Vehicle createItem(String name, String description, double startingPrice, Seller seller) {
    return new Vehicle(name, description, startingPrice, seller);
  }
}
