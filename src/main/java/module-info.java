module org.deptrai.auctionsystem {
  // === CÁC THƯ VIỆN LÕI HỆ THỐNG CẦN DÙNG ===
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires org.slf4j;

  // === CẤU HÌNH CHO PHÍA CLIENT (GIAO DIỆN) ===
  // 1. Mở package controllers để JavaFX có quyền "bơm" dữ liệu vào các biến @FXML
  opens org.deptrai.auctionsystem.client.controllers to
          javafx.fxml;

  // 2. Mở package client (nơi chứa ClientApplication) để JavaFX khởi động ứng dụng
  opens org.deptrai.auctionsystem.client to
          javafx.graphics,
          javafx.fxml;

  // 3. Xuất các package để trình biên dịch và JavaFX có thể nhìn thấy
  exports org.deptrai.auctionsystem.client;
  exports org.deptrai.auctionsystem.client.controllers;

  // === CẤU HÌNH CHO PHÍA DỮ LIỆU CHUNG (SHARED) ===
  // Cần thiết khi bạn muốn đẩy các Object này lên TableView hoặc ListView của JavaFX
  exports org.deptrai.auctionsystem.shared.models.users;
  exports org.deptrai.auctionsystem.shared.models.items;
  exports org.deptrai.auctionsystem.shared.models.auction;
  exports org.deptrai.auctionsystem.shared.models.bid;

  // === CẤU HÌNH CHO PHÍA SERVER ===
  // Giúp IDE có thể chạy được file ServerMain.java
  exports org.deptrai.auctionsystem.server;
}
