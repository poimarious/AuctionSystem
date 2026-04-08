package org.deptrai.auctionsystem.models.items;


import org.deptrai.auctionsystem.models.users.Seller;

public class ElectronicFactory extends ItemFactory {

    @Override
    public Item createItem(String name, String description, double startingPrice, Seller seller) {
        return new Electronic(name, description, startingPrice, seller);
    }
}
