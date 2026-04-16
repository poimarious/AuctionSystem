package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.observer.AuctionSubject;
import org.deptrai.auctionsystem.observer.BidObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements AuctionSubject {
    private String auctionId; // Database automatically assign this
    private Item item;
    private double currentPrice;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private List<Bid> bids;
    private List<BidObserver> observers;

    public Auction(Item item, LocalDateTime endTime){
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        status = AuctionStatus.OPEN;
        this.endTime = endTime;
        bids = new ArrayList<>();
        observers = new CopyOnWriteArrayList<>();
    }// For creating a new object

    public Auction(String auctionId, Item item, double currentPrice, startTime, AuctionStatus status, LocalDateTime endTime, List<Bid> bids){
        this.auctionId = auctionId;
        this.item = item;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
        this.bids = bids;
        observers =  new CopyOnWriteArrayList<>(); // use CopyOnWriteArrayList to avoid ConcurrentModificationException
    }// For loading from database

    public void placeBid(Bid bid) {

    }

    public void closeAuction() {

    }

    public Bidder getWinner(){
        return null;
    }

    public void addObserver(BidObserver o){

    }

    @Override
    public void attach(BidObserver observer) {
        if(!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("Someone just subscribed to the session: " + item.getName());
        }
    }

    @Override
    public void detach(BidObserver observer) {
        if(observers.contains(observer)) {
            observers.remove(observer);
            System.out.println("Someone just unsubscribed to the session: " + item.getName());
        }
    }

    @Override
    public void notifyObservers(){
        for(BidObserver obs: observers) {
            obs.update(item.getCurrentPrice(), topBidderName);
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

    public List<BidObserver> getObservers() {
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

    public void setObservers(List<BidObserver> observers) {
        this.observers = observers;
    }
    //endregion
}