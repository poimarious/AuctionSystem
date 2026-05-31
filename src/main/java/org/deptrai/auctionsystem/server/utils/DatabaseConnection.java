package org.deptrai.auctionsystem.server.utils;

import java.sql.*;

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
                    + "adminLevel INTEGER DEFAULT 0, "
                    + "balance REAL DEFAULT 0.0, "
                    + "isBanned INTEGER DEFAULT 0, "
                    + "banReason TEXT"
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
    String sqlCreateNotifications = "CREATE TABLE IF NOT EXISTS notifications ("
            + "notification_id TEXT PRIMARY KEY, "
            + "user_id TEXT, "
            + "message TEXT, "
            + "is_read INTEGER DEFAULT 0, "
            + "created_at TEXT"
            + ");";


    try (Connection conn = getConnection();
         Statement stmt = conn.createStatement()) {

      stmt.execute("PRAGMA foreign_keys = ON;"); // Turning on foreign keys in Sqlite

      // 1. Tạo bảng (Nếu chưa có)
      stmt.execute(sqlCreateUsers);
      stmt.execute(sqlCreateItems);
      stmt.execute(sqlCreateAuctions);
      stmt.execute(sqlCreateBids);
      stmt.execute(sqlCreateNotifications);

      // 2. Kiểm tra và nâng cấp Schema (Migration)
      updateSchema(conn);

      // Gieo hạt tài khoản Admin nếu cần
      seedDefaultAdmins(conn);

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

      // Kiểm tra cột adminLevel, isBanned, banReason
      if (!metaData.getColumns(null, null, "Users", "adminLevel").next()) {
        try (Statement stmt = conn.createStatement()) {
          stmt.execute("ALTER TABLE Users ADD COLUMN adminLevel INTEGER DEFAULT 0;");
          stmt.execute("ALTER TABLE Users ADD COLUMN isBanned INTEGER DEFAULT 0;");
          stmt.execute("ALTER TABLE Users ADD COLUMN banReason TEXT;");
          System.out.println("[SCHEMA UPDATE] Thêm cột 'adminLevel', 'isBanned', 'banReason'.");
        }
      }
    } catch (SQLException e) {
      System.out.println("[LỖI SCHEMA] Không thể cập nhật cấu trúc DB: " + e.getMessage());
    }
  }

  // ự động tạo dữ liệu mẫu nếu DB trống
  private static void seedDefaultAdmins(Connection conn) {
    String checkSql = "SELECT count(*) FROM Users WHERE role = 'ADMIN'";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(checkSql)) {

      // Nếu đếm số lượng Admin == 0, tiến hành tạo mới
      if (rs.next() && rs.getInt(1) == 0) {
        String insertSql = "INSERT INTO Users (userId, username, password, email, role, adminLevel, balance, isBanned) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
          // ==========================================
          // 1. TẠO ADMIN LEVEL 2 (Quyền tối cao)
          // ==========================================
          pstmt.setString(1, java.util.UUID.randomUUID().toString());
          pstmt.setString(2, "admin2");
          pstmt.setString(3, "Admin2@123");
          pstmt.setString(4, "admin2@uet.edu.vn");
          pstmt.setString(5, "ADMIN");
          pstmt.setInt(6, 2); // Level 2
          pstmt.setDouble(7, 0.0);
          pstmt.setInt(8, 0);
          pstmt.addBatch();

          // ==========================================
          // 2. TẠO ADMIN LEVEL 1 (Mod kiểm duyệt)
          // ==========================================
          pstmt.setString(1, java.util.UUID.randomUUID().toString());
          pstmt.setString(2, "admin1");
          pstmt.setString(3, "Admin1@123");
          pstmt.setString(4, "admin1@uet.edu.vn");
          pstmt.setString(5, "ADMIN");
          pstmt.setInt(6, 1); // Level 1
          pstmt.setDouble(7, 0.0);
          pstmt.setInt(8, 0);
          pstmt.addBatch(); //

          // Thực thi Insert cả 2 tài khoản cùng lúc
          pstmt.executeBatch();
          System.out.println("[SEEDING] Đã tạo tự động 2 tài khoản Admin mặc định (admin1 và admin2)!");
        }
      }
    } catch (SQLException e) {
      System.out.println("Lỗi tạo Admin mặc định: " + e.getMessage());
    }
  }
}