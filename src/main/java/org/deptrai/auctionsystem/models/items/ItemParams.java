package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class ItemParams {

    private String type;
    private String name;
    private String description;
    private double startingPrice;
    private Seller seller;


    public ItemParams(String type, String name, String description, double startingPrice, Seller seller) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.seller = seller;
    }


    // Getters
    public String getType() {return type;}
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public Seller getSeller() {
        return seller;
    }
}
