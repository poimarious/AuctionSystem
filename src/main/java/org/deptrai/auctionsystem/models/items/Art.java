package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class Art extends Item {
    private String artist;
    private int year;

    public Art(String name, String description, double startingPrice, Seller seller) {
        super(name, description, startingPrice, seller);
    }

    private Art(Builder builder) {
        super(builder.name, builder.description, builder.startingPrice, builder.seller);
        this.artist = builder.artist;
        this.year = builder.year;
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
    public Art setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public Art setYear(int year) {
        this.year = year;
        return this;
    }
    //endregion

    public static class Builder {
        private String name;
        private String description;
        private double startingPrice;
        private Seller seller;

        private String artist;
        private int year;


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

        public Builder setArtist(String artist) {
            this.artist = artist;
            return this;
        }
        public Builder setYear(int year) {
            this.year = year;
            return this;
        }

        public Art build() {
            return new Art(this);
        }
    }
}