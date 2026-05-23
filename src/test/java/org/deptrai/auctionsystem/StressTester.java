package org.deptrai.auctionsystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.network.Message;

public class StressTester {

    private static final int NUM_USERS = 500;
    private static final int NUM_AUCTIONS = 5000;
    private static final int REQUESTS_PER_USER = 10; // Mỗi user bắn 10 phát liên tiếp

    // Thống kê kết quả
    private static AtomicInteger successCount = new AtomicInteger(0);
    private static AtomicInteger failCount = new AtomicInteger(0);
    private static AtomicInteger timeoutCount = new AtomicInteger(0);
    private static AtomicInteger totalLatency = new AtomicInteger(0);

    private static List<String> userIds = new ArrayList<>();
    private static List<String> auctionIds = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== KHỞI ĐỘNG CỖ MÁY STRESS TEST ===");
        DatabaseConnection.initializeDatabase();

        // BƯỚC 1: NẠP ĐẠN (Chỉ mở dòng này ở Lần chạy 1, chạy xong thì comment lại)
        // seedData();

        //mở servermain ở lần chạy 2 trước rồi chạy file này
        loadDataForAttack(); //Đóng ở lần 1

        System.out.println("\n=== BẮT ĐẦU TẤN CÔNG SERVER ===");
        long startTime = System.currentTimeMillis();
        startAttack();//đóng ở lần 1
        long endTime = System.currentTimeMillis();

