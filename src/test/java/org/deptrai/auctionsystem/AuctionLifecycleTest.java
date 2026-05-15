package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.ItemFactory;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.network.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionLifecycleTest {

    private static ServerSocket serverSocket;
    private static Thread serverThread;

    private static Seller testSeller;

    // Biến lưu ID của phiên đấu giá được tạo ra ở Test 1, dùng để Đóng và Xóa ở Test sau
    private static String targetAuctionId;

    @BeforeAll
    static void setUp() throws Exception {
        DatabaseConnection.initializeDatabase();
        AuctionManager.getInstance().loadAuctionsFromDatabase();

        // Khởi chạy Server ảo trên cổng 5007
        serverSocket = new ServerSocket(5007);
        serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                // Bỏ qua lỗi khi tắt server
            }
        });
        serverThread.start();

        // Kết nối Client giả lập
        SocketClient.connect("localhost", 5007);

        // Tạo sẵn một Seller hợp lệ dưới Database để có quyền Đăng bán đồ
        UserDAO userDAO = new UserDAO();
        String sellerName = "lifecycle_seller_" + System.currentTimeMillis();
        testSeller = new Seller(null, sellerName, "Test1234!", "lifecycle@gmail.com");
        userDAO.insertUser(testSeller, "SELLER");
        testSeller = (Seller) userDAO.getUserByUsername(sellerName);
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
    // GIAI ĐOẠN 1: TẠO PHIÊN ĐẤU GIÁ (CREATE)
    // ==========================================

    @Test
    @Order(1)
    void testCreateAuction_ValidData_ShouldSucceed() {
        // 1. Chuẩn bị dữ liệu Item
        ItemFactory factory = new ElectronicsFactory();
        Item newItem = factory.createItem("Điện thoại Test", "Mô tả test", 150.0, testSeller);

        // 2. Chuẩn bị thời gian và ảnh giả
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        byte[] dummyImageBytes = new byte[] { 1, 2, 3 }; // Mảng byte giả lập file ảnh
        String fileName = "test_image.jpg";

        // 3. Đóng gói Payload theo đúng chuẩn 4 phần tử bạn đã định nghĩa
        Object[] payload = new Object[] { newItem, endTime, dummyImageBytes, fileName };
        Message request = new Message("CREATE_AUCTION", payload);

        // 4. Gửi Request
        Message response = SocketClient.sendRequest(request);

        // Kiểm tra kết quả
        assertEquals("SUCCESS", response.getStatus(), "Tạo phiên đấu giá phải thành công");
        assertTrue(response.getData() instanceof Auction, "Dữ liệu trả về phải là một Object Auction");

        Auction createdAuction = (Auction) response.getData();
        assertNotNull(createdAuction.getAuctionId(), "Phiên đấu giá mới phải được cấp ID");

        // LƯU LẠI ID ĐỂ DÙNG CHO CÁC BÀI TEST SAU
        targetAuctionId = createdAuction.getAuctionId();
        System.out.println(">> [TEST 1] Đã tạo thành công Auction ID: " + targetAuctionId);
    }

    // ==========================================
    // GIAI ĐOẠN 2: ĐÓNG PHIÊN ĐẤU GIÁ (CLOSE)
    // ==========================================

    @Test
    @Order(2)
    void testCloseAuction_ValidId_ShouldUpdateStatus() {
        // Bỏ qua nếu Test 1 chạy thất bại
        assertNotNull(targetAuctionId, "Cần có targetAuctionId từ Test 1");

        Message request = new Message("CLOSE_AUCTION", targetAuctionId);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus(), "Đóng phiên đấu giá phải thành công");
        Auction closedAuction = (Auction) response.getData();

        // Kiểm tra xem trạng thái đã được cập nhật thành CANCELED chưa
        assertEquals(AuctionStatus.CANCELED, closedAuction.getStatus(), "Trạng thái phải là CANCELED");

        // Kiểm tra chéo trên RAM (AuctionManager) xem đã đồng bộ trạng thái chưa
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(targetAuctionId);
        assertEquals(AuctionStatus.CANCELED, ramAuction.getStatus(), "Trạng thái trên RAM cũng phải được cập nhật");

        System.out.println(">> [TEST 2] Đã đóng (CANCELED) Auction ID: " + targetAuctionId);
    }

    // ==========================================
    // GIAI ĐOẠN 3: XÓA PHIÊN ĐẤU GIÁ (DELETE)
    // ==========================================

    @Test
    @Order(3)
    void testDeleteAuction_ValidId_ShouldRemoveCompletely() {
        assertNotNull(targetAuctionId, "Cần có targetAuctionId từ Test 1");

        Message request = new Message("DELETE_AUCTION", targetAuctionId);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus(), "Xóa phiên đấu giá phải thành công");

        // 1. Kiểm tra RAM: Phiên đấu giá phải biến mất khỏi Manager
        Auction ramAuction = AuctionManager.getInstance().getAuctionById(targetAuctionId);
        assertNull(ramAuction, "Phiên đấu giá phải bị xóa khỏi RAM");

        // 2. Kiểm tra DB: Gọi xuống DAO xem còn tồn tại không
        AuctionDAO auctionDAO = new AuctionDAO();
        Auction dbAuction = auctionDAO.getAuctionById(targetAuctionId);
        assertNull(dbAuction, "Phiên đấu giá phải bị xóa triệt để khỏi Database");

        System.out.println(">> [TEST 3] Đã xóa hoàn toàn Auction ID: " + targetAuctionId);
    }

    // ==========================================
    // TEST NGOẠI LỆ (Ném ID tào lao vào hệ thống)
    // ==========================================

    @Test
    @Order(4)
    void testCloseAuction_InvalidId_ShouldFail() {
        Message request = new Message("CLOSE_AUCTION", "ID_KHONG_TON_TAI_123");
        Message response = SocketClient.sendRequest(request);
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @Order(5)
    void testDeleteAuction_InvalidId_ShouldFail() {
        Message request = new Message("DELETE_AUCTION", "ID_KHONG_TON_TAI_456");
        Message response = SocketClient.sendRequest(request);
        assertEquals("FAIL", response.getStatus());
    }
}