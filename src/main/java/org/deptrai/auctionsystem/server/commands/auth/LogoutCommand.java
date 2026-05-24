package org.deptrai.auctionsystem.server.commands.auth;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class LogoutCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(LogoutCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String username = (clientHandler.getAuthenticatedUser() != null) ?
          clientHandler.getAuthenticatedUser().getUsername() : "Guest";

      clientHandler.setAuthenticatedUser(null);

      clientHandler.sendMessage(new Message("SUCCESS", "LOGOUT", "Đã đăng xuất an toàn khỏi Server"));

      logger.info("Người dùng [{}] đã chủ động hủy Session và đăng xuất.", username);
    } catch (Exception e) {
      logger.error("Lỗi gửi phản hồi đăng xuất: ", e);
      clientHandler.sendMessage(new Message("ERROR", "LOGOUT", "Lỗi hệ thống Server khi đăng xuất."));
    }
  }
}