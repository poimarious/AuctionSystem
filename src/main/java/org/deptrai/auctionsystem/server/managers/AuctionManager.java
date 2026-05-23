package org.deptrai.auctionsystem.server.managers;

import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.items.Item;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
  // implement treemap with thread-safe
  private final Map<String, Auction> auctions = Collections.synchronizedMap(new LinkedHashMap<>());

  // Map này userId -> <AuctionId, MaxBid>
  private final Map<String, Map<String, Double>> autoBidLocks = new ConcurrentHashMap<>();

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
    List<Auction> list;
    // 1. Khóa Map lại trong chốc lát để bốc dữ liệu ra an toàn (Thread-safe)
    synchronized (auctions) {
      list = new ArrayList<>(auctions.values());
    }
    // 2. Lật ngược danh sách (Reverse) ngay tại Server
    Collections.reverse(list);
    return list;
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


  public void registerAutoBid(String userId, String auctionId, double maxBid) {
    autoBidLocks.computeIfAbsent(userId, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(auctionId, maxBid);
  }

  public void unregisterAutoBid(String userId, String auctionId) {
    if (autoBidLocks.containsKey(userId)) {
      autoBidLocks.get(userId).remove(auctionId);
    }
  }

  public double getTotalLockedAutoBid(String userId, String excludeAuctionId) {
    if (!autoBidLocks.containsKey(userId)) return 0.0;

    double total = 0.0;
    for (java.util.Map.Entry<String, Double> entry : autoBidLocks.get(userId).entrySet()) {
      if (!entry.getKey().equals(excludeAuctionId)) {
        total += entry.getValue();
      }
    }
    return total;
  }
}
