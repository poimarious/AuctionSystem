package org.deptrai.auctionsystem.client.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.deptrai.auctionsystem.shared.network.Message;

public class SocketClient {
  private static Socket socket;
  private static ObjectOutputStream out;
  private static ObjectInputStream in;

  public static void connect(String serverAddress, int port) {
    try {
      socket = new Socket(serverAddress, port);
      out =
          new ObjectOutputStream(
              socket.getOutputStream()); // Output is always first to avoid deadlock
      in = new ObjectInputStream(socket.getInputStream());
      System.out.println("Đã kết nối tới Server thành công!");
    } catch (IOException e) {
      System.out.println("Không thể kết nối mạng: " + e.getMessage());
    }
  }

  // Nhận vào 1 Message, trả về 1 Message
  public static Message sendRequest(Message request) {
    if (out == null || in == null) return new Message("FAIL", "NETWORK", "Chưa kết nối");
    try {
      out.writeObject(request);
      out.flush();
      return (Message) in.readObject(); // Wait to receive Message
    } catch (Exception e) {
      return new Message("FAIL", "NETWORK", e.getMessage());
    }
  }

  public static void disconnect() {
    try {
      if (socket != null) socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
