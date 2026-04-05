package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

import java.util.HashMap;

public class ItemFactory {

    public static Item createItem(Params params) {
        switch(params.getType().toUpperCase()) {
            case "ELECTRONIC":
                return new Electronic(params);
            case "ART":
                return new Art(params);
            case "VEHICLE":
                return new Vehicle(params);
            default:
                throw new IllegalArgumentException("Item Type Error:" + params.getType());
        }
    }
}
