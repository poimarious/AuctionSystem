package org.deptrai.auctionsystem.models.users;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.observer.BidObserver;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User implements BidObserver {
    private List<Bid> bidHistory;

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        bidHistory = new ArrayList<>();
    }

    public Bidder(String userId, String username, String password, String email, double balance, List<Bid> bidHistory) {
        super(userId, username, password, email);
        this.bidHistory = bidHistory;
    }

    public void placeBid(Auction auction, double amount){

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

    @Override
    public void update(double newPrice, String topBidderName) {
        if(super.getUsername() == topBidderName) {
            System.out.println( "[" + super.getUsername() + "] Nice. you are the top Bidder with $" + newPrice);
        } else {
            System.out.print("[" + super.getUsername() + "] Warning: Price has been pushed up to $" + newPrice);
        }
    }

    //region Getters
    public List<Bid> getBidHistory() {
        return bidHistory;
    }
    //endregion

    //region Setters
    public void setBidHistory(List<Bid> bidHistory) {
        this.bidHistory = bidHistory;
    }
    //endregion
}
