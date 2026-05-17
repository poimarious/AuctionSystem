package org.deptrai.auctionsystem.server.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.User;

public class AuctionDAO {
  public boolean insertAuction(Auction auction) {
    String sql =
        "INSERT INTO Auctions (auctionId, itemId, currentPrice, status, endTime) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      String id =
          (auction.getAuctionId() != null) ? auction.getAuctionId() : UUID.randomUUID().toString();
      auction.setAuctionId(id);

      pstmt.setString(
          1, auction.getAuctionId()); // Always has id if it is handled in AuctionManager
      pstmt.setString(2, auction.getItem().getItemId());
      pstmt.setDouble(3, auction.getCurrentPrice());
      pstmt.setString(4, auction.getStatus().name());
      pstmt.setString(5, auction.getEndTime().toString());

      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.out.println("Lỗi lưu Auction: " + e.getMessage());
      return false;
    }
  }

  // Use when there's a new bid/closing auction
  public boolean updateAuctionState(Auction auction) {
    String sql = "UPDATE Auctions SET currentPrice = ?, status = ?, endTime = ? WHERE auctionId = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setDouble(1, auction.getCurrentPrice());
      pstmt.setString(2, auction.getStatus().name());
      // SỬA: Lưu lại thời gian kết thúc (có thể đã được gia hạn)
      pstmt.setString(3, auction.getEndTime().toString());
      pstmt.setString(4, auction.getAuctionId());

      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.out.println("Lỗi cập nhật Auction: " + e.getMessage());
      return false;
    }
  }

  public Auction getAuctionById(String auctionId) {
    String sql = "SELECT * FROM Auctions WHERE auctionId = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, auctionId);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String itemId = rs.getString("itemId");
        double currentPrice = rs.getDouble("currentPrice");
        String statusStr = rs.getString("status");
        String endTimeStr = rs.getString("endTime");

        ItemDAO itemDAO = new ItemDAO();
        Item item = itemDAO.getItemById(itemId);

        AuctionStatus status = AuctionStatus.valueOf(statusStr);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);

        Auction auction =
            new Auction(
                auctionId, item, currentPrice, status, endTime, new CopyOnWriteArrayList<>());
        BidDAO bidDAO = new BidDAO();
        List<Bid> auctionBids = bidDAO.getBidsByAuctionId(auctionId, auction);
        auction.setBids(auctionBids);

        return auction;
      }
    } catch (SQLException e) {
      System.out.println("Lỗi tìm Auction theo ID: " + e.getMessage());
    }
    return null;
  }

  // Thêm hàm này vào dưới cùng của class AuctionDAO để đồng bộ với auction manager
  public List<Auction> getAllAuctions() {
    List<Auction> auctionList = new ArrayList<>();

    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt = conn.createStatement()) {
      Map<String, User> userCache = new HashMap<>();
      try(ResultSet rs = stmt.executeQuery("SELECT * FROM Users")) {
        while(rs.next()) {
          String role = rs.getString("role");
          String userId = rs.getString("userId");
          String username = rs.getString("username");
          String pass = rs.getString("password");
          String email = rs.getString("email");
          double balance = rs.getDouble("balance");

          User u = null;
          if ("BIDDER".equals(role)) {
            u = new org.deptrai.auctionsystem.shared.models.users.Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());
          } else if ("SELLER".equals(role)) {
            u = new org.deptrai.auctionsystem.shared.models.users.Seller(userId, username, pass, email);
          } else if ("ADMIN".equals(role)) {
            u = new org.deptrai.auctionsystem.shared.models.users.Admin(userId, username, pass, email, rs.getInt("adminLevel"));
          }
          if (u != null) {
            u.setBalance(balance);
            userCache.put(userId, u);
          }
        }
      }

      Map<String, Item> itemCache = new HashMap<>();
      try (ResultSet rs = stmt.executeQuery("SELECT * FROM Items")) {
        while (rs.next()) {
          String itemId = rs.getString("itemId");
          String category = rs.getString("category");
          org.deptrai.auctionsystem.shared.models.users.Seller seller =
                  (org.deptrai.auctionsystem.shared.models.users.Seller) userCache.get(rs.getString("sellerId"));

          org.deptrai.auctionsystem.shared.models.items.ItemFactory factory = null;
          if ("Electronics".equals(category))
            factory = new org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory();
          else if ("Art".equals(category)) factory = new org.deptrai.auctionsystem.shared.models.items.ArtFactory();
          else if ("Vehicle".equals(category))
            factory = new org.deptrai.auctionsystem.shared.models.items.VehicleFactory();

          if (factory != null) {
            Item item = factory.createItem(rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), seller);
            item.setItemId(itemId);
            item.setImageUrl(rs.getString("imageUrl"));

            if (item instanceof org.deptrai.auctionsystem.shared.models.items.Electronics e) {
              e.setBrand(rs.getString("brand")).setWarrantyMonths(rs.getInt("warrantyMonths"));
            } else if (item instanceof org.deptrai.auctionsystem.shared.models.items.Art a) {
              a.setArtist(rs.getString("artist")).setYearCreated(rs.getInt("yearCreated"));
            } else if (item instanceof org.deptrai.auctionsystem.shared.models.items.Vehicle v) {
              v.setMake(rs.getString("make")).setMileage(rs.getInt("mileage"));
            }
            itemCache.put(itemId, item);
          }
        }
      }

      Map<String, Auction> auctionCache = new LinkedHashMap<>();
      try (ResultSet rs = stmt.executeQuery("SELECT * FROM Auctions ORDER BY rowid ASC")) {
        while (rs.next()) {
          String auctionId = rs.getString("auctionId");
          Item item = itemCache.get(rs.getString("itemId"));
          AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
          LocalDateTime endTime = LocalDateTime.parse(rs.getString("endTime"));

          Auction auction = new Auction(auctionId, item, rs.getDouble("currentPrice"), status, endTime, new CopyOnWriteArrayList<>());
          auctionCache.put(auctionId, auction);
          auctionList.add(auction); // Giữ đúng thứ tự cho danh sách trả về
        }
      }

      try (ResultSet rs = stmt.executeQuery("SELECT * FROM Bids ORDER BY timestamp ASC")) {
        while (rs.next()) {
          String auctionId = rs.getString("auctionId");
          Auction auction = auctionCache.get(auctionId);

          if (auction != null) {
            org.deptrai.auctionsystem.shared.models.users.Bidder bidder =
                    (org.deptrai.auctionsystem.shared.models.users.Bidder) userCache.get(rs.getString("bidderId"));

            Bid bid = new Bid(
                    rs.getString("bidId"), bidder, auction, rs.getDouble("amount"), LocalDateTime.parse(rs.getString("timestamp"))
            );
            auction.getBids().add(bid);
          }
        }
      }

    } catch(Exception e) {
      System.out.println("Lỗi khi lấy danh sách tất cả Auction bằng Cache RAM: " + e.getMessage());
      e.printStackTrace();
    }
    return auctionList;
  }

  public boolean deleteAuctionById(String auctionId,String itemId) {

    String deleteBidsSql = "DELETE FROM Bids WHERE auctionId = ?";
    String deleteAuctionSql = "DELETE FROM Auctions WHERE auctionId = ?";
    String deleteItemSql = "DELETE FROM Items WHERE itemId = ?";
    try (Connection conn = DatabaseConnection.getConnection()) {
      conn.setAutoCommit(false); // Bắt đầu Transaction

      try (PreparedStatement pstmtBids = conn.prepareStatement(deleteBidsSql);
          PreparedStatement pstmtAuction = conn.prepareStatement(deleteAuctionSql);
          PreparedStatement pstmtItem = conn.prepareStatement(deleteItemSql)) {

        // 1. Xóa Bids
        pstmtBids.setString(1, auctionId);
        pstmtBids.executeUpdate();

        // 2. Xóa Auction
        pstmtAuction.setString(1, auctionId);
        pstmtAuction.executeUpdate();

        // 3. Xóa Item
        pstmtItem.setString(1, itemId);
        pstmtItem.executeUpdate();

        conn.commit(); // Thành công thì lưu
        return true;

      } catch (SQLException e) {
        conn.rollback(); // Lỗi thì khôi phục
        throw e;
      }
    } catch (SQLException e) {
      System.out.println("Lỗi khi xóa triệt để Auction và Item: " + e.getMessage());
      return false;
    }
  }
}
