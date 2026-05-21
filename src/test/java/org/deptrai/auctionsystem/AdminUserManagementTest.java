package org.deptrai.auctionsystem;



import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.deptrai.auctionsystem.client.utils.SocketClient;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain; // Import thêm ServerMain
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import org.deptrai.auctionsystem.shared.models.users.Admin;
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
public class AdminUserManagementTest {
    private static ServerSocket serverSocket;
    private static Thread serverThread;
    private static UserDAO userDAO;

    private static Admin adminLevel2;
    private static Admin adminLevel1;
    private static Bidder targetUser;

    @BeforeAll
    static void setUp() throws Exception {
        DatabaseConnection.initializeDatabase();
        userDAO = new UserDAO();

        // 1. Khởi chạy Server ảo trên cổng 5009 (Để không đụng chạm các bài test khác)
        serverSocket = new ServerSocket(5009);
        serverThread = new Thread(() -> {
            try {
                // Khởi tạo list tĩnh của ServerMain để test tính năng Online Kicking
                if (ServerMain.activeClients == null) {
                    ServerMain.activeClients = new java.util.concurrent.CopyOnWriteArrayList<>();
                }

                while (!serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket);

                    // BẮT BUỘC: Add client vào danh sách quản lý của Server
                    ServerMain.activeClients.add(handler);

                    new Thread(handler).start();
                }
            } catch (IOException e) {}
        });
        serverThread.start();

        // 2. Kết nối SocketClient (Đóng vai trò là Admin đang thao tác trên máy của họ)
        SocketClient.connect("localhost", 5009);

        // 3. Chuẩn bị dữ liệu: Admin Cấp 2 (Có quyền sinh sát)
        String admin2Name = "admin_super_" + System.currentTimeMillis();
        adminLevel2 = new Admin(null, admin2Name, "Admin123!", admin2Name + "@gmail.com", 2);
        userDAO.insertUser(adminLevel2, "ADMIN");
        adminLevel2 = (Admin) userDAO.getUserByUsername(admin2Name);

        // 4. Chuẩn bị dữ liệu: Admin Cấp 1 (Nhân viên bình thường, không có quyền Ban)
        String admin1Name = "admin_staff_" + System.currentTimeMillis();
        adminLevel1 = new Admin(null, admin1Name, "Admin123!", admin1Name + "@gmail.com", 1);
        userDAO.insertUser(adminLevel1, "ADMIN");
        adminLevel1 = (Admin) userDAO.getUserByUsername(admin1Name);

        // 5. Chuẩn bị dữ liệu: Nạn nhân (Target User)
        String targetName = "victim_user_" + System.currentTimeMillis();
        targetUser = new Bidder(null, targetName, "User123!", targetName + "@gmail.com", new java.util.concurrent.CopyOnWriteArrayList<>());
        userDAO.insertUser(targetUser, "BIDDER");
        targetUser = (Bidder) userDAO.getUserByUsername(targetName);
    }

    @AfterAll
    static void tearDown() throws Exception {
        SocketClient.disconnect();
        if (serverSocket != null) serverSocket.close();
        if (serverThread != null) serverThread.interrupt();
    }

    // ==========================================
    // BÀI TEST 1: PHÂN QUYỀN (AUTHORIZATION)
    // ==========================================
    @Test
    @Order(1)
    void testBanUser_WithLevel1Admin_ShouldFail() {
        Object[] payload = {adminLevel1, targetUser.getUserId(), "Phá giá thị trường"};
        Message request = new Message("BAN_USER", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("FAIL", response.getStatus());
        assertEquals("Bạn không có quyền Ban người dùng!", response.getData());
    }

    // ==========================================
    // BÀI TEST 2: BAN NGƯỜI DÙNG THÀNH CÔNG (DATABASE)
    // ==========================================
    @Test
    @Order(2)
    void testBanUser_WithLevel2Admin_ShouldSucceed() {
        Object[] payload = {adminLevel2, targetUser.getUserId(), "Vi phạm nghiêm trọng"};
        Message request = new Message("BAN_USER", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());

        // Kiểm tra Database xem còng số 8 đã được khóa chưa
        User dbTarget = userDAO.getUserById(targetUser.getUserId());
        assertTrue(dbTarget.isBanned(), "Người dùng bắt buộc phải bị đánh dấu isBanned = true trong DB");
        assertEquals("Vi phạm nghiêm trọng", dbTarget.getBanReason(), "Lý do Ban phải được ghi nhận");
    }

    // ==========================================
    // BÀI TEST 3: GỠ BAN NGƯỜI DÙNG (UNBAN)
    // ==========================================
    @Test
    @Order(3)
    void testUnbanUser_WithLevel2Admin_ShouldSucceed() {
        Object[] payload = {adminLevel2, targetUser.getUserId()};
        Message request = new Message("UNBAN_USER", payload);
        Message response = SocketClient.sendRequest(request);

        assertEquals("SUCCESS", response.getStatus());

        // Kiểm tra Database xem đã gỡ còng chưa
        User dbTarget = userDAO.getUserById(targetUser.getUserId());
        assertFalse(dbTarget.isBanned(), "Người dùng phải được đánh dấu isBanned = false sau khi gỡ");
        assertNull(dbTarget.getBanReason(), "Lý do Ban phải bị xóa sạch (Null)");
    }

    // ==========================================
    // BÀI TEST 4: ĐÁ NGƯỜI DÙNG ONLINE (REAL-TIME KICK)
    // ==========================================
    @Test
    @Order(4)
    void testBanUser_OnlineUser_ShouldReceiveForceLogout() throws Exception {
        // 1. Tạo 1 Socket ngầm, đóng vai trò là chiếc máy tính của Nạn nhân đang mở app
        java.util.concurrent.CountDownLatch kickLatch = new java.util.concurrent.CountDownLatch(1);
        boolean[] isKicked = {false};

        Thread victimThread = new Thread(() -> {
            try (Socket s = new Socket("localhost", 5009);
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

                // A. Nạn nhân đăng nhập vào Server
                String[] loginData = {targetUser.getUsername(), "User123!"};
                out.writeObject(new Message("LOGIN", loginData));
                out.flush();

                // Nhận thông báo đăng nhập thành công
                in.readObject();

                // B. Nín thở chờ bị Admin "đá" khỏi phòng
                Message nextMsg = (Message) in.readObject();
                if ("UPDATE".equals(nextMsg.getStatus()) && "FORCE_LOGOUT".equals(nextMsg.getCommand())) {
                    isKicked[0] = true;
                    kickLatch.countDown();
                }

            } catch (Exception e) {}
        });
        victimThread.start();

        // Chờ 1 giây để nạn nhân đăng nhập thành công
        Thread.sleep(1000);

        // 2. TẠI MÁY CỦA ADMIN: Phát lệnh Tử hình
        Object[] payload = {adminLevel2, targetUser.getUserId(), "Dùng Tool Hack Đấu Giá"};
        Message request = new Message("BAN_USER", payload);
        SocketClient.sendRequest(request);

        // 3. Đánh giá xem nạn nhân có bị đá ra không?
        boolean completed = kickLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(completed, "LỖI SOCKET: Server không gửi tín hiệu FORCE_LOGOUT cho người dùng đang online!");
        assertTrue(isKicked[0], "Luồng của nạn nhân phải nhận được chính xác gói tin FORCE_LOGOUT!");

        System.out.println(">> [ADMIN TEST] Đã ban và đá văng người dùng đang online thành công!");
    }
}
