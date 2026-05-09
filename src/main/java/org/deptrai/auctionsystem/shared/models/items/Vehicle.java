package org.deptrai.auctionsystem.shared.models.items;

import org.deptrai.auctionsystem.shared.models.users.Seller;

public class Vehicle extends Item {
    private String make;
    private int mileage;

    public Vehicle(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    public Vehicle(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String make, int mileage) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.make = make;
        this.mileage = mileage;
    }

    @Override
    public void printInfo() {
        System.out.println("[VEHICLE]");
        System.out.println("Name :" + super.getName());
        System.out.println("Description: " + super.getDescription());
        System.out.println("Starting price: " + super.getStartingPrice() + "$");
        System.out.println("Seller: " + super.getSeller().getUsername());
        System.out.println();
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }

    //region Getters
    public String getMake() {
        return make;
    }

    public int getMileage() {
        return mileage;
    }
    //endregion

    //region Setters
    public Vehicle setMake(String make) {
        this.make = make;
        return this;
    }

    public Vehicle setMileage(int mileage) {
        this.mileage = mileage;
        return this;
    }
    //endregion
}