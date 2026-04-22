package org.deptrai.auctionsystem.dao;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.models.bid.Bid;
import org.deptrai.auctionsystem.models.items.Item;
import org.deptrai.auctionsystem.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionDAO {
    public boolean insertAuction(Auction auction) {
        String sql = "INSERT INTO Auctions (auctionId, itemId, currentPrice, status, endTime) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String id = (auction.getAuctionId() != null) ? auction.getAuctionId() : UUID.randomUUID().toString();
            auction.setAuctionId(id);

            pstmt.setString(1, auction.getAuctionId()); // Always has id if it is handled in AuctionManager
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
        String sql = "UPDATE Auctions SET currentPrice = ?, status = ? WHERE auctionId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, auction.getCurrentPrice());
            pstmt.setString(2, auction.getStatus().name());
            pstmt.setString(3, auction.getAuctionId());

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

                Auction auction = new Auction(
                        auctionId,
                        item,
                        currentPrice,
                        status,
                        endTime,
                        new CopyOnWriteArrayList<>()
                );
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
}