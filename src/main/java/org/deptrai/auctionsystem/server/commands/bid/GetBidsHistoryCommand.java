package org.deptrai.auctionsystem.server.commands.bid;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.BidDAO;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class GetBidsHistoryCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetBidsHistoryCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String userId = (String) request.getData();
      if (userId == null || userId.trim().isEmpty()) {
        clientHandler.sendMessage(new Message("FAIL", "GET_BIDS_HISTORY", "ID người dùng không hợp lệ."));
        return;
      }

      clientHandler.sendMessage(new Message("SUCCESS", "GET_BIDS_HISTORY", new BidDAO().getBidsByBidderId(userId)));

    } catch (Exception e) {
      logger.error("Lỗi tải lịch sử đặt giá: ", e);
      clientHandler.sendMessage(new Message("ERROR", "GET_BIDS_HISTORY", "Lỗi hệ thống khi tải lịch sử."));
    }
  }
}