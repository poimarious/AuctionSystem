package org.deptrai.auctionsystem.client;

import java.time.LocalDateTime;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.items.Electronics;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;

public class ClientMain {
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
//        System.out.println("-> ThoSanSale thử đặt 1500.0");
//        bidder2.placeBid(auction, 1500.0);

        // PhuGia đặt giá hợp lệ
        System.out.println("\n-> PhuGia đặt 1600.0");
        bidder1.placeBid(auction, 1600.0);

        // ThoSanSale cố gắng giật lại
        System.out.println("\n-> ThoSanSale đặt 1650.0");
        bidder2.placeBid(auction, 1650.0);

        System.out.println("\n=== 6. KẾT THÚC PHIÊN ===");
        auction.closeAuction();




//        // DATABASE TEST
//        System.out.println("=== 1. KHỞI TẠO CƠ SỞ DỮ LIỆU ===");
//        DatabaseConnection.initializeDatabase();
//
//        // Thêm số ngẫu nhiên vào tên để chạy test nhiều lần không bị lỗi trùng Username (UNIQUE)
//        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
//        String sellerName = "ShopCongNghe_" + suffix;
//        String bidderName = "PhuGia_" + suffix;
//
//        System.out.println("\n=== 2. TEST LƯU USER ===");
//        UserDAO userDAO = new UserDAO();
//
//        Seller seller = new Seller(null, sellerName, "pass123", "shop@gmail.com");
//        userDAO.insertUser(seller, "SELLER");
//        System.out.println("Đã lưu Seller: " + seller.getUsername());
//
//        Bidder bidder = new Bidder(null, bidderName, "pass123", "phugia@gmail.com", new CopyOnWriteArrayList<>());
//        userDAO.insertUser(bidder, "BIDDER");
//        System.out.println("Đã lưu Bidder: " + bidder.getUsername());
//
//        System.out.println("\n=== 3. TEST LƯU ITEM ===");
//        ItemDAO itemDAO = new ItemDAO();
//        ItemFactory elecFactory = new ElectronicsFactory();
//
//        Item laptop = elecFactory.createItem("Macbook Pro M3", "Laptop xịn", 1500.0, seller);
//        ((Electronics) laptop).setBrand("Apple").setWarrantyMonths(24);
//        itemDAO.insertItem(laptop);
//        System.out.println("Đã lưu Item: " + laptop.getName() + " (ID: " + laptop.getItemId() + ")");
//
//        System.out.println("\n=== 4. TEST LƯU AUCTION ===");
//        AuctionDAO auctionDAO = new AuctionDAO();
//        Auction auction = new Auction(null, laptop, laptop.getStartingPrice(), AuctionStatus.RUNNING, LocalDateTime.now().plusDays(1), new CopyOnWriteArrayList<>());
//        auctionDAO.insertAuction(auction);
//        System.out.println("Đã lưu Auction với ID: " + auction.getAuctionId());
//
//        System.out.println("\n=== 5. TEST LƯU BID ===");
//        BidDAO bidDAO = new BidDAO();
//        Bid bid = new Bid(null, bidder, auction, 1600.0, LocalDateTime.now());
//        bidDAO.insertBid(bid);
//        System.out.println("Đã lưu Bid của: " + bidder.getUsername() + " với giá: $" + bid.getAmount());
//
//        System.out.println("\n=== 6. TEST ĐỌC NGƯỢC DỮ LIỆU TỪ DB LÊN RAM ===");
//
//        // 6.1. Thử đọc User
//        User loadedUser = userDAO.getUserByUsername(bidderName);
//        if (loadedUser != null) {
//            System.out.println("-> Đọc User thành công: " + loadedUser.getUsername() + " | Class thực tế: " + loadedUser.getClass().getSimpleName());
//        }
//
//        // 6.2. Thử đọc Auction (nó sẽ tự động nối chuỗi gọi ItemDAO, UserDAO và BidDAO)
//        Auction loadedAuction = auctionDAO.getAuctionById(auction.getAuctionId());
//        if (loadedAuction != null) {
//            System.out.println("-> Đọc Auction thành công!");
//            System.out.println("   + Tên sản phẩm: " + loadedAuction.getItem().getName());
//            System.out.println("   + Người bán: " + loadedAuction.getItem().getSeller().getUsername());
//            System.out.println("   + Số lượng Bid trong lịch sử: " + loadedAuction.getBids().size());
//            if (!loadedAuction.getBids().isEmpty()) {
//                System.out.println("   + Người đang dẫn đầu: " + loadedAuction.getBids().get(0).getBidder().getUsername() + " ($" + loadedAuction.getBids().get(0).getAmount() + ")");
//            }
//        }
//
//        System.out.println("\n=== TEST HOÀN TẤT VÀ THÀNH CÔNG ===");
    }
}