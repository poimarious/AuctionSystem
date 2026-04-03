package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.models.users.Bidder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer; // Deprecated. Can upgrade

public class Auction {
    private String auctionId; // Database automatically assign this
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<Bid> bids;
    private List<Observer> observers; //Deprecated. Can upgrade

    public Auction(Item item, LocalDateTime startTime, LocalDateTime endTime){
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        status = AuctionStatus.OPEN;
        bids = new ArrayList<>();
        observers = new ArrayList<>();
    }// For creating a new object

    public Auction(String auctionId, Item item, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status, List<Bid> bids){
        this.auctionId = auctionId;
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.bids = bids;
        observers = new ArrayList<>();
    }// For loading from database

    public void placeBid(Bidder bidder, double amount) {

    }

    public void close() {

    }

    public Bidder getWinner(){
        return null;
    }

    public void addObserver(Observer o){

    }//Deprecated. Can upgrade

    public void notifyObservers(){

    }//Deprecated. Can upgrade

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

    public List<Observer> getObservers() {
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

    public void setObservers(List<Observer> observers) {
        this.observers = observers;
    }
    //endregion
}