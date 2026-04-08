package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class ArtFactory extends ItemFactory {

    @Override
    public Item createItem(String name, String description, double startingPrice, Seller seller) {
        return new Art(name, description, startingPrice, seller);
    }
}
