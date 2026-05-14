package org.deptrai.auctionsystem.client.utils;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.User;

public class SessionManager {
  private static SessionManager instance;

  // Lưu trữ người dùng đang đăng nhập hệ thống
  private User currentUser;

  // Lưu trữ phiên đấu giá được chọn khi người dùng bấm "Đặt giá ngay"
  private Auction selectedAuction;

  // Biến lắng nghe sự thay đổi số dư ví
  private Runnable balanceListener;

  private final ObservableList<String> notifications = FXCollections.observableArrayList();

  private SessionManager() {}

  public static SessionManager getInstance() {
    if (instance == null) {
      instance = new SessionManager();
    }
    return instance;
  }

  public User getCurrentUser() {
    return currentUser;
  }

  // --- QUẢN LÝ NGƯỜI DÙNG ---
  public void setCurrentUser(User user) {
    this.currentUser = user;
  }

  public void logout() {
    this.currentUser = null;
    this.selectedAuction = null;
    this.balanceListener = null; // Xóa chuông khi đăng xuất
  }

  public Auction getSelectedAuction() {
    return selectedAuction;
  }

  // --- QUẢN LÝ PHIÊN ĐẤU GIÁ ĐƯỢC CHỌN ---
  public void setSelectedAuction(Auction auction) {
    this.selectedAuction = auction;
  }

  public void clearSelectedAuction() {
    this.selectedAuction = null;
  }

  // --- HỆ THỐNG LẮNG NGHE SỐ DƯ (MỚI THÊM) ---
  public void setBalanceListener(Runnable listener) {
    this.balanceListener = listener;
  }

  public void notifyBalanceChanged() {
    if (balanceListener != null) {
      balanceListener.run();
    }
  }

  public ObservableList<String> getNotifications() {
    return notifications;
  }

  public void addNotification(String msg) {
    Platform.runLater(() -> {
      notifications.add(0, msg);
    });
  }
}
