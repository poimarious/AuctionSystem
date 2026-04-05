package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Vehicle extends Item {
    private String make;
    private int mileage;

//    public Vehicle(String name, String description, double startingPrice, Seller seller, String make, int mileage) {
//        super(ps);
//        this.make = make;
//        this.mileage = mileage;
//    }

    public Vehicle(Params p){
        super(p);
        this.make = p.getMake();
        this.mileage = p.getMileage();
    }

    public Vehicle(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String make, int mileage) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.make = make;
        this.mileage = mileage;
    }

    @Override
    public void printInfo() {

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
    public void setMake(String make) {
        this.make = make;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }
    //endregion
}