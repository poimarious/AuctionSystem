package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class ArtFactory extends ItemFactory {

  @Override
  public Art createItem(String name, String description, double startingPrice, Seller seller) {
    return new Art(name, description, startingPrice, seller);
  }
}
