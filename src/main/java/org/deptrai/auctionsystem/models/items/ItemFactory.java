package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public abstract class ItemFactory {
    public abstract Item createItem(String name, String description, double startingPrice, Seller seller);
}
