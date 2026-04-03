package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

import java.util.HashMap;

public class ItemFactory {


    public static Item createItem(String type, String id, String name, String description, double startingPrice, double currentPrice, Seller seller, HashMap<String, Object> subParams) {
        switch(type.toUpperCase()) {
            case "ELECTRONIC":
                String brand;
                int warrantyYears;
                brand = (String) subParams.get("brand");
                warrantyYears = (int) subParams.get("warrantyYears");
                return new Electronic(id, name,description, startingPrice, currentPrice, seller, brand, warrantyYears);
            case "ART":
                String artist;
                int year;
                artist = (String) subParams.get("artist");
                year = (int) subParams.get("year");
                return new Art(id, name, description, startingPrice, currentPrice, seller, artist, year);
            default: // Vehicle
                String make;
                int mileage;
                make = (String) subParams.get("make");
                mileage = (int) subParams.get("mileage");
                return new Vehicle(id, name, description, startingPrice, currentPrice, seller, make, mileage);
        }
    }

}
