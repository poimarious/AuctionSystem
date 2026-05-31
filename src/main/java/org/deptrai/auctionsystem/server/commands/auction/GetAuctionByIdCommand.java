package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.exceptions.ResourceNotFoundException;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectOutputStream;

public class GetAuctionByIdCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetAuctionByIdCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String auctionId = (String) request.getData();

      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
      if (auction == null) {
        auction = new AuctionDAO().getAuctionById(auctionId);
      }

      if (auction == null) {
        throw new ResourceNotFoundException("Không tìm thấy phiên đấu giá này!");
      }

      clientHandler.sendMessage(new Message("SUCCESS", "GET_AUCTION_BY_ID",auction));

    } catch (ResourceNotFoundException e) {
      clientHandler.sendMessage(new Message("FAIL", "GET_AUCTION_BY_ID", e.getMessage()));
    } catch (Exception e) {
      logger.error("Lỗi lấy chi tiết Auction: ", e);
      clientHandler.sendMessage(new Message("ERROR", "GET_AUCTION_BY_ID", "Lỗi hệ thống Server."));
    }
  }
}