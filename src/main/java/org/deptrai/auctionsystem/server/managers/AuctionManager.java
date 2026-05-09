package org.deptrai.auctionsystem.server.managers;

import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.items.Item;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;


public class AuctionManager {
    //implement treemap with thread-safe
    private ConcurrentSkipListMap<String, Auction> auctions = new ConcurrentSkipListMap<>();
    private AuctionManager() {}

    private static class SingletonHelper {//Helper class to generate instance,because this class only loads once so it ensures thread-safe
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() { //Bill Pugh singleton implementation
        return SingletonHelper.INSTANCE;
    }
    public Auction createAuction(Item item, LocalDateTime endTime) {
        Auction newAuction = new Auction(item, endTime);

        //need initialized after creating object to get auctionId
        String tempId = UUID.randomUUID().toString();
        newAuction.setAuctionId(tempId);

        auctions.put(newAuction.getAuctionId(), newAuction);
        return newAuction;
    }

    public Auction getAuction(String id) {
        return auctions.get(id);
    }
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }
    // MAYBE NEED ONE MORE METHOD THAT CONNECT TO DATABASE
}