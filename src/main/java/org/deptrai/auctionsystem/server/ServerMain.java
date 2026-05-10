package org.deptrai.auctionsystem.server;

import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT      = 5000;

    // Giới hạn tối đa 50 client chạy đồng thời, tránh server bị quá tải
    private static final int POOL_SIZE = 50;

    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ SERVER ===");

        DatabaseConnection.initializeDatabase();
        AuctionManager.getInstance().loadAuctionsFromDatabase();
        /*
         * Thread pool thay cho new Thread().start():
         *   - Tái sử dụng thread thay vì tạo mới mỗi lần → tiết kiệm tài nguyên
         *   - Giới hạn số thread đồng thời → tránh OOM khi có quá nhiều client
         *   - Client vượt quá POOL_SIZE sẽ vào hàng đợi, không bị từ chối
         */
        ExecutorService threadPool = Executors.newFixedThreadPool(POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Máy chủ đang chạy và lắng nghe tại cổng " + PORT + "...");

            while (true) {
                // Waiting for a connection from clients
                Socket clientSocket = serverSocket.accept();
                System.out.println(">> Có Client mới kết nối: " + clientSocket.getInetAddress().getHostAddress());

                // Thay new Thread(handler).start() → submit vào pool, không tạo thread mới
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            System.out.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }
}