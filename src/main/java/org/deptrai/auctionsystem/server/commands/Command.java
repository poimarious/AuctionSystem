package org.deptrai.auctionsystem.server.commands;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.shared.network.Message;
import java.io.ObjectOutputStream;

public interface Command {
  // clientHandler: Dùng để gọi các hàm cấp Server (ví dụ get/set User, đóng mạng...)
  // request: Gói tin Client gửi lên
  // out: Đường ống để bắn kết quả về
  void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception;
}