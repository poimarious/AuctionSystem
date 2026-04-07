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

//    public Item(String name, String description, double startingPrice, Seller seller){
//        this.name = name;
//        this.description = description;
//        this.startingPrice = startingPrice;
//        this.currentPrice = startingPrice; // currentPrice is the same as startingPrice when starting out
//        this.seller = seller;
//    } //For creating a new object

    public Item(ItemParams p) {
        this.name = p.getName();
        this.description = p.getDescription();
        this.startingPrice = p.getStartingPrice(); // currentPrice is the same as startingPrice when starting out
        this.seller = p.getSeller();
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
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }
    // endregion

}