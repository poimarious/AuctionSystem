package org.deptrai.auctionsystem.models.users;

import org.deptrai.auctionsystem.exceptions.AuctionClosedException;
import org.deptrai.auctionsystem.exceptions.InvalidBidException;
import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.observer.AuctionObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bidder extends User implements AuctionObserver {
    private List<Bid> bidHistory;

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        bidHistory = new ArrayList<>();
    }

    public Bidder(String userId, String username, String password, String email, double balance, List<Bid> bidHistory) {
        super(userId, username, password, email);
        this.bidHistory = bidHistory;
    }

    public void placeBid(Auction auction, double amount) {
        Bid newBid = new Bid(this, auction, amount, LocalDateTime.now());

        if(auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Error when place bid: Auction has been closed!");
        }
        else if(amount < 0) {
            throw new InvalidBidException("Error when place bid: amount is lower then 0$.");
        }
        else if(auction.getCurrentPrice() >= amount) {
            throw new InvalidBidException("Error when place bid: amount is lower than or equal current price(" + auction.getCurrentPrice() + ")");
        }
        else {
            this.bidHistory.add(newBid);
            System.out.println("Bid placed successfully by " + super.getUsername());
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
        return "Bidder: " + getUsername() + " | Email: " + getEmail();
    }

    @Override
    public void onBidPlaced(Auction a, Bid b) {
        if(this.getUsername().equals(b.getBidder().getUsername())) {
            System.out.println("[" + this.getUsername() + "] Nice! You are leading! '" + a.getItem().getName() + "' with $" + b.getAmount());
        } else {
            System.out.println("[" + this.getUsername() + "] Notice: " + b.getBidder().getUsername() + " just increased the bid for '" + a.getItem().getName() + "' by $" + b.getAmount());
        }
    }

    @Override
    public void onAuctionStatusChanged(Auction a) {
        System.out.println("[" + this.getUsername() + "] Announcement: Auction '" + a.getItem().getName() + "' has just changed its status to: " + a.getStatus());
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
