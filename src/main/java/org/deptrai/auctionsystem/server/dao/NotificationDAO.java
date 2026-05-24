package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDAO {

  private static final Logger logger = LoggerFactory.getLogger(NotificationDAO.class);

  public void insertNotification(String userId, String message) {
    String InsertSql = "INSERT INTO notifications (notification_id, user_id, message, created_at) VALUES (?, ?, ?, ?)";
    String cleanupSql = "DELETE FROM notifications WHERE user_id = ? AND notification_id NOT IN (" +
            "SELECT notification_id FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 10)";
    try (Connection conn = DatabaseConnection.getConnection()) {
      try (PreparedStatement pstmtInsert = conn.prepareStatement(InsertSql)) {
        pstmtInsert.setString(1, UUID.randomUUID().toString());
        pstmtInsert.setString(2, userId);
        pstmtInsert.setString(3, message);
        pstmtInsert.setString(4, java.time.LocalDateTime.now().toString());
        pstmtInsert.executeUpdate();
      }


      try (PreparedStatement pstmtDelete = conn.prepareStatement(cleanupSql)) {
        pstmtDelete.setString(1, userId);
        pstmtDelete.setString(2, userId);
        pstmtDelete.executeUpdate();
      }


    } catch (Exception e) {
      System.err.println("Lỗi lưu thông báo vào DB: " + e.getMessage());
    }
  }

  public List<String> getUnreadNotifications(String userId) {
    List<String> list = new ArrayList<>();
    String sql = "SELECT message FROM notifications WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, userId);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        list.add(rs.getString("message"));
      }
    } catch (Exception e) {
      System.err.println("Lỗi tải thông báo từ DB: " + e.getMessage());
    }
    return list;
  }

  public void deleteNotificationsByUserId(String userId) {
    String sql = "DELETE FROM notifications WHERE user_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, userId);
      pstmt.executeUpdate();
    } catch (Exception e) {
       logger.info("Lỗi xóa thông báo dưới DB: " + e.getMessage());
    }
  }
}