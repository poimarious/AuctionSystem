package org.deptrai.auctionsystem.models.users;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.bid.Bid;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private double balance;
    private List<Bid> activeBids;

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        balance = 0.0;
        activeBids = new ArrayList<>();
    }

    public Bidder(String userId, String username, String password, String email, double balance, List<Bid> activeBids) {
        super(userId, username, password, email);
        this.balance = balance;
        this.activeBids = activeBids;
    }

    public void placeBid(Auction auction, double amount){

    }

    public void setAutoBid(double maxBid, double incr){

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
    public double getBalance() {
        return balance;
    }

    public List<Bid> getActiveBids() {
        return activeBids;
    }
    //endregion

    //region Setters
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setActiveBids(List<Bid> activeBids) {
        this.activeBids = activeBids;
    }
    //endregion
}
