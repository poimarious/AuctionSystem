package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Params {

    private String type;
    private String name;
    private String description;
    private double startingPrice;
    private Seller seller;


    // Electronic's params
    private String brand;
    private int warrantyYears;

    // Art's params
    private String artist;
    private int year;

    // Vehicle params
    private String make;
    private int mileage;

    // Default constructor
    public Params() {}

    // Setters
    public Params setType(String type) {
        this.type = type;
        return this;
    }
    public Params setName(String name) {
        this.name = name;
        return this;
    }
    public Params setDescription(String description) {
        this.description = description;
        return this;
    }
    public Params setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
        return this;
    }
    public Params  setSeller(Seller seller) {
        this.seller = seller;
        return this;
    }
    public Params setBrand(String brand) {
        this.brand = brand;
        return this;
    }
    public Params setWarrantyYears(int warrantyYears) {
        this.warrantyYears = warrantyYears;
        return this;
    }
    public Params setArtist(String artist) {
        this.artist = artist;
        return this;
    }
    public Params setYear(int year) {
        this.year = year;
        return this;
    }
    public Params setMake(String make) {
        this.make = make;
        return this;
    }
    public Params setMileage(int mileage) {
        this.mileage = mileage;
        return this;
    }
    // return a Params type to make a "connector"
    // example:
    // Params p = new Params()
    // p.setType("Art")
    //  .setName("Poi Highlight")
    //  ...
    // instead of
    // p.setType()
    // p.setName()
    // ...

    // Getters
    public String getType() {
        return type;
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
    public Seller getSeller() {
        return seller;
    }
    public String getBrand() {
        return brand;
    }
    public int getWarrantyYears() {
        return warrantyYears;
    }
    public String getArtist() {
        return artist;
    }
    public int getYear() {
        return year;
    }
    public String getMake() {
        return make;
    }
    public int getMileage() {
        return mileage;
    }
}
