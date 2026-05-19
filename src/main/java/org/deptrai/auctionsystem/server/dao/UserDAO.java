package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;

public class UserDAO {

  public boolean insertUser(User user, String role) {
    String sql =
        "INSERT INTO Users (userId, username, password, email, role, adminLevel, balance, isBanned, banReason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      String id = (user.getUserId() != null) ? user.getUserId() : UUID.randomUUID().toString();
      user.setUserId(id);

      pstmt.setString(1, user.getUserId());
      pstmt.setString(2, user.getUsername());
      pstmt.setString(3, user.getPassword());
      pstmt.setString(4, user.getEmail());
      pstmt.setString(5, role);

      // Admin's own variable adminLevel
      if (user instanceof Admin) {
        pstmt.setInt(6, ((Admin) user).getAdminLevel());
      } else {
        pstmt.setObject(6, 0); // Setting non-admin users to 0
      }
      pstmt.setDouble(7, user.getBalance()); // Everyone has a balance
      pstmt.setInt(8, user.isBanned() ? 1 : 0); // SQLite lưu boolean bằng 0 và 1
      pstmt.setString(9, user.getBanReason());

      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.out.println("Lỗi lưu User: " + e.getMessage());
      return false;
    }
  }

  private User mapResultSetToUser(ResultSet rs) throws SQLException {
    String userId = rs.getString("userId");
    String username = rs.getString("username");
    String pass = rs.getString("password");
    String email = rs.getString("email");
    String role = rs.getString("role");
    double balance = rs.getDouble("balance");
    int adminLevel = rs.getInt("adminLevel");

    // Lấy trạng thái Ban từ Database
    boolean isBanned = rs.getInt("isBanned") == 1;
    String banReason = rs.getString("banReason");

    User mappedUser = null;

    switch (role) {
      case "BIDDER":
        mappedUser = new Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());
        break;
      case "SELLER":
        Seller seller = new Seller(userId, username, pass, email);
        seller.setListedItems(new ArrayList<>());
        mappedUser = seller;
        break;
      case "ADMIN":
        mappedUser = new Admin(userId, username, pass, email, adminLevel);
        break;
    }

    if (mappedUser != null) {
      mappedUser.setBalance(balance);
      mappedUser.setBanned(isBanned);
      mappedUser.setBanReason(banReason);
    }
    return mappedUser;
  }

  public User getUserById(String userId) {
    String sql = "SELECT * FROM Users WHERE userId = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, userId);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) return mapResultSetToUser(rs);
    } catch (SQLException e) {
      System.out.println("Lỗi tìm User theo ID: " + e.getMessage());
    }
    return null;
  }

  // For login stuff
  public User getUserByUsername(String username) {
    String sql = "SELECT * FROM Users WHERE username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) return mapResultSetToUser(rs);
    } catch (SQLException e) {
      System.out.println("Lỗi tìm User theo Username: " + e.getMessage());
    }
    return null;
  }

  public boolean banUser(String userId, String reason) {
    // isBanned = 1 nghĩa là đã bị khóa
    String sql = "UPDATE Users SET isBanned = 1, banReason = ? WHERE userId = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, reason);
      pstmt.setString(2, userId);

      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0;

    } catch (SQLException e) {
      System.out.println("Lỗi Ban User trong DB: " + e.getMessage());
      return false;
    }
  }

  public boolean unbanUser(String userId) {
    String sql = "UPDATE Users SET isBanned = 0, banReason = NULL WHERE userId = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, userId);
      return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
      return false;
    }
  }

  public boolean isUsernameTaken(String username) {
    String sql = "SELECT 1 FROM Users WHERE username = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      // Nếu rs.next() là true tức là đã có dòng dữ liệu tồn tại
      return rs.next();

    } catch (SQLException e) {
      System.out.println("Lỗi kiểm tra trùng lặp Username: " + e.getMessage());
      // Trả về true (coi như đã tồn tại) để an toàn, chặn không cho tạo mới nếu DB đang lỗi
      return true;
    }
  }

  public boolean isEmailTaken(String email) {
    String sql = "SELECT 1 FROM Users WHERE email = ?";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, email);
      ResultSet rs = pstmt.executeQuery();

      // Nếu tìm thấy bất kỳ dòng nào khớp email, trả về true
      return rs.next();

    } catch (SQLException e) {
      System.out.println("Lỗi kiểm tra trùng lặp Email: " + e.getMessage());
      // Trả về true để an toàn (ngăn chặn tạo tài khoản nếu DB đang gặp sự cố)
      return true;
    }
  }

  public boolean updateBalance(String userId, double newBalance) {
    String sql = "UPDATE Users SET balance = ? WHERE userId = ?";
    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setDouble(1, newBalance);
      pstmt.setString(2, userId);
      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      return false;
    }
  }


  public boolean updatePassword(String userId, String newPassword) {
    String sql = "UPDATE Users SET password = ? WHERE userId = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, newPassword);
      pstmt.setString(2, userId);

      int rowsAffected = pstmt.executeUpdate();
      return rowsAffected > 0; // Trả về true nếu có ít nhất 1 dòng được cập nhật thành công

    } catch (SQLException e) {
      System.out.println("Lỗi cập nhật mật khẩu DB: " + e.getMessage());
      return false;
    }
  }

  public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM Users";

    try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        User user = mapResultSetToUser(rs);
        if (user != null) {
          users.add(user);
        }
      }
    } catch (SQLException e) {
      System.out.println("Lỗi lấy danh sách toàn bộ User: " + e.getMessage());
    }
    return users;
  }
}
