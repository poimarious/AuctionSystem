package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Vehicle extends Item {
    private String make;
    private int mileage;

    public Vehicle(String name, String description, double startingPrice, Seller seller){
        super(name, description, startingPrice, seller);
    }

    public Vehicle(Builder builder) {
        super(builder.name, builder.description, builder.startingPrice, builder.seller);
        this.make = builder.make;
        this.mileage = builder.mileage;
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
    public Vehicle setMake(String make) {
        this.make = make;
        return this;
    }

    public Vehicle setMileage(int mileage) {
        this.mileage = mileage;
        return this;
    }
    //endregion

    public static class Builder {
        private String name;
        private String description;
        private double startingPrice;
        private Seller seller;

        private String make;
        private int mileage;


        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        public Builder setStartingPrice(double startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }
        public Builder setSeller(Seller seller ) {
            this.seller = seller;
            return this;
        }

        public Builder setMake(String make) {
            this.make = make;
            return this;
        }
        public Builder setMileage(int mileage) {
            this.mileage = mileage;
            return this;
        }

        public Vehicle build() {
            return new Vehicle(this);
        }
    }
}