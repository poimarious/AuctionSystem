package org.deptrai.auctionsystem;

import org.deptrai.auctionsystem.models.auction.Auction;
import org.deptrai.auctionsystem.models.auction.AuctionManager;
import org.deptrai.auctionsystem.models.items.Electronics;
import org.deptrai.auctionsystem.models.users.Bidder;
import org.deptrai.auctionsystem.models.users.Seller;

import org.deptrai.auctionsystem.views.HelloApplication; // UI stuff in main

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== 1. KHỞI TẠO DỮ LIỆU ===");
        Seller seller = new Seller("ShopDoCongNghe", "pass123", "seller@gmail.com");
        Electronics laptop = new Electronics("Macbook Pro", "Chip M3 Max", 1500.0, seller);
        laptop.setBrand("Apple").setWarrantyMonths(12);

        System.out.println("\n=== 2. TẠO PHIÊN ĐẤU GIÁ ===");
        // Thời gian kết thúc giả định là 1 tiếng sau
        Auction auction = AuctionManager.getInstance().createAuction(laptop, LocalDateTime.now().plusHours(1));

        System.out.println("\n=== 3. NGƯỜI DÙNG VÀO PHÒNG & THEO DÕI (OBSERVER) ===");
        Bidder bidder1 = new Bidder("PhuGia", "pass", "rich@gmail.com");
        Bidder bidder2 = new Bidder("ThoSanSale", "pass", "hunter@gmail.com");

        // Đăng ký nhận thông báo
        auction.attach(bidder1);
        auction.attach(bidder2);

        System.out.println("\n=== 4. BẮT ĐẦU PHIÊN ĐẤU GIÁ ===");
        auction.startAuction();

        System.out.println("\n=== 5. TIẾN HÀNH ĐẶT GIÁ ===");
        // ThoSanSale đặt giá bằng với giá khởi điểm (1500) -> Sẽ thất bại vì phải lớn hơn hiện tại
        System.out.println("-> ThoSanSale thử đặt 1500.0");
        bidder2.placeBid(auction, 1500.0);

        // PhuGia đặt giá hợp lệ
        System.out.println("\n-> PhuGia đặt 1600.0");
        bidder1.placeBid(auction, 1600.0);

        // ThoSanSale cố gắng giật lại
        System.out.println("\n-> ThoSanSale đặt 1650.0");
        bidder2.placeBid(auction, 1650.0);

        System.out.println("\n=== 6. KẾT THÚC PHIÊN ===");
        auction.closeAuction();

        HelloApplication.main(args);
    }
}