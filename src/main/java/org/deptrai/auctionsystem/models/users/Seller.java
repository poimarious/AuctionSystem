package org.deptrai.auctionsystem.models.users;
import org.deptrai.auctionsystem.models.items.Item;

import java.util.List;

public class Seller extends User {
    private List<Item> listings;
    private double rating;

    public Seller(String username, String password, String email) {
        super(username, password, email);
        rating = -1.0; // No rating yet
    }

    public Seller(String userId, String username, String password, String email, double rating) {
        super(userId, username, password, email);
        this.rating = rating;
    }

    public void addItem(Item item){
        listings.add(item);
    }

    public void removeItem(String id){
        for (Item item : listings){
            if (item.getItemId().equals(id)){
                listings.remove(item);
                break;
            }
        }
    }

    @Override
    public boolean login() {
        return false;
    }

    @Override
    public void logout() {

    }

    @Override
    public String getInfo() {
        return "";
    }

    //region Getters
    public double getRating() {
        return rating;
    }

    public List<Item> getListings(){
        return listings;
    }
    //endregion

    //region Setters
    public void setListings(List<Item> listings) {
        this.listings = listings;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
    //endregion
}