package org.deptrai.auctionsystem.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.scene.control.PasswordField;
import org.deptrai.auctionsystem.server.dao.*;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.server.utils.ServerThreadPool;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus;
import org.deptrai.auctionsystem.shared.models.auction.AuctionSummary;
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.Admin;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.deptrai.auctionsystem.utils.ValidationUtils;

public class  ClientHandler implements Runnable {
  private final Socket socket;
  private final UserDAO userDAO;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private User authenticatedUser;

  public User getAuthenticatedUser() {
    return authenticatedUser;
  }

  public void setAuthenticatedUser(User authenticatedUser) {
    this.authenticatedUser = authenticatedUser;
  }

  public ClientHandler(Socket socket) {
    this.socket = socket;
    this.userDAO = new UserDAO();
  }

  public Socket getSocket() {
    return socket;
  }

  @Override
  public void run() {
    try {
      out = new ObjectOutputStream(socket.getOutputStream()); // Output always before
      in = new ObjectInputStream(socket.getInputStream());

      Message request;
      while ((request = (Message) in.readObject()) != null) {
        switch (request.getCommand()) {
          case "LOGIN":
            handleLogin(request);
            break;
          case "REGISTER":
            handleRegister(request);
            break;
          case "LOGOUT":
            handleLogout(request);
            break;
          case "CHANGE_BALANCE":
            handleChangeBalance(request);
            break;
          case "GET_ALL_AUCTIONS":
            handleGetAllAuctions(request);
            break;
          case "GET_AUCTION_BY_ID":
            handleGetAuctionById(request);
            break;
          case "CREATE_AUCTION":
            handleCreateAuction(request);
            break;
          case "CLOSE_AUCTION":
            handleCloseAuction(request);
            break;
          case "GET_SELLER_AUCTIONS":
            handleGetSellerAuctions(request);
            break;
          case "PLACE_BID":
            handlePlaceBid(request);
            break;
          case "UPDATE_PASSWORD":
            handleUpdatePassword(request);
            break;
          case "DELETE_AUCTION":
            handleDeleteAuction(request);
            break;
          case "GET_BIDS_HISTORY":
            handleGetBidsHistory(request);
            break;
          case "FINISH_AUCTION":
            handleFinishAuction(request);
            break;
          case "GET_NOTIFICATIONS":
            handleGetNotifications(request);
            break;
          case "CHECKOUT":
            handleCheckout(request);
            break;
          case "GET_ALL_USERS":
            handleGetAllUsers(request);
            break;
          case "GET_ALL_AUCTIONS_ADMIN":
            handleGetAllAuctionsAdmin(request);
            break;
          case "BAN_USER":
            handleBanUser(request);
            break;
          case "UNBAN_USER":
            handleUnbanUser(request);
            break;
          default:
            out.writeObject(
                new Message("FAIL", "COMMAND", "Lệnh không hợp lệ hoặc chưa được Server hỗ trợ!"));
            out.flush();
            break;
        }
      }
    } catch (Exception e) {
      System.out.println("Client ngắt kết nối.");
    } finally {
      try {
        ServerMain.activeClients.remove(this);
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void sendMessage(Message msg) {
    try {
      out.reset();
      out.writeObject(msg);
      out.flush();
    } catch (IOException e) {
      // Nếu lỗi tức là client này rớt mạng -> Rút cáp, xóa khỏi danh sách
      ServerMain.activeClients.remove(this);
    }
  }

  private void handleLogin(Message request) {
    // Data ta quy ước Client gửi sang là mảng String[] {username, password}
    String[] credentials = (String[]) request.getData();
    String username = credentials[0];
    String password = credentials[1];

    User user = userDAO.getUserByUsername(username);

    try {
      if (user != null && user.getPassword().equals(password)) {
        if (user.isBanned()) {
          out.reset();
          out.writeObject(new Message("FAIL", "LOGIN", "Tài khoản của bạn đã bị cấm!\nLý do: " + user.getBanReason()));
          out.flush();
          return;
        }
        this.authenticatedUser = user;
        out.writeObject(new Message("SUCCESS", "LOGIN", user));
      } else {
        out.writeObject(new Message("FAIL", "LOGIN", "Sai tên đăng nhập hoặc mật khẩu."));
      }
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleRegister(Message request) {
    String[] data = (String[]) request.getData();
    String username = data[0];
    String password = data[1];
    String email = data[2];
    String role = data[3];

    try {
      if (!ValidationUtils.isValidPassword(password)) {
        out.writeObject(
            new Message(
                "FAIL",
                "REGISTER",
                "Mật khẩu bao gồm chữ cái thường, chữ cái in hoa, số và kí tự đặc biệt!"));
        out.flush();
        return;
      } else if (userDAO.isUsernameTaken(username)) {
        out.writeObject(new Message("FAIL", "REGISTER", "Tên đăng nhập đã tồn tại!"));
        out.flush();
        return;
      } else if (userDAO.isEmailTaken(email)) {
        out.writeObject(
            new Message("FAIL", "REGISTER", "Email này đã được sử dụng cho một tài khoản khác!"));
        out.flush();
        return;
      }

      User newUser; // Admin account cannot be registered
      if (role.equals("SELLER")) {
        newUser = new Seller(null, username, password, email);
      } else { // role.equals("BIDDER")
        newUser =
            new Bidder(
                null, username, password, email, new java.util.concurrent.CopyOnWriteArrayList<>());
      }

      boolean success = userDAO.insertUser(newUser, role);
      if (success) {
        out.writeObject(new Message("SUCCESS", "REGISTER", "Đăng ký thành công"));
      } else {
        out.writeObject(new Message("FAIL", "REGISTER", "Lỗi DB khi tạo tài khoản."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleLogout(Message request) {
    // Đưa đường truyền Socket này về trạng thái vô danh (Guest)
    this.setAuthenticatedUser(null);

    try {
      out.reset();
      out.writeObject(new Message("SUCCESS", "LOGOUT", "Đã đăng xuất an toàn khỏi Server"));
      out.flush();
      System.out.println(">> [SERVER] Một Client đã hủy Session và đăng xuất thành công.");
    } catch (Exception e) {
      System.err.println("Lỗi gửi phản hồi đăng xuất: " + e.getMessage());
    }
  }

  // For top-up buttons (Dùng cho mấy nút nạp tiền ấy)
  private void handleChangeBalance(Message request) {
    // Dữ liệu Client gửi sang sẽ gồm: [userId, amount]
    Object[] data = (Object[]) request.getData();
    String userId = (String) data[0];
    double amount = (Double) data[1];

    User user = userDAO.getUserById(userId);
    if (user != null) {
      double newBalance = user.getBalance() + amount;
      if(user instanceof Seller) {
        newBalance = user.getBalance() - amount;
        if(newBalance < 0) {
          try {
            out.writeObject(new Message("FAIL", "CHANGE_BALANCE", "số tiền rút phải nhỏ hơn số dư!"));
            out.flush();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return ;
        }
      }
      if (userDAO.updateBalance(userId, newBalance)) {
        try {
          out.writeObject(new Message("SUCCESS", "CHANGE_BALANCE", newBalance));
          out.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return;
      }
    }
    try {
      out.writeObject(new Message("FAIL", "CHANGE_BALANCE", "Lỗi cập nhật số dư."));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void handleGetAllAuctions(Message request) {
    try {
      // 1. Nhận userId từ Client (Có thể null nếu người dùng chưa đăng nhập - Guest)
      String userId = null;
      if (request.getData() instanceof String) {
        userId = (String) request.getData();
      }

      List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();
      List<AuctionSummary> activeAuctionsDTO = new ArrayList<>();

      for (Auction auction : allAuctions) {
        // Điều kiện 1: Phiên đang mở
        boolean isOngoing = (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING);

        // Điều kiện 2: Phiên đã kết thúc HOẶC đã thanh toán, và người đang đăng nhập là người chiến thắng
        boolean isWonByMe = false;
        if (userId != null && (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID)) {
          Bidder winner = auction.getWinner();
          if (winner != null && winner.getUserId().equals(userId) || auction.getItem().getSeller().getUserId().equals(userId)) {
            isWonByMe = true;
          }
        }

        // Nếu thỏa mãn 1 trong 2 điều kiện thì gửi về Client
        if (isOngoing || isWonByMe) {
          AuctionSummary auctionSummary = new AuctionSummary(
                  auction.getAuctionId(),
                  auction.getItem().getName(),
                  auction.getItem().getDescription(),
                  auction.getItem().getCategory(),
                  auction.getCurrentPrice(),
                  auction.getStatus(),
                  auction.getEndTime(),
                  auction.getItem().getImageUrl(),
                  auction.getItem().getImageBytes()
          );

          activeAuctionsDTO.add(auctionSummary);
        }
      }

      out.writeObject(new Message("SUCCESS", "GET_ALL_AUCTIONS", activeAuctionsDTO));
      out.flush();

    } catch (Exception e) {
      System.out.println("Lỗi khi gửi danh sách Auction cho Client.");
      e.printStackTrace();
    }
  }

  private void handleGetAuctionById(Message request) {
    try {
      String auctionId = (String) request.getData();

      // 1. Tìm trong RAM (Manager) trước
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      // 2. Nếu RAM không có (có thể server vừa khởi động lại), tìm trong DB
      if (auction == null) {
        AuctionDAO auctionDAO = new AuctionDAO();
        auction = auctionDAO.getAuctionById(auctionId);
      }

      if (auction != null) {
        out.writeObject(new Message("SUCCESS", "GET_AUCTION_BY_ID", auction));
      } else {
        out.writeObject(
            new Message("FAIL", "GET_AUCTION_BY_ID", "Không tìm thấy phiên đấu giá này!"));
      }
      out.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleCreateAuction(Message request) {
    try {
      // Quy ước dữ liệu gửi sang: Object[] {Item item, LocalDateTime endTime, imagebytes, filename}
      // }
      Object[] data = (Object[]) request.getData();
      Item item = (Item) data[0];
      LocalDateTime endTime = (LocalDateTime) data[1];
      byte[] imageBytes = (byte[]) data[2];
      String fileName = (String) data[3];
      // --- XỬ LÝ LƯU FILE ẢNH VÀO SERVER ---
      if (imageBytes != null && imageBytes.length > 0) {
        // Tạo thư mục "uploads" trên Server nếu chưa có
        File uploadDir = new File("server_uploads");
        if (!uploadDir.exists()) {
          uploadDir.mkdir();
        }

        // Tạo đường dẫn lưu file (Thêm timestamp để không bị trùng tên)
        String savePath = "server_uploads/" + System.currentTimeMillis() + "_" + fileName;
        Files.write(Paths.get(savePath), imageBytes);

        System.out.println("Đã lưu ảnh sản phẩm tại Server: " + savePath);

        item.setImageUrl(savePath);
        item.setImageBytes(imageBytes);
      }
      // Bước 1: Lưu Item xuống DB trước
      ItemDAO itemDAO = new ItemDAO();
      boolean isItemSaved = itemDAO.insertItem(item);

      if (!isItemSaved) {
        out.writeObject(new Message("FAIL", "CREATE_AUCTION", "Lỗi Database khi lưu Vật phẩm."));
        out.flush();
        return;
      }

      // Bước 2: Tạo Auction trong RAM qua Manager (Tự động sinh AuctionID)
      Auction newAuction = AuctionManager.getInstance().createAuction(item, endTime);

      // Bước 3: Lưu Auction mới này xuống DB
      AuctionDAO auctionDAO = new AuctionDAO();
      boolean isAuctionSaved = auctionDAO.insertAuction(newAuction);

      if (isAuctionSaved) {
        // Gửi trả về đối tượng Auction đã hoàn chỉnh (kèm ID) để Client cập nhật UI
        // 3. ĐỒNG BỘ: Chỉ khi DB thành công mới đưa vào RAM
        AuctionManager.getInstance().addAuctionToMemory(newAuction);
        out.writeObject(new Message("SUCCESS", "CREATE_AUCTION", newAuction));
      } else {
        out.writeObject(
            new Message("FAIL", "CREATE_AUCTION", "Lỗi Database khi tạo Phiên đấu giá."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(new Message("ERROR", "CREATE_AUCTION", "Lỗi dữ liệu đầu vào."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handleCloseAuction(Message request) {
    try {
      // Nhận ID phiên đấu giá cần đóng
      String auctionId = (String) request.getData();

      AuctionDAO auctionDAO = new AuctionDAO();
      Auction auction = auctionDAO.getAuctionById(auctionId);

      if (auction == null) {
        out.writeObject(new Message("FAIL", "CLOSE_AUCTION", "Không tìm thấy phiên đấu giá."));
        out.flush();
        return;
      }

      // Đổi trạng thái thành Đóng (Giả sử bạn có Enum AuctionStatus.CLOSED)
      auction.setStatus(AuctionStatus.CANCELED);

      // Cập nhật xuống Database
      boolean isUpdated = auctionDAO.updateAuctionState(auction);

      if (isUpdated) {
        // Cập nhật đồng bộ lại trên RAM (AuctionManager)
        Auction inMemoryAuction = AuctionManager.getInstance().getAuctionById(auctionId);
        if (inMemoryAuction != null) {
          inMemoryAuction.setStatus(AuctionStatus.CANCELED);
        }

        out.writeObject(new Message("SUCCESS", "CLOSE_AUCTION", auction));
      } else {
        out.writeObject(new Message("FAIL", "CLOSE_AUCTION", "Lỗi DB khi cập nhật trạng thái."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleGetSellerAuctions(Message request) {
    try {
      // Quy ước dữ liệu gửi sang: String clientID
      // 1. Nhận sellerId từ Client gửi lên
      String sellerId = (String) request.getData();

      // 2. Lấy toàn bộ danh sách từ RAM
      List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();
      List<Auction> sellerAuctions = new ArrayList<>();

      // 3. Lọc ra những phiên đấu giá có chứa Item do Seller này đăng bán
      for (Auction auction : allAuctions) {
        if (auction.getItem() != null
            && auction.getItem().getSeller() != null
            && auction.getItem().getSeller().getUserId().equals(sellerId)) {

          sellerAuctions.add(auction);
        }
      }

      // 4. Đóng gói và gửi trả về cho Client
      out.writeObject(new Message("SUCCESS", "GET_SELLER_AUCTIONS", sellerAuctions));
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(
            new Message("FAIL", "GET_SELLER_AUCTIONS", "Lỗi khi tải danh sách kho hàng."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handlePlaceBid(Message request) {
    try {
      // Dữ liệu Client gửi sang gồm : [auctionId, currentUserId, bidAmount]
      Object[] data = (Object[]) request.getData();
      String auctionId = (String) data[0];
      String currentUserId = (String) data[1];
      double bidAmount = (Double) data[2];

      UserDAO userDAO = new UserDAO();
      User currentUser = userDAO.getUserById(currentUserId);

      if (!(currentUser instanceof Bidder bidder)) {
        out.writeObject(
            new Message(
                "FAIL", "PLACE_BID", "Chỉ tài khoản Người mua (Bidder) mới có quyền đặt giá!"));
        out.flush();
        return;
      }

      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
      if (auction == null) {
        out.writeObject(
            new Message("FAIL", "PLACE_BID", "Phiên đấu giá không tồn tại trên Server!"));
        out.flush();
        return;
      }
      synchronized (auction) {
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID || auction.getStatus() == AuctionStatus.CANCELED) {
          out.writeObject(new Message("FAIL", "PLACE_BID", "Phiên đấu giá đã kết thúc, không thể đặt giá!"));
          out.flush();
          return;
        }//Check for invalid when auction status is finished or not
        Bid newBid = new Bid(bidder, auction, bidAmount, LocalDateTime.now());

        // 5. Bid validate check
        try {
          newBid.validate();
        } catch (Exception validationException) {
          // Bắt thông báo lỗi từ Exception và gửi thẳng về cho Client hiển thị
          out.writeObject(new Message("FAIL", "PLACE_BID", validationException.getMessage()));
          out.flush();
          return;
        }

        // ================= KIỂM TRA SỐ DƯ ĐỘNG TRÊN RAM (ON-THE-FLY BALANCE CHECK) =================
        double lockedBalance = 0.0;
        List<Auction> allAuctions = AuctionManager.getInstance().getAllAuctions();

        for (Auction a : allAuctions) {
          if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
            Bidder topBidder = a.getWinner();
            // Nếu User hiện tại đang là người dẫn đầu ở một phiên đấu giá
            if (topBidder != null && topBidder.getUserId().equals(bidder.getUserId())) {
              // Chỉ cộng tiền giam nếu đó là một phiên đấu giá KHÁC.
              // Nếu là phiên hiện tại, ta bỏ qua để lát nữa tính bằng mức giá mới (bidAmount)
              if (!a.getAuctionId().equals(auctionId)) {
                lockedBalance += a.getCurrentPrice();
              }
            }
          }
        }

        double totalRequiredBalance = lockedBalance + bidAmount;

        if (totalRequiredBalance > bidder.getBalance()) {
          out.writeObject(new Message("FAIL", "PLACE_BID",
                  String.format("Số dư không đủ! Bạn đã đặt giá tổng cộng $%.2f ở các phiên khác.", lockedBalance)));
          out.flush();
          return;
        }
        // =========================================================================================
        // 6. Save data to database
        BidDAO bidDAO = new BidDAO();
        boolean isBidSaved = bidDAO.insertBid(newBid); // Gọi đúng 1 tham số
  //      userDAO.updateBalance(bidder.getUserId(), bidder.getBalance() - bidAmount); DO NOT change balance yet

        if (!isBidSaved) {
          out.writeObject(new Message("FAIL", "PLACE_BID", "Lỗi Database khi lưu lịch sử đặt giá."));
          out.flush();
          return;
        }

        // ================= TÍNH NĂNG ANTI-SNIPER (GIA HẠN THỜI GIAN) =================
        // x = 30 giây (Thời gian chót), y = 60 giây (Thời gian được cộng thêm)
        long THRESHOLD_SECONDS = 30;
        long EXTEND_SECONDS = 60;

        java.time.Duration remainingTime = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime());

        // Nếu thời gian còn lại nhỏ hơn hoặc bằng 30 giây (và phiên chưa kết thúc)
        if (!remainingTime.isNegative() && remainingTime.getSeconds() <= THRESHOLD_SECONDS) {
          // Cộng thêm 60s vào thời gian kết thúc
          auction.setEndTime(auction.getEndTime().plusSeconds(EXTEND_SECONDS));
          System.out.println(" Phiên [" + auction.getItem().getName() + "] được gia hạn thêm " + EXTEND_SECONDS + "s");
        }
        // =============================================================================

        // update new price on price board
        auction.setCurrentPrice(bidAmount);
        if (auction.getStatus() == AuctionStatus.OPEN) {
          auction.setStatus(AuctionStatus.RUNNING);
        }

        // Lúc này hàm updateAuctionState sẽ tự động lưu cả giá mới, trạng thái mới và endTime mới
        AuctionDAO auctionDAO = new AuctionDAO();
        boolean isAuctionUpdated = auctionDAO.updateAuctionState(auction);

        // Update RAM data
        auction.getBids().add(newBid);
        out.reset();
        out.writeObject(new Message("SUCCESS", "PLACE_BID", newBid));
        out.flush();
      }


      // Khi Broadcast được gửi đi, gói bưu kiện 'auction' này đã mang theo endTime mới!
      ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));

      ServerThreadPool.submitTask(()-> {
        NotificationDAO notiDAO = new NotificationDAO();
        Set<String> targetUserIds = new HashSet<>();

        if(auction.getItem() != null && auction.getItem().getSeller() != null) {
          targetUserIds.add(auction.getItem().getSeller().getUserId());
        }

        for(Bid bid : auction.getBids()) {
          if(bid.getBidder() != null) {
            targetUserIds.add(bid.getBidder().getUserId());
          }
        }

        targetUserIds.remove(bidder.getUserId());

        String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String notiMsg = timeStampStr + " || " + bidder.getUsername() + " vừa đặt giá cho " + auction.getItem().getName() +
                " lên $" + bidAmount;

        for(String userid : targetUserIds) {
          boolean isOnline = false;

          for(ClientHandler client : ServerMain.activeClients) {
            if(client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(userid)) {
              client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", notiMsg));
              isOnline = true;
              break;
            }
          }
          if(!isOnline) {
            notiDAO.insertNotification(userid, notiMsg);
          }
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(new Message("ERROR", "PLACE_BID", "Lỗi xử lý dữ liệu đặt giá tại Server."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handleUpdatePassword(Message request) {
    try {
      // QUY ƯỚC DỮ LIỆU TỪ CLIENT GỬI LÊN (Payload):
      // Dữ liệu là một mảng String[] gồm 3 phần tử: {userId, currentPassword, newPassword}
      String[] data = (String[]) request.getData();
      String userId = data[0];
      String currentPassword = data[1];
      String newPassword = data[2];

      // 1. Kiểm tra độ mạnh của mật khẩu mới theo chuẩn ValidationUtils
      if (!ValidationUtils.isValidPassword(newPassword)) {
        out.writeObject(
            new Message(
                "FAIL",
                "UPDATE_PASSWORD",
                "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@#$%^&+=!)"));
        out.flush();
        return;
      }

      // 2. Xác thực lại với Database (Bảo mật lớp 2)

      User user = userDAO.getUserById(userId);
      if (user == null) {
        out.writeObject(
            new Message("FAIL", "UPDATE_PASSWORD", "Tài khoản không tồn tại trên hệ thống!"));
        out.flush();
        return;
      }

      if (!user.getPassword().equals(currentPassword)) {
        out.writeObject(new Message("FAIL", "UPDATE_PASSWORD", "Mật khẩu hiện tại không đúng!"));
        out.flush();
        return;
      }

      // 3. Thực hiện lưu mật khẩu mới xuống CSDL
      boolean isUpdated = userDAO.updatePassword(userId, newPassword);

      // 4. Trả kết quả về cho Client
      if (isUpdated) {
        out.writeObject(new Message("SUCCESS", "UPDATE_PASSWORD", "Cập nhật mật khẩu thành công!"));
      } else {
        out.writeObject(
            new Message("FAIL", "UPDATE_PASSWORD", "Lỗi hệ thống khi cập nhật mật khẩu."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(
            new Message("ERROR", "UPDATE_PASSWORD", "Định dạng dữ liệu gửi lên không hợp lệ."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handleDeleteAuction(Message request) {
    try {
      String auctionId = (String) request.getData();

      // TỐI ƯU: Lấy thẳng Auction từ RAM để trích xuất itemId
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      if (auction == null || auction.getItem() == null) {
        out.writeObject(
            new Message("FAIL", "DELETE_AUCTION", "Không tìm thấy phiên đấu giá trên hệ thống!"));
        out.flush();
        return;
      }

      String itemId = auction.getItem().getItemId();
      AuctionDAO auctionDAO = new AuctionDAO();

      // Truyền cả 2 ID xuống để xóa dứt điểm trong 1 giao dịch
      boolean isDeleted = auctionDAO.deleteAuctionById(auctionId, itemId);

      if (isDeleted) {
        // ĐỒNG BỘ: Xóa khỏi RAM
        AuctionManager.getInstance().removeAuctionFromMemory(auctionId);
        out.writeObject(
            new Message("SUCCESS", "DELETE_AUCTION", "Xóa triệt để phiên đấu giá thành công!"));
      } else {
        out.writeObject(new Message("FAIL", "DELETE_AUCTION", "Lỗi cơ sở dữ liệu khi xóa."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(
            new Message("ERROR", "DELETE_AUCTION", "Lỗi xử lý yêu cầu xóa tại Server."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handleGetBidsHistory(Message request) {
    // Quy ước dữ liệu:{UserId}
    try {
      String userId = (String) request.getData();
      if (userId == null || userId.trim().isEmpty()) {
        out.writeObject(new Message("FAIL", "GET_BIDS_HISTORY", "ID người dùng không hợp lệ."));
        out.flush();
        return;
      }
      BidDAO bidDAO = new BidDAO();
      List<Bid> bidList = bidDAO.getBidsByBidderId(userId);
      out.writeObject(new Message("SUCCESS", "GET_BIDS_HISTORY", bidList));
      out.flush();

    } catch (Exception e) {
      System.out.println("Lỗi khi gửi danh sách bid cho Client");
      e.printStackTrace();
      try {
        out.writeObject(
            new Message("ERROR", "GET_BIDS_HISTORY", "Lỗi hệ thống khi tải lịch sử đặt giá."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

  private void handleFinishAuction(Message request) {
    try {
      String auctionId = (String) request.getData();
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      if (auction == null) {
        out.writeObject(new Message("FAIL", "FINISH_AUCTION", "Không tìm thấy phiên đấu giá."));
        out.flush();
        return;
      }

      // ================= VÙNG AN TOÀN ĐA LUỒNG (THREAD-SAFE) =================
      // Ổ khóa chặn đứng 99 request spam và ngăn chặn xung đột với lệnh Đặt giá (Place Bid)
      synchronized (auction) {
        // CHẶN SPAM: Chỉ xử lý nếu phiên đấu giá đang ở trạng thái OPEN hoặc RUNNING
        if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {

          // KIỂM TRA BẢO MẬT: Đảm bảo thời gian hiện tại trên Server thực sự đã vượt qua endTime
          if (!LocalDateTime.now().isBefore(auction.getEndTime())) {

            // ĐÃ SỬA: Dù có lượt đặt giá hay không, khi hết giờ trạng thái luôn là FINISHED
            auction.setStatus(AuctionStatus.FINISHED);

            // 2. Lưu trạng thái FINISHED mới này xuống Database
            AuctionDAO auctionDAO = new AuctionDAO();
            boolean isUpdated = auctionDAO.updateAuctionState(auction);

            if (isUpdated) {
              System.out.println("Phiên đấu giá [" + auction.getItem().getName() + "] đã KẾT THÚC.");

              // 3. Phát Broadcast cho toàn bộ Client đang online để cập nhật lại giao diện thẻ sản phẩm công khai
              ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));

              // ==================== XỬ LÝ HỆ THỐNG THÔNG BÁO (ONLINE/OFFLINE) ====================
              ServerThreadPool.submitTask(() -> {
                NotificationDAO notiDAO = new NotificationDAO();
                String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String itemName = auction.getItem().getName();

                String sellerId = null;
                if (auction.getItem() != null && auction.getItem().getSeller() != null) {
                  sellerId = auction.getItem().getSeller().getUserId();
                }

                // Dùng Map để định danh [ID Người nhận -> Nội dung thông báo tương ứng]
                Map<String, String> targetUsers = new HashMap<>();

                // Lấy thông tin người thắng cuộc từ danh sách bid trên RAM
                Bidder winner = auction.getWinner();
                String winnerId = (winner != null) ? winner.getUserId() : null;
                double finalPrice = auction.getCurrentPrice();

                // 1. Phân loại thông báo gửi cho Người Bán (Seller)
                if (sellerId != null) {
                  if (winner == null) {
                    targetUsers.put(sellerId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC nhưng không có người đặt giá.");
                  } else {
                    targetUsers.put(sellerId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC và được bán thành công với mức giá $" + finalPrice + ".");
                  }
                }

                // 2. Phân loại thông báo gửi cho những Người Mua (Bidders) đã từng tham gia đặt giá
                if (winner != null) {
                  Set<String> participantIds = new HashSet<>();
                  for (Bid bid : auction.getBids()) {
                    if (bid.getBidder() != null) {
                      participantIds.add(bid.getBidder().getUserId());
                    }
                  }

                  // Quét qua danh sách để phân định rạch ròi ai Thắng - ai Thua
                  for (String pId : participantIds) {
                    if (pId.equals(winnerId)) {
                      targetUsers.put(pId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC. Chúc mừng, bạn đã THẮNG với mức giá $" + finalPrice + "!");
                    } else {
                      targetUsers.put(pId, timeStampStr + " || Phiên đấu giá [" + itemName + "] đã KẾT THÚC. Rất tiếc, bạn đã THUA người ra giá cao nhất.");
                    }
                  }
                }

                // 3. Tiến hành phân phối thông báo đến đích (Online đẩy qua Socket, Offline găm vào DB)
                for (java.util.Map.Entry<String, String> entry : targetUsers.entrySet()) {
                  String targetUserId = entry.getKey();
                  String msgText = entry.getValue();
                  boolean isOnline = false;

                  for (ClientHandler client : ServerMain.activeClients) {
                    if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(targetUserId)) {
                      client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", msgText));
                      isOnline = true;
                      break;
                    }
                  }

                  if (!isOnline) {
                    notiDAO.insertNotification(targetUserId, msgText);
                  }
                }
              });

            }
          }
        }
      }

      out.writeObject(new Message("SUCCESS", "FINISH_AUCTION", "Đã xử lý xong"));
      out.flush();
    } catch (Exception e) {
      System.out.println("Lỗi khi kết thúc phiên đấu giá: " + e.getMessage());
    }
  }

  private void handleGetNotifications(Message request) {
    try {
      String userId = (String) request.getData();
      NotificationDAO notiDAO = new NotificationDAO();

      // Lấy danh sách chưa đọc từ DB lên
      List<String> unreadNotifs = notiDAO.getUnreadNotifications(userId);

      out.writeObject(new Message("SUCCESS", "GET_NOTIFICATIONS", unreadNotifs));
      out.flush();

      // Đọc xong thì tự động xóa trong DB
      notiDAO.deleteNotificationsByUserId(userId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleCheckout(Message request) {
    try {
      String auctionId = (String) request.getData();
      Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

      // 1. KIỂM TRA TRẠNG THÁI: Chỉ cho phép thanh toán nếu đã FINISHED
      if (auction == null || auction.getStatus() != AuctionStatus.FINISHED) {
        out.writeObject(new Message("FAIL", "CHECKOUT", "Phiên đấu giá không hợp lệ hoặc đã được thanh toán."));
        out.flush();
        return;
      }

      Bidder winner = auction.getWinner();
      if (winner == null) {
        out.writeObject(new Message("FAIL", "CHECKOUT", "Không có người chiến thắng để thanh toán."));
        out.flush();
        return;
      }

      UserDAO userDAO = new UserDAO();


      // Lấy thông tin cả Người Mua và Người Bán để đảm bảo số dư không bị lệch
      User dbWinner = userDAO.getUserById(winner.getUserId());
      String sellerId = auction.getItem().getSeller().getUserId();
      User dbSeller = userDAO.getUserById(sellerId);

      double finalPrice = auction.getCurrentPrice();


      if (dbWinner.getBalance() < finalPrice) {
        out.writeObject(new Message("FAIL", "CHECKOUT", "Tài khoản không đủ số dư để thanh toán!"));
        out.flush();
        return;
      }

      // 3. THỰC HIỆN CHUYỂN TIỀN (TRANSACTION)
      // Bước 3.1: Trừ tiền người mua
      boolean isBuyerDeducted = userDAO.updateBalance(dbWinner.getUserId(), dbWinner.getBalance() - finalPrice);

      if (isBuyerDeducted) {
        // Bước 3.2: Cộng tiền cho người bán
        boolean isSellerAdded = userDAO.updateBalance(sellerId, dbSeller.getBalance() + finalPrice);

        if (isSellerAdded) {
          // 4. CẬP NHẬT TRẠNG THÁI THÀNH PAID
          auction.setStatus(AuctionStatus.PAID);

          AuctionDAO auctionDAO = new AuctionDAO();
          auctionDAO.updateAuctionState(auction); // Lưu trạng thái mới xuống DB

          // Phát loa thông báo cho TẤT CẢ các Client cập nhật lại UI (Đổi nhãn thành Đã thanh toán)
          ServerMain.broadcast(new Message("SUCCESS", "AUCTION_UPDATE", auction));
          ServerThreadPool.submitTask(() -> {
            NotificationDAO notiDAO = new NotificationDAO();
            String timeStampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String itemName = auction.getItem().getName();

            HashMap<String,String> usersMessage = new HashMap<>();
            usersMessage.put(sellerId,"Phiên đấu giá " + itemName + " của bạn đã được thanh toán bởi " + dbWinner.getUsername() + " với số tiền: " + finalPrice + "$.");
            usersMessage.put(dbWinner.getUserId(),"Bạn đã thanh toán thành công số tiền " + finalPrice + "$ cho phiên đấu giá " + itemName + ".");

            for(Map.Entry<String,String> users : usersMessage.entrySet()) {
              String targetUserId = users.getKey();
              String targetMessage = users.getValue();
              Boolean isOnline = false;

              for (ClientHandler client : ServerMain.activeClients) {
                if (client.getAuthenticatedUser() != null && client.getAuthenticatedUser().getUserId().equals(targetUserId)) {
                  client.sendMessage(new Message("SUCCESS", "PUSH_NOTIFICATION_BELL", targetMessage));
                  isOnline = true;
                  break;
                }
              }
              if(!isOnline) {
                notiDAO.insertNotification(targetUserId,targetMessage);
              }
            }
          });

          out.writeObject(new Message("SUCCESS", "CHECKOUT", "Thanh toán thành công!"));
        } else {
          out.writeObject(new Message("FAIL", "CHECKOUT", "Lỗi chuyển tiền cho người bán."));
        }
      } else {
        out.writeObject(new Message("FAIL", "CHECKOUT", "Lỗi khi trừ tiền người mua."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(new Message("ERROR", "CHECKOUT", "Lỗi Server khi xử lý thanh toán."));
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void handleBanUser(Message request) {
    // Payload: Object[]{ AdminUser, TargetUserId, BanReason }
    Object[] data = (Object[]) request.getData();
    User requester = (User) data[0];
    String targetUserId = (String) data[1];
    String banReason = (String) data[2];

    // Phân quyền: Kiểm tra đúng Admin cấp 2 không
    if (!(requester instanceof Admin) || ((Admin) requester).getAdminLevel() < 2) {
      try {
        out.writeObject(new Message("FAIL", "BAN_USER", "Bạn không có quyền Ban người dùng!"));
        out.flush();
      } catch (Exception e) {}
      return;
    }

    boolean success = userDAO.banUser(targetUserId, banReason);

    if (success) {
      // Ban người dùng nếu họ đang online
      ClientHandler onlineTarget = ServerMain.getClientByUserId(targetUserId);
      if (onlineTarget != null) {
        // Gửi lệnh FORCE_LOGOUT thẳng xuống luồng ngầm của người bị ban
        onlineTarget.sendMessage(new Message("UPDATE", "FORCE_LOGOUT", "Tài khoản của bạn vừa bị cấm bởi Admin!\nLý do: " + banReason));

        onlineTarget.setAuthenticatedUser(null);
      }

      try {
        out.reset();
        out.writeObject(new Message("SUCCESS", "BAN_USER", "Đã ban người dùng thành công!"));
        out.flush();
      } catch (Exception e) {}
    }
  }

  private void handleUnbanUser(Message request) {
    try {
      // Payload: Object[]{ AdminUser, TargetUserId }
      Object[] data = (Object[]) request.getData();
      User requester = (User) data[0];
      String targetUserId = (String) data[1];

      // Kiểm tra đúng Admin cấp 2 mới được Gỡ Ban
      if (!(requester instanceof Admin) || ((Admin) requester).getAdminLevel() < 2) {
        out.writeObject(new Message("FAIL", "UNBAN_USER", "Bạn không có quyền Gỡ cấm người dùng!"));
        out.flush();
        return;
      }

      boolean success = userDAO.unbanUser(targetUserId);

      if (success) {
        out.writeObject(new Message("SUCCESS", "UNBAN_USER", "Đã gỡ cấm thành công!"));
      } else {
        out.writeObject(new Message("FAIL", "UNBAN_USER", "Lỗi CSDL khi gỡ cấm."));
      }
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleGetAllUsers(Message request) {
    try {
      List<User> users = userDAO.getAllUsers();

      out.reset();
      out.writeObject(new Message("SUCCESS", "GET_ALL_USERS", users));
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleGetAllAuctionsAdmin(Message request) {
    try {
      // Lấy danh sách full hd không che dành riêng cho Admin
      List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();

      out.reset();
      out.writeObject(new org.deptrai.auctionsystem.shared.network.Message("SUCCESS", "GET_ALL_AUCTIONS_ADMIN", auctions));
      out.flush();
    } catch (Exception e) {
      System.out.println("Lỗi gửi danh sách cho Admin: " + e.getMessage());
    }
  }
}
