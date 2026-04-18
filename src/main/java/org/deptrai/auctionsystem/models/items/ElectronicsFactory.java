package org.deptrai.auctionsystem.models.items;


import org.deptrai.auctionsystem.models.users.Seller;

public class ElectronicsFactory extends ItemFactory {

    @Override
    public Electronics createItem(String name, String description, double startingPrice, Seller seller) {
        return new Electronics(name, description, startingPrice, seller);
    }
}
