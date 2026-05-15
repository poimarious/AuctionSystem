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
    /*
    @Test
    @Order(1)
    void testPlaceBid_ValidBid_ShouldSucceedAndIncreasePrice() {
        // 1. Tạo đối tượng Bid với giá 150.0 (Lớn hơn giá gốc 100.0)
        double bidAmount = 150.0;
        Bid newBid = new Bid(null, testBidder, targetAuction, bidAmount, LocalDateTime.now());

        // 2. Gửi Request
        // Giả định Server của bạn đang nhận Payload là đối tượng Bid
        Message request = new Message("PLACE_BID", newBid);
        Message response = SocketClient.sendRequest(request);

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

        Bid newBid = new Bid(null, testBidder, fakeAuction, 200.0, LocalDateTime.now());
        Message request = new Message("PLACE_BID", newBid);
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
    }*/

    @Test
    @Order(4)
    void testGetBidsHistory_EmptyUserId_ShouldFail() {
        // Gửi ID rỗng (Lỗi Client)
        Message request = new Message("GET_BIDS_HISTORY", "   ");
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus(), "Phải báo lỗi nếu ID rỗng");
        assertEquals("ID người dùng không hợp lệ.", response.getData());
    }
}