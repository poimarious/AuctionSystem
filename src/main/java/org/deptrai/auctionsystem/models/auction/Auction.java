package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.observer.AuctionSubject;
import org.deptrai.auctionsystem.observer.BidObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer; // Deprecated. Can upgrade
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction implements AuctionSubject {
    private String auctionId; // Database automatically assign this
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String topBidderName;
    private List<Bid> bids;
    //private List<Observer> observers; //Deprecated. Can upgrade
    private List<BidObserver> observers;

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime){
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        status = AuctionStatus.OPEN;
        bids = new ArrayList<>();
        observers = new CopyOnWriteArrayList<>();
    }// For creating a new object

    public Auction(String auctionId, Item item, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status, List<Bid> bids){
        this.auctionId = auctionId;
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.bids = bids;
        observers =  new CopyOnWriteArrayList<>(); // use CopyOnWriteArrayList to avoid ConcurrentModificationException
    }// For loading from database

    public void placeBid(Bidder bidder, double amount) {

    }

    public void close() {

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

    public void extendIfSniped() {

    }

    //region Getters
    public String getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public AuctionStatus getStatus() {
        return status;
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

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public void setObservers(List<BidObserver> observers) {
        this.observers = observers;
    }
    //endregion
}