        printReport(endTime - startTime);
    }

    private static void seedData() {
        System.out.println(">> Đang nạp " + NUM_USERS + " Users và " + NUM_AUCTIONS + " Auctions vào Database...");
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        // ================= SỬA LỖI TẠI ĐÂY =================
        // Tạo một mã ID duy nhất cho lần chạy này dựa trên thời gian thực
        long batchId = System.currentTimeMillis();

        String sellerName = "stress_seller_" + batchId;
        Seller stressSeller = new Seller(null, sellerName, "Pass123!", sellerName + "@stress.com");
        userDAO.insertUser(stressSeller, "SELLER");
        stressSeller = (Seller) userDAO.getUserByUsername(sellerName);

        // Tạo 500 User với tên không bao giờ trùng lặp
        for (int i = 0; i < NUM_USERS; i++) {
            String userName = "stress_user_" + batchId + "_" + i;
            Bidder b = new Bidder(null, userName, "Pass123!", userName + "@stress.com", new ArrayList<>());
            userDAO.insertUser(b, "BIDDER");
            Bidder dbBidder = (Bidder) userDAO.getUserByUsername(userName);
            userDAO.updateBalance(dbBidder.getUserId(), 1000000.0); // Bơm tiền
            userIds.add(dbBidder.getUserId());
        }

        // Tạo 5000 Auction
        ElectronicsFactory factory = new ElectronicsFactory();
        for (int i = 0; i < NUM_AUCTIONS; i++) {
            Item item = factory.createItem("Đồ vật " + batchId + "_" + i, "Mô tả", 10.0, stressSeller);
            itemDAO.insertItem(item);

            Auction auction = new Auction(item, LocalDateTime.now().plusDays(1));
            auction.setAuctionId(java.util.UUID.randomUUID().toString());
            auction.setStatus(AuctionStatus.RUNNING);
            auctionDAO.insertAuction(auction);

            auctionIds.add(auction.getAuctionId());
        }
        // ===================================================

        System.out.println(">> Nạp dữ liệu hoàn tất!");
    }

    private static void loadDataForAttack() {
        System.out.println(">> Đang nạp danh sách ID từ Database để chuẩn bị tấn công...");
        try (java.sql.Connection conn = org.deptrai.auctionsystem.server.utils.DatabaseConnection.getConnection()) {

            // SỬA TẠI ĐÂY: Dùng đúng tên cột 'userId' và 'Users' theo UserDAO.java
            java.sql.PreparedStatement psUser = conn.prepareStatement("SELECT userId FROM Users WHERE role = 'BIDDER' LIMIT ?");
            psUser.setInt(1, NUM_USERS);
            java.sql.ResultSet rsUser = psUser.executeQuery();
            while(rsUser.next()) {
                userIds.add(rsUser.getString("userId"));
            }

            // SỬA TẠI ĐÂY: Dùng đúng tên cột 'auctionId' và 'Auctions' theo AuctionDAO.java
            java.sql.PreparedStatement psAuction = conn.prepareStatement("SELECT auctionId FROM Auctions LIMIT ?");
            psAuction.setInt(1, NUM_AUCTIONS);
            java.sql.ResultSet rsAuction = psAuction.executeQuery();
            while(rsAuction.next()) {
                auctionIds.add(rsAuction.getString("auctionId"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LỖI: Không thể lấy dữ liệu từ Database. Đảm bảo cấu hình DB đúng!");
        }

        System.out.println(">> Đã tải thành công " + userIds.size() + " Users và " + auctionIds.size() + " Auctions từ DB!");
    }
    private static void startAttack() {
        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_USERS);
        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_USERS);
        Random random = new Random();

        for (int i = 0; i < NUM_USERS; i++) {
            final String myUserId = userIds.get(i);

            threadPool.submit(() -> {
                try {
                    // Mở Socket tới cổng 5000 (Cổng chạy app chính)
                    Socket socket = new Socket("localhost", 5000);
                    // QUAN TRỌNG: Thiết lập Timeout là 5 giây.
                    // Nếu Server "Treo" quá 5 giây không trả lời, ném lỗi ngay!
                    socket.setSoTimeout(5000);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                    readyLatch.await(); // Nín thở đợi mọi người vào vị trí

                    for (int req = 0; req < REQUESTS_PER_USER; req++) {
                        long pingStart = System.currentTimeMillis();

                        try {
                            // Chọn ngẫu nhiên 1 hành động:
                            // 20% là tải danh sách 5000 món đồ (Rất tốn RAM/Mạng của Server)
                            // 80% là đặt giá ngẫu nhiên (Tốn CPU khóa đồng bộ)
                            Message request;
                            if (random.nextInt(100) < 20) {
                                request = new Message("GET_ALL_AUCTIONS", myUserId);
                            } else {
                                String randomAuction = auctionIds.get(random.nextInt(NUM_AUCTIONS));
                                request = new Message("PLACE_BID", new Object[]{randomAuction, myUserId, 100.0 + random.nextInt(500)});
                            }

                            out.writeObject(request);
                            out.flush();

                            Message response = (Message) in.readObject();

                            long pingEnd = System.currentTimeMillis();
                            totalLatency.addAndGet((int)(pingEnd - pingStart));

                            if ("SUCCESS".equals(response.getStatus())) {
                                successCount.incrementAndGet();
                            } else {
                                failCount.incrementAndGet();
                                System.out.println("Bị Server từ chối: " + response.getData());
                            }

                        } catch (java.net.SocketTimeoutException e) {
                            timeoutCount.incrementAndGet(); // Bắt quả tang Server bị Treo
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            System.out.println("Văng mạng: Hệ điều hành từ chối kết nối!");// Bắt quả tang Server sập ngầm
                        }
                    }
                    socket.close();
                } catch (Exception e) {
                    failCount.incrementAndGet(); // Không thể kết nối (Server quá tải từ chối)
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        System.out.println(">> Phóng bão Request (" + (NUM_USERS * REQUESTS_PER_USER) + " yêu cầu) đồng loạt...");
        readyLatch.countDown(); // Bóp cò!

        try {
            doneLatch.await(); // Chờ khói tan
        } catch (InterruptedException e) {}
        threadPool.shutdown();
    }

    private static void printReport(long totalTimeMs) {
        int totalRequests = NUM_USERS * REQUESTS_PER_USER;
        int success = successCount.get();
        int fails = failCount.get();
        int timeouts = timeoutCount.get();
        int avgPing = success > 0 ? (totalLatency.get() / success) : 0;
        double throughput = (success / (totalTimeMs / 1000.0));

        System.out.println("\n============= BÁO CÁO STRESS TEST =============");
        System.out.println("Thời gian tấn công: " + (totalTimeMs / 1000.0) + " giây");
        System.out.println("Tổng Request đã bắn: " + totalRequests);
        System.out.println("Thành công (SUCCESS): " + success);
        System.out.println("Từ chối / Lỗi (FAIL/CRASH): " + fails);
        System.out.println("Treo / Không phản hồi (TIMEOUT): " + timeouts);
        System.out.println("-----------------------------------------------");
        System.out.println("Độ trễ trung bình (Ping): " + avgPing + " ms");
        System.out.println("Thông lượng (Throughput): " + String.format("%.2f", throughput) + " requests/sec");
        System.out.println("===============================================");

        if (timeouts > 0) {
            System.out.println("⚠️ CẢNH BÁO: Hệ thống của bạn đã bị TREO (Not Responding) " + timeouts + " lần!");
            System.out.println("=> Gợi ý: Kiểm tra lại các ổ khóa `synchronized`, có thể xảy ra hiện tượng thắt cổ chai (Bottleneck) làm các luồng phải đợi nhau quá lâu.");
        } else if (avgPing > 1000) {
            System.out.println("⚠️ CẢNH BÁO: Hệ thống đang bị LAG NẶNG (Ping > 1s).");
            System.out.println("=> Gợi ý: Gói tin gửi đi quá lớn (do 5000 object Auction), hoặc CSDL bị truy xuất quá dồn dập.");
        } else {
            System.out.println("✅ TUYỆT VỜI! Hệ thống của bạn chạy như một cỗ xe tăng, vượt qua bão tải nhẹ nhàng!");
        }
    }
}
