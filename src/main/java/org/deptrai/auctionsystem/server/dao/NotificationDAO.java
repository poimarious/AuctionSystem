package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;

public class NotificationDAO {

  public void insertNotification(String userId, String message) {
    String sql = "INSERT INTO notifications (notification_id, user_id, message, created_at) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, UUID.randomUUID().toString());
      pstmt.setString(2, userId);
      pstmt.setString(3, message);
      pstmt.setString(4, java.time.LocalDateTime.now().toString());

      pstmt.executeUpdate();
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
      System.out.println("Đã xóa sạch thông báo offline của user: " + userId);
    } catch (Exception e) {
      System.err.println("Lỗi xóa thông báo dưới DB: " + e.getMessage());
    }
  }
}