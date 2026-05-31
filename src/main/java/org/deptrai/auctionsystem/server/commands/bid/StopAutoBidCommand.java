package org.deptrai.auctionsystem.server.commands.bid;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class StopAutoBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(StopAutoBidCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      String userId = (String) data[0];
      String auctionId = (String) data[1];

      AuctionManager.getInstance().unregisterAutoBid(userId, auctionId);

      clientHandler.sendMessage(new Message("SUCCESS", "STOP_AUTOBID", "Đã giải phóng tiền giam Auto-Bid"));
    } catch (Exception e) {
      logger.error("Lỗi khi dừng Auto-Bid: ", e);
      clientHandler.sendMessage(new Message("ERROR", "STOP_AUTOBID", "Lỗi hệ thống Server."));
    }
  }
}