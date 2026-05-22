package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import org.deptrai.auctionsystem.client.utils.AutoBidManager;
import org.deptrai.auctionsystem.client.utils.SessionManager;
import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.items.ElectronicsFactory;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AutoBidTest {

  private static ServerSocket serverSocket;
  private static Thread serverThread;

  private static Bidder autoBidUser; // Người dùng bật Auto Bid
  private static Bidder competitor;  // Đối thủ cạnh tranh
  private static Auction targetAuction;

  @BeforeAll
  static void setUp() throws Exception {
    DatabaseConnection.initializeDatabase();
    UserDAO userDAO = new UserDAO();

    // 1. Khởi chạy Server ảo trên cổng 5012
    serverSocket = new ServerSocket(5012);
    serverThread = new Thread(() -> {
      try {
        if (ServerMain.activeClients == null) {
          ServerMain.activeClients = new CopyOnWriteArrayList<>();
        }
        while (!serverSocket.isClosed()) {
          Socket clientSocket = serverSocket.accept();
          ClientHandler handler = new ClientHandler(clientSocket);
          ServerMain.activeClients.add(handler);
          new Thread(handler).start();
        }
      } catch (IOException e) {}
    });
    serverThread.start();

    // 2. Tạo dữ liệu mẫu
    String sellerName = "autobid_seller_" + System.currentTimeMillis();
    Seller seller = new Seller(null, sellerName, "Pass123!", sellerName + "@gmail.com");
    userDAO.insertUser(seller, "SELLER");
    seller = (Seller) userDAO.getUserByUsername(sellerName);

    String myName = "autobid_me_" + System.currentTimeMillis();
    autoBidUser = new Bidder(null, myName, "Pass123!", myName + "@gmail.com", new CopyOnWriteArrayList<>());
    userDAO.insertUser(autoBidUser, "BIDDER");
    autoBidUser = (Bidder) userDAO.getUserByUsername(myName);
    userDAO.updateBalance(autoBidUser.getUserId(), 5000.0);

    String compName = "autobid_comp_" + System.currentTimeMillis();
    competitor = new Bidder(null, compName, "Pass123!", compName + "@gmail.com", new CopyOnWriteArrayList<>());
    userDAO.insertUser(competitor, "BIDDER");
    competitor = (Bidder) userDAO.getUserByUsername(compName);
    userDAO.updateBalance(competitor.getUserId(), 5000.0);

    Item item = new ElectronicsFactory().createItem("AutoBid Test Item", "Test", 100.0, seller);
    new ItemDAO().insertItem(item);

    targetAuction = new Auction(item, LocalDateTime.now().plusDays(1));
    targetAuction.setAuctionId(java.util.UUID.randomUUID().toString());
    targetAuction.setStatus(AuctionStatus.OPEN);

    new AuctionDAO().insertAuction(targetAuction);
    AuctionManager.getInstance().addAuctionToMemory(targetAuction);

    // 3. Client kết nối tới Server
    SocketClient.connect("localhost", 5012);

    // GIẢ LẬP ĐĂNG NHẬP Ở CLIENT: Gắn user vào SessionManager để AutoBidManager nhận diện được "Tôi là ai"
    SessionManager.getInstance().setCurrentUser(autoBidUser);
  }

  @AfterAll
  static void tearDown() throws Exception {
    SocketClient.disconnect();
    if (serverSocket != null) serverSocket.close();
    if (serverThread != null) serverThread.interrupt();
  }

  // ==========================================
  // TEST 1: TỰ ĐỘNG ĐẶT GIÁ KHI BỊ NGƯỜI KHÁC DẪN ĐẦU
  // ==========================================
  // ==========================================
  // TEST 1: TỰ ĐỘNG ĐẶT GIÁ KHI BỊ NGƯỜI KHÁC DẪN ĐẦU
  // ==========================================
  @Test
  @Order(1)
  void testAutoBid_ShouldTrigger_WhenCompetitorIsWinning() throws Exception {
    // 1. Giả lập đối thủ (competitor) đang dẫn đầu với giá 100$
    // SỬA LỖI: Thêm 1 lượt Bid thực tế vào danh sách thay vì gọi setWinner()
    org.deptrai.auctionsystem.shared.models.bid.Bid compBid = new org.deptrai.auctionsystem.shared.models.bid.Bid(
            java.util.UUID.randomUUID().toString(), competitor, targetAuction, 100.0, LocalDateTime.now());
    targetAuction.getBids().add(compBid);
    targetAuction.setCurrentPrice(100.0);

    // 2. Kích hoạt Auto Bid cho user hiện tại: maxBid = 500$, bước giá = 15$
    System.out.println(">> Bật Auto Bid: Max = 500, Increment = 15");
    AutoBidManager.getInstance().startAutoBid(targetAuction, 500.0, 15.0);

    // Khẳng định trạng thái đang bật
    assertTrue(AutoBidManager.getInstance().isAutoBidActive(targetAuction.getAuctionId()));

    // 3. CHỜ ĐỢI: Vì AutoBidManager có lệnh Thread.sleep(1000), ta phải chờ nó bắn súng
    System.out.println(">> Đang chờ AutoBidManager tính toán và gửi lệnh (6.5s)...");
    Thread.sleep(6500);

    // 4. KIỂM TRA KẾT QUẢ TRÊN SERVER:
    // Giá mới phải là 100 + 15 = 115, và người chiến thắng hiện tại phải là autoBidUser
    Auction ramAuction = AuctionManager.getInstance().getAuctionById(targetAuction.getAuctionId());

    assertEquals(115.0, ramAuction.getCurrentPrice(), "Giá phải được Auto Bid nâng lên thành 115.0");
    assertNotNull(ramAuction.getWinner());
    assertEquals(autoBidUser.getUserId(), ramAuction.getWinner().getUserId(), "Bot phải giúp User cướp lại vị trí Top 1");
  }

  // ==========================================
  // TEST 2: TỰ ĐỘNG TẮT KHI VƯỢT QUÁ GIỚI HẠN TÀI CHÍNH (MAX BID)
  // ==========================================
  @Test
  @Order(2)
  void testAutoBid_ShouldStop_WhenExceedingMaxBid() throws Exception {
    // 1. Đối thủ tung cú đòn hiểm, nâng giá lên tận 490$
    // SỬA LỖI: Thêm lượt Bid mới của đối thủ
    org.deptrai.auctionsystem.shared.models.bid.Bid compBid2 = new org.deptrai.auctionsystem.shared.models.bid.Bid(
            java.util.UUID.randomUUID().toString(), competitor, targetAuction, 490.0, LocalDateTime.now());
    targetAuction.getBids().add(compBid2);
    targetAuction.setCurrentPrice(490.0);

    // 2. AutoBidManager sẽ tự động bắt được tín hiệu
    // Nó sẽ cố gắng lấy 490 + 15 = 505$. Nhưng 505$ > 500$ (maxBid).
    // Do đó, logic sẽ gọi stopAutoBid().

    // Gọi lại hàm mồi để giả lập sự kiện Socket nhận tin Broadcast
    java.lang.reflect.Method method = AutoBidManager.class.getDeclaredMethod("onGlobalAuctionUpdated", Auction.class);
    method.setAccessible(true);
    method.invoke(AutoBidManager.getInstance(), targetAuction);

    // 3. CHỜ ĐỢI
    Thread.sleep(500);

    // 4. KIỂM TRA KẾT QUẢ: Tính năng phải bị tắt hoàn toàn
    boolean isActive = AutoBidManager.getInstance().isAutoBidActive(targetAuction.getAuctionId());
    assertFalse(isActive, "Auto Bid bắt buộc phải tự tắt khi mức giá tiếp theo vượt quá túi tiền (Max Bid)!");

    System.out.println(">> [AUTO BID TEST] Bot đã tự động phanh lại an toàn khi chạm ngưỡng tài chính!");
  }
}