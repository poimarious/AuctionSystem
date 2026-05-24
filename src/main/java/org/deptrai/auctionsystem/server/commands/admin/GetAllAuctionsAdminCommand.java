package org.deptrai.auctionsystem.server.commands.admin;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.network.Message;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetAllAuctionsAdminCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetAllAuctionsAdminCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      clientHandler.sendMessage(new Message("SUCCESS", "GET_ALL_AUCTIONS_ADMIN", AuctionManager.getInstance().getAllAuctions()));
    } catch (Exception e) {
      logger.error("Lỗi lấy danh sách đấu giá admin: ", e);
      clientHandler.sendMessage(new Message("ERROR", "GET_ALL_AUCTIONS_ADMIN", "Lỗi hệ thống Server."));
    }
  }
}