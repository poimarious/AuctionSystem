package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.observer.AuctionObserver;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.observer.AuctionSubject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements AuctionSubject {
    private String auctionId; // Database automatically assign this
    private Item item;
    private double currentPrice;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private List<Bid> bids;
    private List<AuctionObserver> observers;

    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, LocalDateTime endTime){
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }// For creating a new object

    public Auction(String auctionId, Item item, double currentPrice, LocalDateTime startTime, AuctionStatus status, LocalDateTime endTime, List<Bid> bids){
        this.auctionId = auctionId;
        this.item = item;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.bids = bids;
        observers =  new CopyOnWriteArrayList<>(); // use CopyOnWriteArrayList to avoid ConcurrentModificationException
    }// For loading from database

    public void startAuction() {
        lock.lock();
        try {
            if (this.status == AuctionStatus.OPEN) {
                this.status = AuctionStatus.RUNNING;
                System.out.println("Auction for " + item.getName() + " is now RUNNING.");
                notifyStatusChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    public void closeAuction() {
        lock.lock();
        try {
            if (this.status == AuctionStatus.RUNNING) {
                this.status = AuctionStatus.FINISHED;
                System.out.println("Auction for " + item.getName() + " has FINISHED.");
                notifyStatusChanged();

                Bidder winner = getWinner();
                if (winner != null) {
                    System.out.println("Winner is: " + winner.getUsername() + " with $" + currentPrice);
                    // Next step could be transitioning to PAID
                } else {
                    System.out.println("No bids placed. Auction CANCELED.");
                    this.status = AuctionStatus.CANCELED;
                    notifyStatusChanged();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Place bid logic (We add 'synchronized' in Week 7 Task 4/5)
    public boolean placeBid(Bid bid) {
        lock.lock();
        try {
            if (bid.validate()) {
                this.currentPrice = bid.getAmount();
                this.bids.add(bid);

                // Notify all observers when a new valid bid is placed
                notifyBidPlaced(bid);
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false; // Invalid bid
    }

    public Bidder getWinner(){
        lock.lock();
        try {
            if (bids.isEmpty()) return null;
            // The last valid bid in the list is the winner
            return bids.get(bids.size() - 1).getBidder();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void attach(AuctionObserver observer) {
        if(!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Someone just subscribed to the session: " + item.getName());
        }
    }

    @Override
    public void detach(AuctionObserver observer) {
        if(observers.contains(observer)) {
            observers.remove(observer);
            System.out.println("Someone just unsubscribed to the session: " + item.getName());
        }
    }

    @Override
    public void notifyBidPlaced(Bid bid) {
        for(AuctionObserver obs: observers) {
            obs.onBidPlaced(this, bid);
        }
    }

    @Override
    public void notifyStatusChanged() {
        for(AuctionObserver obs: observers) {
            obs.onAuctionStatusChanged(this);
        }
    }

    //region Getters
    public String getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public List<AuctionObserver> getObservers() {
        return observers;
    }
    //endregion

    //region Setters
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public void setObservers(List<AuctionObserver> observers) {
        this.observers = observers;
    }
    //endregion
}