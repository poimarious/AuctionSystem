package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class CloseAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CloseAuctionCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String auctionId = (String) request.getData();
      AuctionDAO auctionDAO = new AuctionDAO();
      Auction auction = auctionDAO.getAuctionById(auctionId);

      if (auction == null) {
        clientHandler.sendMessage(new Message("FAIL", "CLOSE_AUCTION", "Không tìm thấy phiên đấu giá."));
        return;
      }

      auction.setStatus(AuctionStatus.CANCELED);

      if (auctionDAO.updateAuctionState(auction)) {
        Auction inMemoryAuction = AuctionManager.getInstance().getAuctionById(auctionId);
        if (inMemoryAuction != null) {
          inMemoryAuction.setStatus(AuctionStatus.CANCELED);
        }
        clientHandler.sendMessage(new Message("SUCCESS", "CLOSE_AUCTION", auction));
      } else {
        clientHandler.sendMessage(new Message("FAIL", "CLOSE_AUCTION", "Lỗi DB khi cập nhật trạng thái."));
      }
    } catch (Exception e) {
      logger.error("Lỗi đóng phiên đấu giá: ", e);

    }
  }
}