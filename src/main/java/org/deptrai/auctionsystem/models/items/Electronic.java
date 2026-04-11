package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Electronic extends Item {
    private String brand;
    private int warrantyYears;

    public Electronic(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    public Electronic(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String brand, int warrantyYears) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.brand = brand;
        this.warrantyYears = warrantyYears;
    }

    @Override
    public void printInfo() {

    }

    @Override
    public String getCategory() {
        return "Electronic";
    }

    //region Getters
    public String getBrand() {
        return brand;
    }

    public int getWarrantyYears() {
        return warrantyYears;
    }
    //endregion

    //region Setters
    public Electronic setBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public Electronic setWarrantyYears(int warrantyYears) {
        this.warrantyYears = warrantyYears;
        return this;
    }
    //endregion


}