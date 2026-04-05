package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Electronic extends Item {
    private String brand;
    private int warrantyYears;

//    public Electronic(String name, String description, double startingPrice, Seller seller, String brand, int warrantyYears) {
//        super(name, description, startingPrice, seller);
//        this.brand = brand;
//        this.warrantyYears = warrantyYears;
//    }

    public Electronic(Params p){
        super(p);
        this.brand = p.getBrand();
        this.warrantyYears = p.getWarrantyYears();
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
    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setWarrantyYears(int warrantyYears) {
        this.warrantyYears = warrantyYears;
    }
    //endregion
}