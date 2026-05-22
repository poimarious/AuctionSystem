package org.deptrai.auctionsystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.deptrai.auctionsystem.server.ServerMain;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerConnectionTest {

    private static Thread serverThread;

    @BeforeAll
    static void setUp() throws InterruptedException {
        // Đảm bảo danh sách activeClients hoàn toàn sạch sẽ trước khi test
        ServerMain.activeClients.clear();

        // CHẠY SERVER NGẦM: Đưa hàm main() vào một Thread độc lập để không làm treo Unit Test
        serverThread = new Thread(() -> {
            ServerMain.main(new String[]{});
        });
        serverThread.start();

        // Cho Server 1 giây để khởi tạo Database, Load RAM và mở cổng 5000
        Thread.sleep(1000);
    }

    @AfterAll
    static void tearDown() {
        // Dọn dẹp luồng Server sau khi chạy xong tất cả các bài test
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    // ==========================================
    // BÀI TEST 1: KẾT NỐI BÌNH THƯỜNG (Dưới mức 50)
    // ==========================================
    @Test
    @Order(1)
    @DisplayName("Test kết nối nhiều Client cùng lúc (Dưới mức giới hạn Thread Pool)")
    void testConnectMultipleClients_UnderPoolSize() throws Exception {
        int numClients = 10;
        List<Socket> clients = new ArrayList<>();

        // Giả lập 10 người dùng mở App cùng lúc
        for (int i = 0; i < numClients; i++) {
            Socket socket = new Socket("localhost", 5000);
            clients.add(socket);
        }

        // Chờ Server xử lý lệnh accept() và add vào list
        Thread.sleep(500);

        // Kiểm tra số lượng client đang active trên Server
        System.out.println(">> [TEST 1] Số Client kết nối thành công: " + ServerMain.activeClients.size());
        assertTrue(ServerMain.activeClients.size() >= numClients,
                "Server phải ghi nhận đủ số lượng client kết nối");

        // Dọn dẹp các Socket đã mở
        for (Socket s : clients) {
            s.close();
        }
    }

    // ==========================================
    // BÀI TEST 2: KIỂM THỬ SỨC CHỊU TẢI (Vượt mức 50)
    // ==========================================
    @Test
    @Order(2)
    @DisplayName("Test Server chịu tải (Vượt quá POOL_SIZE = 50)")
    void testServerOverload_ExceedingPoolSize() throws Exception {
        ServerMain.activeClients.clear(); // Dọn dẹp tàn dư của Test 1

        // POOL_SIZE của ServerMain là 50, ta sẽ bắn 70 kết nối đồng loạt
        int overloadSize = 70;
        List<Socket> overloadClients = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(overloadSize);

        for (int i = 0; i < overloadSize; i++) {
            new Thread(() -> {
                try {
                    Socket socket = new Socket("localhost", 5000);
                    synchronized (overloadClients) {
                        overloadClients.add(socket);
                    }
                } catch (IOException e) {
                    System.err.println("Kết nối thất bại: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // Chờ tất cả 70 luồng hoàn thành việc kết nối
        latch.await(5, TimeUnit.SECONDS);

        // Cho Server 1 khoảng thời gian nhỏ để kịp nhét các Client vào Queue của ThreadPool
        Thread.sleep(1000);

        System.out.println(">> [TEST 2] Số Client nã vào cổng 5000: " + overloadSize);
        System.out.println(">> [TEST 2] Số Client Server đã tiếp nhận: " + ServerMain.activeClients.size());

        // CHỐT HẠ: Dù Thread Pool chỉ cấp 50 luồng để xử lý đồng thời,
        // nhưng ServerSocket vẫn phải accept() thành công cả 70 người và đưa 20 người thừa vào Hàng đợi (Queue).
        // Nếu Server bị Crash hoặc từ chối kết nối (Reject), activeClients.size() sẽ nhỏ hơn 70.
        assertEquals(overloadSize, ServerMain.activeClients.size(),
                "Hệ thống ThreadPool bị lỗi! Server phải accept và đưa tất cả các request vào hàng đợi mà không bị crash!");

        // Dọn dẹp
        for (Socket s : overloadClients) {
            s.close();
        }
    }
}