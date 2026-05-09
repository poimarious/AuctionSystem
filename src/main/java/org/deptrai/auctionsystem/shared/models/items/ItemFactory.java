package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public abstract class ItemFactory {
    public abstract Item createItem(String name, String description, double startingPrice, Seller seller);
}
