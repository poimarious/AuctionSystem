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

public class DeleteAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(DeleteAuctionCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String auctionId = (String) request.getData();
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      if (auction == null || auction.getItem() == null) {
        out.writeObject(new Message("FAIL", "DELETE_AUCTION", "Không tìm thấy phiên đấu giá trên hệ thống!"));
        out.flush();
        return;
      }

      synchronized (auction) {
        String itemId = auction.getItem().getItemId();
        AuctionDAO auctionDAO = new AuctionDAO();

        if (auctionDAO.deleteAuctionById(auctionId, itemId)) {
          AuctionManager.getInstance().removeAuctionFromMemory(auctionId);
          auction.setStatus(AuctionStatus.CANCELED); // Ẩn khỏi main page

          out.reset();
          out.writeObject(new Message("SUCCESS", "DELETE_AUCTION", "Xóa triệt để phiên đấu giá thành công!"));
        } else {
          out.writeObject(new Message("FAIL", "DELETE_AUCTION", "Lỗi cơ sở dữ liệu khi xóa."));
        }
      }
      out.flush();

    } catch (Exception e) {
      logger.error("Lỗi xóa phiên đấu giá: ", e);
      out.writeObject(new Message("ERROR", "DELETE_AUCTION", "Lỗi xử lý yêu cầu xóa tại Server."));
      out.flush();
    }
  }
}