package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.items.ItemFactory;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
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

    // Biến mẫu lưu ID phục vụ cho Test 7 và 9
    private static String sampleAuctionId;
    private static String sampleSellerId;

    @BeforeAll
    static void setUp() throws Exception {
        DatabaseConnection.initializeDatabase();
        userDAO = new UserDAO();

        // ========================================================
        // TẠO DỮ LIỆU MẪU ĐỂ TEST 7 VÀ 9 LUÔN ĐƯỢC CHẠY
        // ========================================================


        // 1. Tạo một Seller
        String sellerName = "seller_" + System.currentTimeMillis();
        // SỬA: Dùng luôn sellerName làm email để đảm bảo không bao giờ bị trùng
        Seller dummySeller = new Seller(null, sellerName, "Test1234!", sellerName + "@gmail.com");
        userDAO.insertUser(dummySeller, "SELLER");
        dummySeller = (Seller) userDAO.getUserByUsername(sellerName);
        sampleSellerId = dummySeller.getUserId();

        // 2. Tạo một Item cho Seller đó
        ItemFactory factory = new ElectronicsFactory();
        Item dummyItem = factory.createItem("Laptop Test", "Máy tính thử nghiệm", 500.0, dummySeller);
        ItemDAO itemDAO = new ItemDAO();
        itemDAO.insertItem(dummyItem);

        // 3. Tạo một Auction và lưu thẳng xuống DB
        Auction dummyAuction = new Auction(dummyItem, LocalDateTime.now().plusDays(2));
        dummyAuction.setAuctionId(java.util.UUID.randomUUID().toString());
        dummyAuction.setStatus(AuctionStatus.OPEN);
        AuctionDAO auctionDAO = new AuctionDAO();
        auctionDAO.insertAuction(dummyAuction);

        sampleAuctionId = dummyAuction.getAuctionId();

        // ========================================================

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

        // Tạo user để test Change Balance và Update Password
        String username = "testUser_" + System.currentTimeMillis();
        // SỬA: Dùng luôn username làm email
        testUser = new Bidder(null, username, "Test1234!", username + "@gmail.com", new java.util.concurrent.CopyOnWriteArrayList<>());
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
    // TESTS CHO METHOD: handleChangeBalance (Cũ: handleTopUp)
    // ==========================================
    @Test
    @Order(1)
    void testChangeBalance_ValidUser_ShouldIncreaseBalance() {
        Object[] topUpData = {testUser.getUserId(), 50.5};
        Message request = new Message("CHANGE_BALANCE", topUpData); // Lệnh đã đổi tên
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(50.5, (Double) response.getData(), 0.001);
    }

    @Test
    @Order(2)
    void testChangeBalance_InvalidUser_ShouldFail() {
        Object[] topUpData = {"Fake_User_ID_123", 100.0};
        Message request = new Message("CHANGE_BALANCE", topUpData);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
    }

    // ==========================================
    // TESTS CHO METHOD: handleUpdatePassword
    // ==========================================
    @Test
    @Order(3)
    void testUpdatePassword_InvalidFormat_ShouldFail() {
        String[] payload = {testUser.getUserId(), "Test1234!", "weakpass123"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertTrue(((String) response.getData()).contains("ít nhất 8 ký tự"));
    }

    @Test
    @Order(4)
    void testUpdatePassword_WrongCurrentPassword_ShouldFail() {
        String[] payload = {testUser.getUserId(), "WrongPass!", "NewPass123!@#"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertEquals("Mật khẩu hiện tại không đúng!", response.getData());
    }

    @Test
    @Order(5)
    void testUpdatePassword_ValidData_ShouldSucceed() {
        String[] payload = {testUser.getUserId(), "Test1234!", "NewPass123!@#"};
        Message request = new Message("UPDATE_PASSWORD", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Cập nhật mật khẩu thành công!", response.getData());

        User dbUser = userDAO.getUserById(testUser.getUserId());
        assertEquals("NewPass123!@#", dbUser.getPassword());
    }

    // ==========================================
    // TESTS CHO METHOD: handleGetAllAuctions
    // ==========================================
    @Test
    @Order(6)
    void testGetAllAuctions_ShouldReturnList() {
        // Truyền lên userId để cover trường hợp lấy cả món đồ Đã thắng
        Message request = new Message("GET_ALL_AUCTIONS", testUser.getUserId());
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());

        // Đã đổi kiểu ép (Cast) thành List<AuctionSummary> thay vì List<Auction>
        @SuppressWarnings("unchecked")
        List<AuctionSummary> returnedAuctions = (List<AuctionSummary>) response.getData();
        assertNotNull(returnedAuctions);
        assertTrue(returnedAuctions.size() > 0, "Danh sách phải có ít nhất 1 phiên đấu giá mẫu vừa tạo");
    }

    // ==========================================
    // TESTS CHO METHOD: handleGetAuctionById
    // ==========================================
    @Test
    @Order(7)
    void testGetAuctionById_ValidId_ShouldReturnAuction() {
        Message request = new Message("GET_AUCTION_BY_ID", sampleAuctionId);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());
        Auction auction = (Auction) response.getData(); // Vẫn là Auction đầy đủ
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

        assertEquals("SUCCESS", response.getStatus());
        @SuppressWarnings("unchecked")
        List<Auction> sellerAuctions = (List<Auction>) response.getData();
        assertEquals(0, sellerAuctions.size(), "Danh sách phải rỗng đối với người không có món đồ nào");
    }
}