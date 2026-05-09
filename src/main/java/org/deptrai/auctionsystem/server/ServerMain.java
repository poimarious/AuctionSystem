package org.deptrai.auctionsystem.server;

import org.deptrai.auctionsystem.server.utils.DatabaseConnection;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG ĐẤU GIÁ SERVER ===");

        DatabaseConnection.initializeDatabase();

        // Opening server port
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Máy chủ đang chạy và lắng nghe tại cổng " + PORT + "...");

            while (true) {
                // Waiting for a connection from clients
                Socket clientSocket = serverSocket.accept();
                System.out.println(">> Có Client mới kết nối: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }
}