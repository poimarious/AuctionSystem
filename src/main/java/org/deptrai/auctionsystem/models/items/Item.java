package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

import java.io.Serializable;

public abstract class Item implements Serializable {
    private String itemId; // Database automatically assigns this
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private Seller seller;

    public Item() {}

    public Item(String name, String description, double startingPrice, Seller seller) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice; // currentPrice is the same as startingPrice when starting out
        this.currentPrice = startingPrice;
        this.seller = seller;
    } //For creating a new object

    public Item(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller){
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.seller = seller;
    } //For loading an object


    public abstract void printInfo();
    public abstract String getCategory();

    //region Getters
    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Seller getSeller() {
        return seller;
    }
    //endregion

    //region Setters
    public Item setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    public Item setName(String name) {
        this.name = name;
        return this;
    }

    public Item setDescription(String description) {
        this.description = description;
        return this;
    }

    public Item setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
        return this;
    }

    public Item setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    public Item setSeller(Seller seller) {
        this.seller = seller;
        return this;
    }
    // endregion
}