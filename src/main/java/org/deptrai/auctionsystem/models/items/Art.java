package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Art extends Item {
    private String artist;
    private int yearCreated;

    public Art(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    public Art(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String artist, int yearCreated) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
    
    @Override
    public void printInfo() {

    }

    @Override
    public String getCategory() {
        return "Art";
    }

    //region Getters
    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }
    //endregion

    //region Setters
    public Art setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public Art setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
        return this;
    }
    //endregion
}