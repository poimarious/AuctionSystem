package org.deptrai.auctionsystem.utils;

import org.deptrai.auctionsystem.models.users.User;
import org.deptrai.auctionsystem.models.auction.Auction;

public class SessionManager {
    private static SessionManager instance;

    // Lưu trữ người dùng đang đăng nhập hệ thống
    private User currentUser;

    // Lưu trữ phiên đấu giá được chọn khi người dùng bấm "Đặt giá ngay"
    private Auction selectedAuction;

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
        this.selectedAuction = null; // Xóa luôn phiên đang chọn khi đăng xuất
    }

    // --- QUẢN LÝ PHIÊN ĐẤU GIÁ ĐƯỢC CHỌN (MỚI THÊM) ---
    public void setSelectedAuction(Auction auction) {
        this.selectedAuction = auction;
    }

    public Auction getSelectedAuction() {
        return selectedAuction;
    }

    public void clearSelectedAuction() {
        this.selectedAuction = null;
    }
}