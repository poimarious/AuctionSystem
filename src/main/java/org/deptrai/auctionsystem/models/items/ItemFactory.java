package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

import java.util.HashMap;

public class ItemFactory {

    public static Electronic createElectronicItem(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String brand, int warrantyYears) {
        return new Electronic(itemId, name, description, startingPrice, currentPrice, seller, brand, warrantyYears);
    }

    public static Art createArtItem(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String artist, int year) {
        return new Art(itemId, name, description, startingPrice, currentPrice, seller, artist, year);
    }

    public static Vehicle createVehicleItem(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String make, int mileage) {
        return new Vehicle(itemId, name, description, startingPrice, currentPrice, seller, make, mileage);
    }
}
