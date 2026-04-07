package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class VehicleParams extends ItemParams {
    private String make;
    private int mileage;

    public VehicleParams(String type, String name, String description, double startingPrice, Seller seller, String make, int mileage) {
        super(type, name, description, startingPrice, seller);
        this.make = make;
        this.mileage = mileage;
    }

    public String getMake() {return make;}

    public int getMileage() {return mileage;}
}
