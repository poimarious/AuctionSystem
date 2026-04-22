package org.deptrai.auctionsystem.dao;

import org.deptrai.auctionsystem.models.users.Admin;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserDAO {

    public boolean insertUser(User user, String role) {
        String sql = "INSERT INTO Users (userId, username, password, email, role, adminLevel) VALUES (?, ?, ?, ?, ?, ?)";

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

                int adminLevel = rs.getInt("adminLevel"); // Currently null if not Admin

                switch (role) {
                    case "BIDDER":
                        return new Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());

                    case "SELLER":
                        Seller seller = new Seller(userId, username, pass, email);
                        seller.setListedItems(new ArrayList<>());
                        return seller;

                    case "ADMIN":
                        return new Admin(userId, username, pass, email, adminLevel);

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

                int adminLevel = rs.getInt("adminLevel"); // Currently null if not Admin

                switch (role) {
                    case "BIDDER":
                        return new Bidder(userId, username, pass, email, new CopyOnWriteArrayList<>());
                    case "SELLER":
                        Seller seller = new Seller(userId, username, pass, email);
                        seller.setListedItems(new ArrayList<>());
                        return seller;
                    case "ADMIN":
                        return new Admin(userId, username, pass, email, adminLevel);
                    default:
                        return null;
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi tìm User theo Username: " + e.getMessage());
        }
        return null;
    }
}