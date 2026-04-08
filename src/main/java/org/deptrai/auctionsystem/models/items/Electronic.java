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

    private Electronic(Builder builder){
        super(builder.name, builder.description, builder.startingPrice, builder.seller);
        this.brand = builder.brand;
        this.warrantyYears = builder.warrantyYears;
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

    public static class Builder {
        private String name;
        private String description;
        private double startingPrice;
        private Seller seller;

        private String brand;
        private int warrantyYears;


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

        public Builder setBrand(String brand) {
            this.brand = brand;
            return this;
        }
        public Builder setWarrantyYears(int warrantyYears) {
            this.warrantyYears = warrantyYears;
            return this;
        }

        public Electronic build() {
            return new Electronic(this);
        }
    }
}