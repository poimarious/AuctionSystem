package org.deptrai.auctionsystem.dao;

import org.deptrai.auctionsystem.models.items.*;
import org.deptrai.auctionsystem.models.users.Seller;
import org.deptrai.auctionsystem.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ItemDAO {

    public boolean insertItem(Item item) {
        String sql = "INSERT INTO Items (itemId, name, description, startingPrice, category, sellerId, " +
                "brand, warrantyMonths, artist, yearCreated, make, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            String brand = null; int warrantyMonths = 0;
            String artist = null; int yearCreated = 0;
            String make = null; int mileage = 0;

            if (item instanceof Electronics) {
                Electronics e = (Electronics) item;
                brand = e.getBrand();
                warrantyMonths = e.getWarrantyMonths();
            } else if (item instanceof Art) {
                Art a = (Art) item;
                artist = a.getArtist();
                yearCreated = a.getYearCreated();
            } else if (item instanceof Vehicle) {
                Vehicle v = (Vehicle) item;
                make = v.getMake();
                mileage = v.getMileage();
            }

            // Value of non-properties will be NULL
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
                    case "Electronics": factory = new ElectronicsFactory(); break;
                    case "Art": factory = new ArtFactory(); break;
                    case "Vehicle": factory = new VehicleFactory(); break;
                }

                if (factory != null) {
                    Item item = factory.createItem(name, description, startingPrice, seller);
                    item.setItemId(itemId);

                    if (item instanceof Electronics) {
                        ((Electronics) item).setBrand(rs.getString("brand"))
                                .setWarrantyMonths(rs.getInt("warrantyMonths"));
                    } else if (item instanceof Art) {
                        ((Art) item).setArtist(rs.getString("artist"))
                                .setYearCreated(rs.getInt("yearCreated"));
                    } else if (item instanceof Vehicle) {
                        ((Vehicle) item).setMake(rs.getString("make"))
                                .setMileage(rs.getInt("mileage"));
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