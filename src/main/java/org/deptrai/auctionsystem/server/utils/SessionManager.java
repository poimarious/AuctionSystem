package org.deptrai.auctionsystem.server.utils;

import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.models.auction.Auction;

public class SessionManager {
    private static SessionManager instance;

    // Lưu trữ người dùng đang đăng nhập hệ thống
    private User currentUser;

    // Lưu trữ phiên đấu giá được chọn khi người dùng bấm "Đặt giá ngay"
    private Auction selectedAuction;

    // Biến lắng nghe sự thay đổi số dư ví
    private Runnable balanceListener;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // --- QUẢN LÝ NGƯỜI DÙNG ---
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
        this.selectedAuction = null;
        this.balanceListener = null; // Xóa chuông khi đăng xuất
    }

    // --- QUẢN LÝ PHIÊN ĐẤU GIÁ ĐƯỢC CHỌN ---
    public void setSelectedAuction(Auction auction) {
        this.selectedAuction = auction;
    }

    public Auction getSelectedAuction() {
        return selectedAuction;
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
}