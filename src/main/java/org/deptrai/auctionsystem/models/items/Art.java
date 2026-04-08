package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Art extends Item {
    private String artist;
    private int year;

//    public Art(String name, String description, double startingPrice, Seller seller, String artist, int year) {
//        super(name, description, startingPrice, seller);
//        this.artist = artist;
//        this.year = year;
//    }

    public Art(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    public Art(String itemId, String name, String description, double startingPrice, double currentPrice, Seller seller, String artist, int year) {
        super(itemId, name, description, startingPrice, currentPrice, seller);
        this.artist = artist;
        this.year = year;
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

    public int getYear() {
        return year;
    }
    //endregion

    //region Setters
    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setYear(int year) {
        this.year = year;
    }
    //endregion
}