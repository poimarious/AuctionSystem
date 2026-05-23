package org.deptrai.auctionsystem.server.commands.bid;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.ServerMain;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.*;
import org.deptrai.auctionsystem.server.exceptions.*;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.ServerThreadPool;
import org.deptrai.auctionsystem.shared.models.auction.*;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.users.*;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlaceBidCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    Object[] data = (Object[]) request.getData();
    String auctionId = (String) data[0];
    String currentUserId = (String) data[1];
    double bidAmount = (Double) data[2];

    try {
      UserDAO userDAO = new UserDAO();
      User currentUser = userDAO.getUserById(currentUserId);

      // 1. KIỂM TRA PHÂN QUYỀN
      if (!(currentUser instanceof Bidder)) {
        throw new AuthenticationException("Chỉ tài khoản Người mua (Bidder) mới có quyền đặt giá!");
      }
      Bidder bidder = (Bidder) currentUser;

      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
      if (auction == null) {
        throw new ResourceNotFoundException("Phiên đấu giá không tồn tại trên Server!");
      }

      // ================= Ổ KHÓA CHỐNG RACE CONDITION =================
      synchronized (auction) {
        // 2. KIỂM TRA TRẠNG THÁI (Dùng Custom Exception)
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID || auction.getStatus() == AuctionStatus.CANCELED) {
          throw new AuctionClosedException("Phiên đấu giá đã kết thúc, không thể đặt giá!");
        }

        // 3. KIỂM TRA GIÁ TRỊ ĐẶT (Dùng Custom Exception)
        if (bidAmount <= 0) {
          throw new InvalidBidException("Mức giá đặt phải lớn hơn 0!");
        }
        if (bidAmount <= auction.getCurrentPrice()) {
          throw new InvalidBidException("Mức giá phải lớn hơn giá hiện tại của sản phẩm!");
        }
        if (auction.getItem() != null && auction.getItem().getSeller() != null
            && currentUserId.equals(auction.getItem().getSeller().getUserId())) {
          throw new AuthenticationException("Người bán không được phép tự đặt giá cho sản phẩm của mình!");
        }

        // 4. KIỂM TRA SỐ DƯ (Dùng Custom Exception MỚI)
        double lockedBalance = getLockedBalance(bidder, auctionId);
        double totalRequiredBalance = lockedBalance + bidAmount;
        if (totalRequiredBalance > bidder.getBalance()) {
          throw new InsufficientBalanceException(String.format("Số dư không đủ! Bạn đã đặt giá tổng cộng $%.2f ở các phiên khác.", lockedBalance));
        }

        // NẾU VƯỢT QUA TOÀN BỘ BÀI TEST BÊN TRÊN -> LƯU VÀO DB
        Bid newBid = new Bid(bidder, auction, bidAmount, LocalDateTime.now());
        BidDAO bidDAO = new BidDAO();
        if (!bidDAO.insertBid(newBid)) {
          throw new Exception("Lỗi Database khi lưu lịch sử đặt giá."); // Lỗi hệ thống
        }

        // XỬ LÝ ANTI-SNIPING
        long THRESHOLD_SECONDS = 30;
        long EXTEND_SECONDS = 60;
        java.time.Duration remainingTime = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime());
        if (!remainingTime.isNegative() && remainingTime.getSeconds() <= THRESHOLD_SECONDS) {
          auction.setEndTime(auction.getEndTime().plusSeconds(EXTEND_SECONDS));
        }

        // CẬP NHẬT RAM & DB
        auction.setCurrentPrice(bidAmount);
        if (auction.getStatus() == AuctionStatus.OPEN) {
          auction.setStatus(AuctionStatus.RUNNING);
        }
        new AuctionDAO().updateAuctionState(auction);
        auction.getBids().add(newBid);

        // PHẢN HỒI THÀNH CÔNG CHO CLIENT ĐẶT GIÁ
        out.reset();
        out.writeObject(new Message("SUCCESS", "PLACE_BID", newBid));
        out.flush();
      }

      // BROADCAST CHO TẤT CẢ MỌI NGƯỜI VÀ GỬI THÔNG BÁO (Đoạn này giữ nguyên của bạn)
      ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));
      pushNotifications(auction, bidder, bidAmount);

    }
    // ================= LƯỚI BẮT LỖI NGHIỆP VỤ =================
    catch (AuthenticationException | ResourceNotFoundException | AuctionClosedException | InvalidBidException | InsufficientBalanceException businessError) {
      if (out != null) {
        out.reset();
        out.writeObject(new Message("FAIL", "PLACE_BID", businessError.getMessage()));
        out.flush();
      }
    }
    // ================= LƯỚI BẮT LỖI HỆ THỐNG =================
    catch (Exception systemError) {
      logger.error("Lỗi khi đặt giá: ", systemError);
      if (out != null) {
        out.reset();
        out.writeObject(new Message("ERROR", "PLACE_BID", "Lỗi hệ thống Server: " + systemError.getMessage()));
        out.flush();
      }
    }
  }

  // Hàm phụ trợ được tách ra cho class gọn gàng
  private double getLockedBalance(Bidder bidder, String auctionId) {
    double lockedBalance = 0.0;
    for (Auction a : AuctionManager.getInstance().getAllAuctions()) {
      if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.FINISHED) {
        Bidder topBidder = a.getWinner();
        if (topBidder != null && topBidder.getUserId().equals(bidder.getUserId()) && !a.getAuctionId().equals(auctionId)) {
          lockedBalance += a.getCurrentPrice();
        }
      }
    }
    return lockedBalance;
  }

  // Hàm phụ trợ gửi thông báo ngầm
  private void pushNotifications(Auction auction, Bidder bidder, double bidAmount) {
    ServerThreadPool.submitTask(() -> {
      NotificationDAO notiDAO = new NotificationDAO();
      Set<String> targetUserIds = new HashSet<>();

      // Thêm người bán vào danh sách nhận thông báo
      if (auction.getItem() != null && auction.getItem().getSeller() != null) {
        targetUserIds.add(auction.getItem().getSeller().getUserId());
      }

      // Thêm những người đã từng đặt giá vào danh sách
      for (Bid bid : auction.getBids()) {
        if (bid.getBidder() != null) {
          targetUserIds.add(bid.getBidder().getUserId());
        }
      }

      // Xóa chính bản thân người vừa đặt giá ra (không ai tự gửi thông báo cho chính mình)
      targetUserIds.remove(bidder.getUserId());

      String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      String notiMsg = timeStampStr + " || " + bidder.getUsername() + " vừa đặt giá cho " + auction.getItem().getName() + " lên $" + bidAmount;

      // Quét danh sách để gửi
      for (String userid : targetUserIds) {
        boolean isOnline = false;

        // Kiểm tra xem user này có đang online không
        for (ClientHandler client : ServerMain.activeClients) {
          if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(userid)) {
            client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", notiMsg));
            isOnline = true;
            break;
          }
        }

        // Nếu offline thì cất vào Database
        if (!isOnline) {
          notiDAO.insertNotification(userid, notiMsg);
        }
      }
    });
  }
}