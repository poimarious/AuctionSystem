package org.deptrai.auctionsystem.models.items;

import org.deptrai.auctionsystem.models.users.Seller;

public class ArtParams extends ItemParams {
    private String artist;
    private int year;


    public ArtParams(String type, String name, String description, double startingPrice, Seller seller, String artist, int year) {
        super(type, name, description, startingPrice, seller);
        this.artist = artist;
        this.year = year;
    }

    public String getArtist() {return artist;}
    public int getYear() {return year;}
}
