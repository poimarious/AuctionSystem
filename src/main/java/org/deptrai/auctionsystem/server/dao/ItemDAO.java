package org.deptrai.auctionsystem.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.items.Art;
import org.deptrai.auctionsystem.shared.models.items.ArtFactory;
import org.deptrai.auctionsystem.shared.models.items.Electronics;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.ItemFactory;
import org.deptrai.auctionsystem.shared.models.items.Vehicle;
import org.deptrai.auctionsystem.shared.models.items.VehicleFactory;
import org.deptrai.auctionsystem.shared.models.users.Seller;

public class ItemDAO {

  public boolean insertItem(Item item) {
    String sql =
        "INSERT INTO Items (itemId, name, description, startingPrice, category, sellerId, "
            + "brand, warrantyMonths, artist, yearCreated, make, mileage) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

      if (item instanceof Electronics e) {
        brand = e.getBrand();
        warrantyMonths = e.getWarrantyMonths();
      } else if (item instanceof Art a) {
        artist = a.getArtist();
        yearCreated = a.getYearCreated();
      } else if (item instanceof Vehicle v) {
        make = v.getMake();
        mileage = v.getMileage();
      }

      // Value of non-properties will be null/-1
      pstmt.setString(7, brand);
      pstmt.setInt(8, warrantyMonths);
      pstmt.setString(9, artist);
      pstmt.setInt(10, yearCreated);
      pstmt.setString(11, make);
      pstmt.setInt(12, mileage);

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

        UserDAO userDAO = new UserDAO();
        Seller seller = (Seller) userDAO.getUserById(sellerId);

        // Using Factory Method to create new Item
        ItemFactory factory = null;
        switch (category) {
          case "Electronics":
            factory = new ElectronicsFactory();
            break;
          case "Art":
            factory = new ArtFactory();
            break;
          case "Vehicle":
            factory = new VehicleFactory();
            break;
        }

        if (factory != null) {
          Item item = factory.createItem(name, description, startingPrice, seller);
          item.setItemId(itemId);

          if (item instanceof Electronics) {
            ((Electronics) item)
                .setBrand(rs.getString("brand"))
                .setWarrantyMonths(rs.getInt("warrantyMonths"));
          } else if (item instanceof Art) {
            ((Art) item).setArtist(rs.getString("artist")).setYearCreated(rs.getInt("yearCreated"));
          } else if (item instanceof Vehicle) {
            ((Vehicle) item).setMake(rs.getString("make")).setMileage(rs.getInt("mileage"));
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
