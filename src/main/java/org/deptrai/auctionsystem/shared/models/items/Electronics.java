package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    public Electronics(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String brand, int warrantyMonths) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[ELECTRONIC]");
        System.out.println("Name :" + super.getName());
        System.out.println("Description: " + super.getDescription());
        System.out.println("Starting price: " + super.getStartingPrice() + "$");
        System.out.println("Seller: " + super.getSeller().getUsername());
        System.out.println();
    }

    @Override
    public String getCategory() {
        return "Electronics";
    }

    //region Getters
    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }
    //endregion

    //region Setters
    public Electronics setBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public Electronics setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
        return this;
    }
    //endregion

}