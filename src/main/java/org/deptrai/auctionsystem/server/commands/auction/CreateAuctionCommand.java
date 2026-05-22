package org.deptrai.auctionsystem.server.commands.auction;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class CreateAuctionCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(CreateAuctionCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      Object[] data = (Object[]) request.getData();
      Item item = (Item) data[0];
      LocalDateTime endTime = (LocalDateTime) data[1];
      byte[] imageBytes = (byte[]) data[2];
      String fileName = (String) data[3];

      // XỬ LÝ LƯU FILE ẢNH VÀO SERVER
      if (imageBytes != null && imageBytes.length > 0) {
        File uploadDir = new File("server_uploads");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
          logger.warn("Không thể tạo thư mục server_uploads");
        }

        String savePath = "server_uploads/" + System.currentTimeMillis() + "_" + fileName;
        Files.write(Paths.get(savePath), imageBytes);
        logger.info("Đã lưu ảnh sản phẩm tại Server: {}", savePath);

        item.setImageUrl(savePath);
        item.setImageBytes(imageBytes);
      }

      // 1. Lưu Item xuống DB
      ItemDAO itemDAO = new ItemDAO();
      if (!itemDAO.insertItem(item)) {
        out.writeObject(new Message("FAIL", "CREATE_AUCTION", "Lỗi Database khi lưu Vật phẩm."));
        out.flush();
        return;
      }

      // 2. Tạo Auction trong RAM
      Auction newAuction = AuctionManager.getInstance().createAuction(item, endTime);

      // 3. Lưu Auction mới này xuống DB
      AuctionDAO auctionDAO = new AuctionDAO();
      if (auctionDAO.insertAuction(newAuction)) {
        AuctionManager.getInstance().addAuctionToMemory(newAuction);
        out.reset();
        out.writeObject(new Message("SUCCESS", "CREATE_AUCTION", newAuction));
      } else {
        out.writeObject(new Message("FAIL", "CREATE_AUCTION", "Lỗi Database khi tạo Phiên đấu giá."));
      }
      out.flush();

    } catch (Exception e) {
      logger.error("Lỗi tạo phiên đấu giá: ", e);
      out.writeObject(new Message("ERROR", "CREATE_AUCTION", "Lỗi dữ liệu đầu vào."));
      out.flush();
    }
  }
}