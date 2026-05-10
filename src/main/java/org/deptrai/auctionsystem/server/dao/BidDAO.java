package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.Bidder;

public class BidDAO {
  public boolean insertBid(Bid bid) {
    String sql =
        "INSERT INTO Bids (bidId, bidderId, auctionId, amount, timestamp) VALUES (?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      String id = (bid.getBidId() != null) ? bid.getBidId() : UUID.randomUUID().toString();
      bid.setBidId(id);

      pstmt.setString(1, bid.getBidId());
      pstmt.setString(2, bid.getBidder().getUserId());
      pstmt.setString(3, bid.getAuction().getAuctionId());
      pstmt.setDouble(4, bid.getAmount());
      pstmt.setString(5, bid.getTimestamp().toString());

      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.out.println("Lỗi lưu Bid: " + e.getMessage());
      return false;
    }
  }

  public List<Bid> getBidsByBidderId(String bidderId) {
    List<Bid> history = new CopyOnWriteArrayList<>();
    // Order by descending timestamps
    String sql = "SELECT * FROM Bids WHERE bidderId = ? ORDER BY timestamp DESC";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, bidderId);
      ResultSet rs = pstmt.executeQuery();

      UserDAO userDAO = new UserDAO();
      AuctionDAO auctionDAO = new AuctionDAO();

      Bidder currentBidder =
          (Bidder) userDAO.getUserById(bidderId); // Can pass in a Bidder instance from method

      while (rs.next()) {
        String bidId = rs.getString("bidId");
        double amount = rs.getDouble("amount");
        String timeString = rs.getString("timestamp");
        String auctionId = rs.getString("auctionId");

        LocalDateTime timestamp = LocalDateTime.parse(timeString);

        Auction auction = auctionDAO.getAuctionById(auctionId);

        Bid bid = new Bid(bidId, currentBidder, auction, amount, timestamp);
        history.add(bid);
      }

    } catch (SQLException e) {
      System.out.println("Lỗi tải lịch sử Bid: " + e.getMessage());
    }
    return history;
  }

  public List<Bid> getBidsByAuctionId(String auctionId, Auction auctionInstance) {
    List<Bid> history = new CopyOnWriteArrayList<>();
    // Order by descending timestamps
    String sql = "SELECT * FROM Bids WHERE auctionId = ? ORDER BY timestamp DESC";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, auctionId);
      ResultSet rs = pstmt.executeQuery();

      UserDAO userDAO = new UserDAO();

      while (rs.next()) {
        String bidId = rs.getString("bidId");
        double amount = rs.getDouble("amount");
        String bidderId = rs.getString("bidderId");
        LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));

        Bidder bidder = (Bidder) userDAO.getUserById(bidderId);

        // Passing in an auctionInstance to stop infinite loops of methods calling each other
        Bid bid = new Bid(bidId, bidder, auctionInstance, amount, timestamp);
        history.add(bid);
      }
    } catch (SQLException e) {
      System.out.println("Lỗi tải danh sách Bid của Auction: " + e.getMessage());
    }
    return history;
  }
}
