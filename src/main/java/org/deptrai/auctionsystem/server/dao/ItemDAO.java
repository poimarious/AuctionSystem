package org.deptrai.auctionsystem.server.dao;

import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.items.*;
import org.deptrai.auctionsystem.shared.models.users.Seller;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ItemDAO {

  public boolean insertItem(Item item) {
    String sql =
            "INSERT INTO Items (itemId, name, description, startingPrice, category, sellerId, "
                    + "brand, warrantyMonths, artist, yearCreated, make, mileage, imageUrl) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      String id = (item.getItemId() != null) ? item.getItemId() : UUID.randomUUID().toString();
      item.setItemId(id);

      pstmt.setString(1, item.getItemId());
      pstmt.setString(2, item.getName());
      pstmt.setString(3, item.getDescription());
      pstmt.setDouble(4, item.getStartingPrice());
      pstmt.setString(5, item.getCategory());
      pstmt.setString(6, item.getSeller().getUserId());

      // Default missing variables to null and -1
      String brand = null;
      int warrantyMonths = -1;
      String artist = null;
      int yearCreated = -1;
      String make = null;
      int mileage = -1;

      switch (item) {
        case Electronics e -> {
          brand = e.getBrand();
          warrantyMonths = e.getWarrantyMonths();
        }
        case Art a -> {
          artist = a.getArtist();
          yearCreated = a.getYearCreated();
        }
        case Vehicle v -> {
          make = v.getMake();
          mileage = v.getMileage();
        }
        default -> {
        }
      }

      // Value of non-properties will be null/-1
      pstmt.setString(7, brand);
      pstmt.setInt(8, warrantyMonths);
      pstmt.setString(9, artist);
      pstmt.setInt(10, yearCreated);
      pstmt.setString(11, make);
      pstmt.setInt(12, mileage);
      pstmt.setString(13, item.getImageUrl());

      pstmt.executeUpdate();
      return true;
    } catch (SQLException e) {
      System.out.println("Lỗi lưu Item: " + e.getMessage());
      return false;
    }
  }

  // Use for Seller only
  public Item getItemById(String itemId) {
    String sql = "SELECT * FROM Items WHERE itemId = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, itemId);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        String name = rs.getString("name");
        String description = rs.getString("description");
        double startingPrice = rs.getDouble("startingPrice");
        String category = rs.getString("category");
        String sellerId = rs.getString("sellerId");
        String imageUrl = rs.getString("imageUrl");

        UserDAO userDAO = new UserDAO();
        Seller seller = (Seller) userDAO.getUserById(sellerId);

        // Using Factory Method to create new Item
        ItemFactory factory = switch (category) {
          case "Electronics" -> new ElectronicsFactory();
          case "Art" -> new ArtFactory();
          case "Vehicle" -> new VehicleFactory();
          default -> null;
        };

        if (factory != null) {
          Item item = factory.createItem(name, description, startingPrice, seller);
          item.setItemId(itemId);
          item.setImageUrl(imageUrl);

          if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
              File imgFile = new File(imageUrl);
              if (imgFile.exists()) {
                byte[] bytes = Files.readAllBytes(imgFile.toPath());
                item.setImageBytes(bytes);
              }
            } catch (Exception e) {
              System.out.println("Cảnh báo: Không thể nạp file ảnh vào RAM: " + imageUrl);
            }
          }

          switch (item) {
            case Electronics electronics -> electronics
                    .setBrand(rs.getString("brand"))
                    .setWarrantyMonths(rs.getInt("warrantyMonths"));
            case Art art -> art.setArtist(rs.getString("artist")).setYearCreated(rs.getInt("yearCreated"));
            case Vehicle vehicle -> vehicle.setMake(rs.getString("make")).setMileage(rs.getInt("mileage"));
            default -> {
            }
          }

          return item;
        }
      }
    } catch (SQLException e) {
      System.out.println("Lỗi tìm Item theo ID: " + e.getMessage());
    }
    return null;
  }
}
