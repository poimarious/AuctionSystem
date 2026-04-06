package org.deptrai.auctionsystem.models.auction;

import org.deptrai.auctionsystem.models.items.Item;
import java.time.LocalDateTime;
import java.util.*;


public class AuctionManager {
    private SortedMap<String,Auction> auctions = Collections.synchronizedSortedMap(new TreeMap<>());
    //implement treemap with thread-safe
    private AuctionManager(){}
    private static class SingletonHelper {//Helper class to generate instance,because this class only loads once so it ensures thread-safe
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() { //Bill Pugh singleton implementation
        return SingletonHelper.INSTANCE;
    }
    public Auction createAuction(Item item, LocalDateTime startTime, LocalDateTime endTime) {
        /* Currently not doing anything,depends on everyone to choose creating obj on this class or itself.
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        status = AuctionStatus.OPEN;
        bids = new ArrayList<>();
        observers = new ArrayList<>();
         */
        return null;
    }

    public Auction getAuction(String id) {
        for (Auction auction: auctions.values()) {
            if (auction.getAuctionId().equals(id)) {
                return auction;
            }
        }
        return null;
    }
    public List<Auction> getAllAuctions() {
        ArrayList<Auction> auctionlist = new ArrayList<>();
        for (Auction auction: auctions.values()) {
            auctionlist.add(auction);
        }
        return auctionlist;
    }
    // MAYBE NEED ONE MORE METHOD THAT CONNECT TO DATABASE
}
