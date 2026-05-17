package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Item;

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
    String sql =
        "SELECT * FROM Auctions ORDER BY rowid ASC"; // Lấy tất cả, hoặc bạn có thể thêm: WHERE status = 'OPEN'

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {

      ItemDAO itemDAO = new ItemDAO();
      BidDAO bidDAO = new BidDAO();

      while (rs.next()) {
        String auctionId = rs.getString("auctionId");
        String itemId = rs.getString("itemId");
        double currentPrice = rs.getDouble("currentPrice");
        String statusStr = rs.getString("status");
        String endTimeStr = rs.getString("endTime");

        Item item = itemDAO.getItemById(itemId);
        AuctionStatus status = AuctionStatus.valueOf(statusStr);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);

        Auction auction =
            new Auction(
                auctionId, item, currentPrice, status, endTime, new CopyOnWriteArrayList<>());

        // Nạp luôn danh sách lịch sử Bid của phiên đấu giá này
        List<Bid> auctionBids = bidDAO.getBidsByAuctionId(auctionId, auction);
        auction.setBids(auctionBids);

        auctionList.add(auction);
      }
    } catch (SQLException e) {
      System.out.println("Lỗi khi lấy danh sách tất cả Auction từ DB: " + e.getMessage());
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
