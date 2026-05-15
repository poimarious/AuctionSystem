package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.User;


import org.deptrai.auctionsystem.shared.network.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionAndTopUpTest {

    private static ServerSocket serverSocket;
    private static Thread serverThread;

    private static User testUser;
    private static UserDAO userDAO;

    // Biến tạm để lấy mẫu ID thật từ Database phục vụ cho bài test sau
    private static String sampleAuctionId = null;
    private static String sampleSellerId = null;

    @BeforeAll
    static void setUp() throws Exception {
        DatabaseConnection.initializeDatabase();
        AuctionManager.getInstance().loadAuctionsFromDatabase();

        // Chạy Server ảo trên cổng 5006
        serverSocket = new ServerSocket(5006);
        serverThread = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                }
            } catch (IOException e) {
                // Bỏ qua lỗi ngắt kết nối
            }
        });
        serverThread.start();

        SocketClient.connect("localhost", 5006);

        // Tạo user để test Top Up và Update Password
        userDAO = new UserDAO();
        String username = "testUser_" + System.currentTimeMillis();
        testUser = new Bidder(null, username, "Test1234!", "testuser@gmail.com", new java.util.concurrent.CopyOnWriteArrayList<>());
        userDAO.insertUser(testUser, "BIDDER");
        testUser = userDAO.getUserByUsername(username);
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
    // TESTS CHO METHOD: handleTopUp
    // ==========================================
    @Test
    @Order(1)
    void testTopUp_ValidUser_ShouldIncreaseBalance() {
        Object[] topUpData = {testUser.getUserId(), 50.5};
        Message request = new Message("TOP_UP", topUpData);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(50.5, (Double) response.getData(), 0.001);
    }

    @Test
    @Order(2)
    void testTopUp_InvalidUser_ShouldFail() {
        Object[] topUpData = {"Fake_User_ID_123", 100.0};
        Message request = new Message("TOP_UP", topUpData);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
    }

    // ==========================================
    // TESTS CHO METHOD: handleUpdatePassword
    // ==========================================
    @Test
    @Order(3)
    void testUpdatePassword_InvalidFormat_ShouldFail() {
        // Mật khẩu mới không có ký tự đặc biệt
        String[] payload = {testUser.getUserId(), "Test1234!", "weakpass123"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertTrue(((String) response.getData()).contains("ít nhất 8 ký tự"));
    }

    @Test
    @Order(4)
    void testUpdatePassword_WrongCurrentPassword_ShouldFail() {
        // Cố tình nhập sai mật khẩu cũ
        String[] payload = {testUser.getUserId(), "WrongPass!", "NewPass123!@#"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertEquals("Mật khẩu hiện tại không đúng!", response.getData());
    }

    @Test
    @Order(5)
    void testUpdatePassword_ValidData_ShouldSucceed() {
        // Đổi mật khẩu thành công
        String[] payload = {testUser.getUserId(), "Test1234!", "NewPass123!@#"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Cập nhật mật khẩu thành công!", response.getData());

        // Test lại DB xem đã lưu thật chưa
        User dbUser = userDAO.getUserById(testUser.getUserId());
        assertEquals("NewPass123!@#", dbUser.getPassword());
    }

    // ==========================================
    // TESTS CHO METHOD: handleGetAllAuctions
    // ==========================================
    @Test
    @Order(6)
    void testGetAllAuctions_ShouldReturnList() {
        Message request = new Message("GET_ALL_AUCTIONS", null);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());

        @SuppressWarnings("unchecked")
        List<Auction> returnedAuctions = (List<Auction>) response.getData();
        assertNotNull(returnedAuctions);

        // Nếu có dữ liệu trong DB, bóc tách ID ra để chạy 2 bài Test phía dưới
        if (!returnedAuctions.isEmpty()) {
            Auction firstAuction = returnedAuctions.get(0);
            sampleAuctionId = firstAuction.getAuctionId();

            if (firstAuction.getItem() != null && firstAuction.getItem().getSeller() != null) {
                sampleSellerId = firstAuction.getItem().getSeller().getUserId();
            }
        }
    }

    // ==========================================
    // TESTS CHO METHOD: handleGetAuctionById
    // ==========================================
    @Test
    @Order(7)
    void testGetAuctionById_ValidId_ShouldReturnAuction() {
        // Bỏ qua test nếu Database trắng tinh
        if (sampleAuctionId == null) return;

        Message request = new Message("GET_AUCTION_BY_ID", sampleAuctionId);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        Auction auction = (Auction) response.getData();
        assertNotNull(auction);
        assertEquals(sampleAuctionId, auction.getAuctionId(), "ID trả về phải khớp với ID yêu cầu");
    }

    @Test
    @Order(8)
    void testGetAuctionById_InvalidId_ShouldFail() {
        Message request = new Message("GET_AUCTION_BY_ID", "Fake_Auction_ID_123");
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertEquals("Không tìm thấy phiên đấu giá này!", response.getData());
    }

    // ==========================================
    // TESTS CHO METHOD: handleGetSellerAuctions
    // ==========================================
    @Test
    @Order(9)
    void testGetSellerAuctions_ValidSeller_ShouldReturnList() {
        // Bỏ qua test nếu không có seller mẫu
        if (sampleSellerId == null) return;

        Message request = new Message("GET_SELLER_AUCTIONS", sampleSellerId);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        @SuppressWarnings("unchecked")
        List<Auction> sellerAuctions = (List<Auction>) response.getData();
        assertNotNull(sellerAuctions);
        assertTrue(sellerAuctions.size() > 0, "Seller này đã đăng đồ thì danh sách phải lớn hơn 0");
    }

    @Test
    @Order(10)
    void testGetSellerAuctions_InvalidSeller_ShouldReturnEmptyList() {
        Message request = new Message("GET_SELLER_AUCTIONS", "Fake_Seller_Id_999");
        Message response = SocketClient.sendRequest(request);

        // Dù Seller không tồn tại, Server vẫn trả về SUCCESS nhưng list rỗng (không có lỗi hệ thống)
        assertEquals("SUCCESS", response.getStatus());
        @SuppressWarnings("unchecked")
        List<Auction> sellerAuctions = (List<Auction>) response.getData();
        assertEquals(0, sellerAuctions.size(), "Danh sách phải rỗng đối với người không có món đồ nào");
    }
}