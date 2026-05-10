package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class ElectronicsFactory extends ItemFactory {

  @Override
  public Electronics createItem(
      String name, String description, double startingPrice, Seller seller) {
    return new Electronics(name, description, startingPrice, seller);
  }
}
