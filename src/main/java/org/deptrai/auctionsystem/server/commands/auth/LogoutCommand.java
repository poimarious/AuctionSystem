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
      clientHandler.setAuthenticatedUser(null);
      out.reset();
      out.writeObject(new Message("SUCCESS", "LOGOUT", "Đã đăng xuất an toàn khỏi Server"));
      out.flush();
      logger.info("Một Client đã hủy Session và đăng xuất thành công.");
    } catch (Exception e) {
      logger.error("Lỗi gửi phản hồi đăng xuất: ", e);
    }
  }
}