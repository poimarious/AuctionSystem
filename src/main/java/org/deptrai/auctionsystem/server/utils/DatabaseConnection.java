package org.deptrai.auctionsystem.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class DatabaseConnection {
  // Database file name (sẽ tự động tạo trong thư mục project)
  private static final String URL = "jdbc:sqlite:auctionsystem.db";

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL);
  }

  public static void initializeDatabase() {
    String sqlCreateUsers =
        "CREATE TABLE IF NOT EXISTS Users ("
            + "userId TEXT PRIMARY KEY, "
            + "username TEXT UNIQUE NOT NULL, "
            + "password TEXT NOT NULL, "
            + "email TEXT, "
            + "role TEXT NOT NULL,"
            + "adminLevel INTEGER, "
            + "balance REAL DEFAULT 0.0"
            + ");";

    String sqlCreateItems =
        "CREATE TABLE IF NOT EXISTS Items ("
            + "itemId TEXT PRIMARY KEY, "
            + "name TEXT NOT NULL, "
            + "description TEXT, "
            + "startingPrice REAL, "
            + "category TEXT, "
            + "sellerId TEXT, "
            + "brand TEXT, "
            + "warrantyMonths INTEGER, "
            + "artist TEXT, "
            + "yearCreated INTEGER, "
            + "make TEXT, "
            + "mileage INTEGER, "
            + "imageUrl TEXT, "
            + "FOREIGN KEY(sellerId) REFERENCES Users(userId)"
            + ");";

    String sqlCreateAuctions =
        "CREATE TABLE IF NOT EXISTS Auctions ("
            + "auctionId TEXT PRIMARY KEY, "
            + "itemId TEXT, "
            + "currentPrice REAL, "
            + "status TEXT, "
            + "endTime TEXT, " // Lưu LocalDateTime dưới dạng chuỗi ISO
            + "FOREIGN KEY(itemId) REFERENCES Items(itemId)"
            + ");";

    String sqlCreateBids =
        "CREATE TABLE IF NOT EXISTS Bids ("
            + "bidId TEXT PRIMARY KEY, "
            + "bidderId TEXT, "
            + "auctionId TEXT, "
            + "amount REAL, "
            + "timestamp TEXT, "
            + "FOREIGN KEY(bidderId) REFERENCES Users(userId), "
            + "FOREIGN KEY(auctionId) REFERENCES Auctions(auctionId)"
            + ");";

    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement()) {

      stmt.execute("PRAGMA foreign_keys = ON;"); // Turning on foreign keys in Sqlite

      // 1. Tạo bảng (Nếu chưa có)
      stmt.execute(sqlCreateUsers);
      stmt.execute(sqlCreateItems);
      stmt.execute(sqlCreateAuctions);
      stmt.execute(sqlCreateBids);

      // 2. Kiểm tra và nâng cấp Schema (Migration)
      updateSchema(conn);

      System.out.println("Cơ sở dữ liệu đã sẵn sàng");

    } catch (SQLException e) {
      System.out.println("Lỗi khởi tạo DB: " + e.getMessage());
    }
  }

  // Hàm tự động Update Schema - Giữ nguyên dữ liệu cũ
  private static void updateSchema(Connection conn) {
    try {
      DatabaseMetaData metaData = conn.getMetaData();

      // Kiểm tra xem bảng Users đã có cột balance chưa
      ResultSet rs = metaData.getColumns(null, null, "Users", "balance");

      if (!rs.next()) {
        // Nếu ResultSet rỗng (nghĩa là chưa có cột balance), tiến hành ALTER TABLE
        try (Statement stmt = conn.createStatement()) {
          stmt.execute("ALTER TABLE Users ADD COLUMN balance REAL DEFAULT 0.0;");
          System.out.println("[SCHEMA UPDATE] Đã tự động thêm cột 'balance' vào bảng Users.");
        }
      }
      ResultSet rsImageUrl = metaData.getColumns(null, null, "Items", "imageUrl");
      if (!rsImageUrl.next()) {
          try (Statement stmt = conn.createStatement()) {
              stmt.execute("ALTER TABLE Items ADD COLUMN imageUrl TEXT;");
              System.out.println("[SCHEMA UPDATE] Đã tự động thêm cột 'imageUrl' vào bảng Items.");
          }
      }
    } catch (SQLException e) {
      System.out.println("[LỖI SCHEMA] Không thể cập nhật cấu trúc DB: " + e.getMessage());
    }
  }
}