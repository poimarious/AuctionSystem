package org.deptrai.auctionsystem.server.dao;

import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserDAO {

    public boolean insertUser(User user, String role) {
        String sql = "INSERT INTO Users (userId, username, password, email, role, adminLevel, balance) VALUES (?, ?, ?, ?, ?, ?, ?)";

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
                pstmt.setObject(6, null); // Setting non-admin users to null adminLevel
            }
            pstmt.setDouble(7, user.getBalance()); // Everyone has a balance

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Lỗi lưu User: " + e.getMessage());
            return false;
        }
    }

    public User getUserById(String userId) {
        String sql = "SELECT * FROM Users WHERE userId = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                String username = rs.getString("username");
                String pass = rs.getString("password");
                String email = rs.getString("email");
                double balance = rs.getDouble("balance");

                int adminLevel = rs.getInt("adminLevel"); // Currently null if not Admin

                switch (role) {
                    case "BIDDER":
                        Bidder bidder = new Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());
                        bidder.setBalance(balance);
                        return bidder;

                    case "SELLER":
                        Seller seller = new Seller(userId, username, pass, email);
                        seller.setListedItems(new ArrayList<>());
                        seller.setBalance(balance);
                        return seller;

                    case "ADMIN":
                        Admin admin = new Admin(userId, username, pass, email, adminLevel);
                        admin.setBalance(balance);
                        return admin;

                    default:
                        return null;
                }
            }
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

            if (rs.next()) {
                String role = rs.getString("role");
                String userId = rs.getString("userId");
                String pass = rs.getString("password");
                String email = rs.getString("email");
                double balance = rs.getDouble("balance");

                int adminLevel = rs.getInt("adminLevel"); // Currently null if not Admin

                switch (role) {
                    case "BIDDER":
                        Bidder bidder = new Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());
                        bidder.setBalance(balance);
                        return bidder;

                    case "SELLER":
                        Seller seller = new Seller(userId, username, pass, email);
                        seller.setListedItems(new ArrayList<>());
                        seller.setBalance(balance);
                        return seller;

                    case "ADMIN":
                        Admin admin = new Admin(userId, username, pass, email, adminLevel);
                        admin.setBalance(balance);
                        return admin;

                    default:
                        return null;
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tìm User theo Username: " + e.getMessage());
        }
        return null;
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
}