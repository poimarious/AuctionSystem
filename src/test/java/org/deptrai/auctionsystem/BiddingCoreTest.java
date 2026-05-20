package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.ItemFactory;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.network.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BiddingCoreTest {

    private static ServerSocket serverSocket;
    private static Thread serverThread;

    private static Bidder testBidder;
    private static Auction targetAuction;

    @BeforeAll
    static void setUp() throws Exception {
        DatabaseConnection.initializeDatabase();
        UserDAO userDAO = new UserDAO();

        // ========================================================
        // TẠO DỮ LIỆU MẪU ĐỂ TEST ĐẶT GIÁ
        // ========================================================

        // 1. Tạo người bán (Seller)
        String sellerName = "bid_seller_" + System.currentTimeMillis();
        Seller dummySeller = new Seller(null, sellerName, "Test1234!", "bidseller@gmail.com");
        userDAO.insertUser(dummySeller, "SELLER");
        dummySeller = (Seller) userDAO.getUserByUsername(sellerName);

        // 2. Tạo vật phẩm với giá khởi điểm 100.0
        ItemFactory factory = new ElectronicsFactory();
        Item dummyItem = factory.createItem("Laptop Bidding Test", "Máy tính để test đặt giá", 100.0, dummySeller);
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.insertItem(dummyItem);

        // 3. Tạo phiên đấu giá
        targetAuction = new Auction(dummyItem, LocalDateTime.now().plusDays(1));
        targetAuction.setAuctionId(java.util.UUID.randomUUID().toString());
        targetAuction.setStatus(AuctionStatus.OPEN);
        AuctionDAO auctionDAO = new AuctionDAO();
        auctionDAO.insertAuction(targetAuction);

        // 4. Tạo người mua (Bidder) có sẵn số dư 1000.0 để thoải mái đặt giá
        String bidderName = "bid_buyer_" + System.currentTimeMillis();
        testBidder = new Bidder(null, bidderName, "Test1234!", "bidbuyer@gmail.com", new java.util.concurrent.CopyOnWriteArrayList<>());
        userDAO.insertUser(testBidder, "BIDDER");
        testBidder = (Bidder) userDAO.getUserByUsername(bidderName);
        userDAO.updateBalance(testBidder.getUserId(), 1000.0); // Nạp tiền

        // ========================================================

        // Nạp RAM
        AuctionManager.getInstance().loadAuctionsFromDatabase();

        // Khởi chạy Server
        serverSocket = new ServerSocket(5008);
        serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
            }
        });
        serverThread.start();

        // Client kết nối
        SocketClient.connect("localhost", 5008);
    }

    @AfterAll
    static void tearDown() throws Exception {
        SocketClient.disconnect();
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    // ==========================================
    // GIAI ĐOẠN 1: ĐẶT GIÁ (PLACE BID)
    // ==========================================
    @Test
    @Order(1)
    void testPlaceBid_ValidBid_ShouldSucceedAndIncreasePrice() {
        // 1. Tạo đối tượng Bid với giá 150.0 (Lớn hơn giá gốc 100.0)
        double bidAmount = 150.0;

        //Bid newBid = new Bid(null, testBidder, targetAuction, bidAmount, LocalDateTime.now());

        Object[] payload = new Object[] {
                targetAuction.getAuctionId(),
                testBidder.getUserId(),
                bidAmount
        };

        // 2. Gửi Request
        // Giả định Server của bạn đang nhận Payload là đối tượng Bid
        Message request = new Message("PLACE_BID", payload);
        Message response = SocketClient.sendRequest(request);

        System.out.println(response.getData());

        // 3. Kiểm tra kết quả trả về
        assertEquals("SUCCESS", response.getStatus(), "Đặt giá hợp lệ phải thành công");

        // 4. Kiểm tra sự đồng bộ trên RAM: Giá hiện tại của Auction phải tăng lên 150.0
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(targetAuction.getAuctionId());
        assertEquals(150.0, ramAuction.getCurrentPrice(), 0.001, "Giá của phiên đấu giá trên RAM phải được cập nhật");
        assertEquals(AuctionStatus.RUNNING, ramAuction.getStatus(), "Trạng thái phải chuyển thành RUNNING khi có người bid");
    }

    @Test
    @Order(2)
    void testPlaceBid_InvalidAuction_ShouldFail() {
        // Thử gửi một lượt đặt giá vào một Auction ID không tồn tại
        Auction fakeAuction = new Auction(targetAuction.getItem(), LocalDateTime.now());
        fakeAuction.setAuctionId("FAKE_AUCTION_ID_123");

        //Bid newBid = new Bid(null, testBidder, fakeAuction, 200.0, LocalDateTime.now());

        Object[] payload = new Object[] {
                fakeAuction.getAuctionId(),
                testBidder.getUserId(),
                200.0
        };

        Message request = new Message("PLACE_BID", payload);
        Message response = SocketClient.sendRequest(request);

        // Dựa theo logic ClientHandler, nếu lỗi DB hoặc Auction không hợp lệ, phải trả về FAIL
        assertEquals("FAIL", response.getStatus(), "Đặt giá vào phiên không tồn tại phải thất bại");
    }

    // ==========================================
    // GIAI ĐOẠN 2: LẤY LỊCH SỬ (GET BIDS HISTORY)
    // ==========================================

    @Test
    @Order(3)
    void testGetBidsHistory_ValidUser_ShouldReturnUpdatedList() {
        // Yêu cầu lấy lịch sử của testBidder (người vừa đặt giá thành công ở Test 1)
        Message request = new Message("GET_BIDS_HISTORY", testBidder.getUserId());
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus(), "Lấy lịch sử phải báo SUCCESS");

        @SuppressWarnings("unchecked")
        List<Bid> history = (List<Bid>) response.getData();

        // Kiểm tra tính chính xác của dữ liệu
        assertNotNull(history, "Lịch sử trả về không được null");
        assertEquals(1, history.size(), "Phải có đúng 1 lượt đặt giá trong lịch sử");
        assertEquals(150.0, history.get(0).getAmount(), 0.001, "Giá trị của lượt bid trong lịch sử phải là 150.0");
        assertEquals(targetAuction.getAuctionId(), history.get(0).getAuction().getAuctionId(), "ID phiên đấu giá phải khớp");
    }

    @Test
    @Order(4)
    void testGetBidsHistory_EmptyUserId_ShouldFail() {
        // Gửi ID rỗng (Lỗi Client)
        Message request = new Message("GET_BIDS_HISTORY", "   ");
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus(), "Phải báo lỗi nếu ID rỗng");
        assertEquals("ID người dùng không hợp lệ.", response.getData());
    }

    // ==========================================
    // GIAI ĐOẠN 3: KIỂM THỬ TƯƠNG TRANH (RACE CONDITION)
    // ==========================================

    @Test
    @Order(5)
    void testConcurrentBidding_ShouldHandleRaceCondition() throws Exception {
        // 1. Tạo một phiên đấu giá TÁCH BIỆT hoàn toàn để test đua lệnh
        ItemFactory factory = new ElectronicsFactory();
        Item raceItem = factory.createItem("Race Condition Phone", "Test", 10.0, targetAuction.getItem().getSeller());
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.insertItem(raceItem);

        Auction raceAuction = new Auction(raceItem, LocalDateTime.now().plusDays(1));
        raceAuction.setAuctionId(java.util.UUID.randomUUID().toString());
        raceAuction.setStatus(AuctionStatus.OPEN);
        AuctionDAO auctionDAO = new AuctionDAO();
        auctionDAO.insertAuction(raceAuction);
        AuctionManager.getInstance().addAuctionToMemory(raceAuction);

        // 2. Chuẩn bị 50 Client (Bằng đúng số lượng POOL_SIZE của ServerMain)
        int numberOfThreads = 50;
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(numberOfThreads);

        // List an toàn trong môi trường đa luồng để lưu các mức giá đặt THÀNH CÔNG
        List<Double> successfulBids = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            // Mỗi người đưa ra một mức giá khác nhau: 20, 30, 40... đến 510
            double bidAmount = 20.0 + (i * 10);

            new Thread(() -> {
                try (Socket s = new Socket("localhost", 5008);
                     java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(s.getOutputStream());
                     java.io.ObjectInputStream in = new java.io.ObjectInputStream(s.getInputStream())) {

                    Object[] payload = {raceAuction.getAuctionId(), testBidder.getUserId(), bidAmount};
                    Message request = new Message("PLACE_BID", payload);

                    // NÍN THỞ: Chờ hiệu lệnh từ luồng chính
                    readyLatch.await();

                    // BÓP CÒ: Gửi lên server
                    out.writeObject(request);
                    out.flush();

                    Message response = (Message) in.readObject();
                    if ("SUCCESS".equals(response.getStatus())) {
                        successfulBids.add(bidAmount);
                    }
                } catch (Exception e) {
                    // Bỏ qua lỗi ngắt mạng
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Cho các Thread thời gian kết nối Socket tới Server (Mất khoảng vài trăm mili-giây)
        Thread.sleep(1000);

        // PHÁT LỆNH: Cho phép 50 request lao vào Server cùng 1 lúc!
        readyLatch.countDown();

        // Đợi tối đa 10s cho Server xử lý xong toàn bộ
        boolean isCompleted = doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(isCompleted, "Test bị treo do Server không xử lý kịp 50 request!");

        // 3. Thống kê kết quả
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(raceAuction.getAuctionId());

        assertFalse(successfulBids.isEmpty(), "Phải có ít nhất 1 lượt bid thành công");

        double maxSuccessfulBid = successfulBids.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
        double minSuccessfulBid = successfulBids.stream().mapToDouble(Double::doubleValue).min().getAsDouble();

        System.out.println(">> [RACE CONDITION] Số Client đồng loạt gọi lệnh: " + numberOfThreads);
        System.out.println(">> [RACE CONDITION] Số lượt Bid qua cửa Server: " + successfulBids.size());
        System.out.println(">> [RACE CONDITION] Giá khởi điểm: 10.0");
        System.out.println(">> [RACE CONDITION] Mức giá THẤP NHẤT thành công: " + minSuccessfulBid);
        System.out.println(">> [RACE CONDITION] Mức giá CAO NHẤT thành công: " + maxSuccessfulBid);
        System.out.println(">> [RACE CONDITION] Giá chốt trên RAM Server: " + ramAuction.getCurrentPrice());

        // CHỐT HẠ: Giá cuối cùng ghi nhận trên RAM Server BẮT BUỘC phải bằng giá của người bid cao nhất đã lọt qua!
        assertEquals(maxSuccessfulBid, ramAuction.getCurrentPrice(),
                "LỖI RACE CONDITION: Server đã bị ghi đè dữ liệu sai lệch do các luồng chạy chồng chéo lên nhau!");
    }

    // ==========================================
    // GIAI ĐOẠN 5: KIỂM THỬ KÉP (RACE CONDITION + ANTI-SNIPING)
    // ==========================================

    @Test
    @Order(7)
    void testConcurrentAntiSniping_ShouldExtendOnlyOnce() throws Exception {
        // 1. Tạo một phiên đấu giá ĐẶC BIỆT: Chỉ còn 15 giây là kết thúc
        ItemFactory factory = new ElectronicsFactory();
        Item sniperItem = factory.createItem("Super Sniper Item", "Test", 10.0, targetAuction.getItem().getSeller());
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.insertItem(sniperItem);

        LocalDateTime initialEndTime = LocalDateTime.now().plusSeconds(15);
        Auction sniperAuction = new Auction(sniperItem, initialEndTime);
        sniperAuction.setAuctionId(java.util.UUID.randomUUID().toString());
        sniperAuction.setStatus(AuctionStatus.OPEN);

        AuctionDAO auctionDAO = new AuctionDAO();
        auctionDAO.insertAuction(sniperAuction);
        AuctionManager.getInstance().addAuctionToMemory(sniperAuction);

        // 2. Chuẩn bị 50 Client cùng "bắn tỉa" một lúc
        int numberOfThreads = 50;
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(numberOfThreads);

        List<Double> successfulBids = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            double bidAmount = 20.0 + (i * 10);

            new Thread(() -> {
                try (Socket s = new Socket("localhost", 5008);
                     java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(s.getOutputStream());
                     java.io.ObjectInputStream in = new java.io.ObjectInputStream(s.getInputStream())) {

                    Object[] payload = {sniperAuction.getAuctionId(), testBidder.getUserId(), bidAmount};
                    Message request = new Message("PLACE_BID", payload);

                    // NÍN THỞ
                    readyLatch.await();

                    // BÓP CÒ
                    out.writeObject(request);
                    out.flush();

                    Message response = (Message) in.readObject();
                    if ("SUCCESS".equals(response.getStatus())) {
                        successfulBids.add(bidAmount);
                    }
                } catch (Exception e) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        Thread.sleep(1000);

        // PHÁT LỆNH CHO 50 REQUEST LAO VÀO CÙNG 1 LÚC
        readyLatch.countDown();

        boolean isCompleted = doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(isCompleted, "Test bị treo do Server quá tải!");

        // 3. Đánh giá kết quả
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(sniperAuction.getAuctionId());

        assertFalse(successfulBids.isEmpty(), "Phải có lượt đặt giá thành công");
        double maxSuccessfulBid = successfulBids.stream().mapToDouble(Double::doubleValue).max().getAsDouble();

        // Đảm bảo Race Condition giá trị vẫn chuẩn
        assertEquals(maxSuccessfulBid, ramAuction.getCurrentPrice(), "Lỗi ghi đè giá (Race Condition)");

        // KIỂM TRA QUAN TRỌNG NHẤT: Thời gian chỉ được cộng ĐÚNG 1 LẦN (60s)
        // 15s gốc + 60s gia hạn = 1m15s (tương đương initialEndTime.plusSeconds(60))
        LocalDateTime expectedEndTime = initialEndTime.plusSeconds(60);

        System.out.println(">> [CONCURRENT ANTI-SNIPER] Thời gian kết thúc GỐC: " + initialEndTime);
        System.out.println(">> [CONCURRENT ANTI-SNIPER] Thời gian kết thúc MỚI (Thực tế): " + ramAuction.getEndTime());
        System.out.println(">> [CONCURRENT ANTI-SNIPER] Thời gian kết thúc MỚI (Mong đợi): " + expectedEndTime);

        assertEquals(expectedEndTime, ramAuction.getEndTime(),
                "LỖI: Server đã cộng dồn thời gian nhiều lần cho các request đồng thời, thiếu đồng bộ Thread-safe!");
    }

    // ==========================================
    // GIAI ĐOẠN 6: KIỂM THỬ XUNG ĐỘT (PLACE_BID vs FINISH_AUCTION)
    // ==========================================

    @Test
    @Order(8)
    void testRaceCondition_PlaceBid_vs_FinishAuction() throws Exception {
        // 1. Tạo một phiên đấu giá đã chạm mốc hết giờ (Để lệnh FINISH chắc chắn hợp lệ)
        ItemFactory factory = new ElectronicsFactory();
        Item clashItem = factory.createItem("Clash Item", "Món đồ gây xung đột", 100.0, targetAuction.getItem().getSeller());
        new ItemDAO().insertItem(clashItem);

        // Chỉnh thời gian kết thúc ở quá khứ (minusSeconds) để giả lập hết giờ
        LocalDateTime pastEndTime = LocalDateTime.now().minusSeconds(2);
        Auction clashAuction = new Auction(clashItem, pastEndTime);
        clashAuction.setAuctionId(java.util.UUID.randomUUID().toString());
        clashAuction.setStatus(AuctionStatus.RUNNING);

        new AuctionDAO().insertAuction(clashAuction);
        AuctionManager.getInstance().addAuctionToMemory(clashAuction);

        // 2. Chạy đua 2 luồng: 1 luồng Đặt giá, 1 luồng Đóng phiên
        java.util.concurrent.CountDownLatch readyLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(2);

        // THREAD 1: Lệnh Đặt giá (Mức giá 500$)
        new Thread(() -> {
            try (Socket s = new Socket("localhost", 5008);
                 java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(s.getOutputStream());
                 java.io.ObjectInputStream in = new java.io.ObjectInputStream(s.getInputStream())) {

                Message req = new Message("PLACE_BID", new Object[]{clashAuction.getAuctionId(), testBidder.getUserId(), 500.0});
                readyLatch.await(); // Nín thở đợi lệnh
                out.writeObject(req); out.flush();
                in.readObject();
            } catch (Exception e) {} finally { doneLatch.countDown(); }
        }).start();

        // THREAD 2: Lệnh Kết thúc phiên
        new Thread(() -> {
            try (Socket s = new Socket("localhost", 5008);
                 java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(s.getOutputStream());
                 java.io.ObjectInputStream in = new java.io.ObjectInputStream(s.getInputStream())) {

                Message req = new Message("FINISH_AUCTION", clashAuction.getAuctionId());
                readyLatch.await(); // Nín thở đợi lệnh
                out.writeObject(req); out.flush();
                in.readObject();
            } catch (Exception e) {} finally { doneLatch.countDown(); }
        }).start();

        // Đợi một chút để 2 kết nối Socket khởi tạo xong
        Thread.sleep(500);

        // BẮT ĐẦU ĐUA! Phóng 2 request vào Server cùng 1 mili-giây
        readyLatch.countDown();

        // Chờ tối đa 5s để Server xử lý xong cả 2 lệnh
        boolean isCompleted = doneLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(isCompleted, "Server bị treo hoặc crash do xung đột dữ liệu đa luồng!");

        // 3. Đánh giá kết quả
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(clashAuction.getAuctionId());

        assertNotNull(ramAuction, "Phiên đấu giá không được biến mất");

        // Nếu Server không có "ổ khóa" synchronized, RAM có thể bị kẹt ở trạng thái RUNNING
        assertEquals(AuctionStatus.FINISHED, ramAuction.getStatus(), "Phiên đấu giá bắt buộc phải được đóng lại an toàn (FINISHED)!");

        System.out.println(">> [PLACE_BID vs FINISH_AUCTION]");
        System.out.println(">> Trạng thái cuối cùng: " + ramAuction.getStatus());
        System.out.println(">> Giá chốt cuối cùng: $" + ramAuction.getCurrentPrice());
        if (ramAuction.getCurrentPrice() == 500.0) {
            System.out.println(">> Kết quả: Luồng PLACE_BID đã vào Server trước, sau đó FINISH_AUCTION đóng sổ.");
        } else {
            System.out.println(">> Kết quả: Luồng FINISH_AUCTION đã vào Server trước đóng sổ, PLACE_BID bị văng ra.");
        }
    }
}