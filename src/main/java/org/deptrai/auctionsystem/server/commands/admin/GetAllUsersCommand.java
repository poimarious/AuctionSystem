package org.deptrai.auctionsystem.server.commands.admin;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.shared.network.Message;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetAllUsersCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetAllUsersCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      clientHandler.sendMessage(new Message("SUCCESS", "GET_ALL_USERS", new UserDAO().getAllUsers()));
    } catch (Exception e) {
      logger.error("Lỗi lấy danh sách người dùng: ", e);
      clientHandler.sendMessage(new Message("ERROR", "GET_ALL_USERS", "Lỗi hệ thống Server."));
    }
  }
}