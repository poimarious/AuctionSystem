package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class ElectronicParams extends ItemParams {
    private String brand;
    private int warrantyYears;

    public ElectronicParams(String type, String name, String description, double startingPrice, Seller seller, String brand, int warrantyYears) {
        super(type, name, description, startingPrice, seller);
        this.brand = brand;
        this.warrantyYears = warrantyYears;
    }

    public void setBrand(String brand) {this.brand = brand;}
    public void setWarrantyYears(int warrantyYears) {this.warrantyYears = warrantyYears;}

    public String getBrand() {return brand;}
    public int getWarrantyYears() {return warrantyYears;}
}
