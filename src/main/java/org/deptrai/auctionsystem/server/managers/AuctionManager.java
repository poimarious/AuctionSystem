package org.deptrai.auctionsystem.server.managers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.items.Item;

public class AuctionManager {
  // implement treemap with thread-safe
  private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();

  private static class SingletonHelper { // Helper class to generate instance,because this class only loads once so it ensures thread-safe
    private static final AuctionManager INSTANCE = new AuctionManager();
  }

  private AuctionManager() {}

  public static AuctionManager getInstance() { // Bill Pugh singleton implementation
    return SingletonHelper.INSTANCE;
  }

  public Auction createAuction(Item item, LocalDateTime endTime) {
    Auction newAuction = new Auction(item, endTime);

    // need initialized after creating object to get auctionId
    String tempId = UUID.randomUUID().toString();
    newAuction.setAuctionId(tempId);

    //auctions.put(newAuction.getAuctionId(), newAuction);
    return newAuction;
  }

  public Auction getAuctionById(String id) {
    return auctions.get(id);
  }

  public List<Auction> getAllAuctions() {
    return new ArrayList<>(auctions.values());
  }

  // MAYBE NEED ONE MORE METHOD THAT CONNECT TO DATABASE
  public void loadAuctionsFromDatabase() {
    AuctionDAO auctionDAO = new AuctionDAO();
    List<Auction> dbAuctions = auctionDAO.getAllAuctions();

    // Đưa từng phiên đấu giá từ DB lên RAM (ConcurrentSkipListMap)
    for (Auction auction : dbAuctions) {
      auctions.put(auction.getAuctionId(), auction);
    }
    System.out.println(
        "Đã đồng bộ " + dbAuctions.size() + " phiên đấu giá từ Database lên AuctionManager.");
  }
  // Thêm method này để đồng bộ 1 phiên đấu giá lẻ từ DB lên RAM,do nếu lệnh lưu vào db thất bại thì phiên đấu giá vẫn tồn tại trên ram
  public void addAuctionToMemory(Auction auction) {
    if (auction != null && auction.getAuctionId() != null) {
      auctions.put(auction.getAuctionId(), auction);
    }
  }
  //Method xóa sp trên RAM ,sử dụng khi seller muốn xóa sản phẩm
  public void removeAuctionFromMemory(String auctionId) {
    if (auctionId != null) {
      auctions.remove(auctionId);
    }
  }
}
