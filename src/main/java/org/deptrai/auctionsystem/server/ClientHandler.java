package org.deptrai.auctionsystem.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import org.deptrai.auctionsystem.server.dao.AuctionDAO;
import org.deptrai.auctionsystem.server.dao.BidDAO;
import org.deptrai.auctionsystem.server.dao.ItemDAO;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.models.auction.Auction;
import org.deptrai.auctionsystem.shared.models.auction.AuctionStatus; // Giả định bạn có enum này
import org.deptrai.auctionsystem.shared.models.bid.Bid;
import org.deptrai.auctionsystem.shared.models.items.Item;
import org.deptrai.auctionsystem.shared.models.users.Bidder;
import org.deptrai.auctionsystem.shared.models.users.Seller;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.deptrai.auctionsystem.utils.ValidationUtils;

public class ClientHandler implements Runnable {
  private final Socket socket;
  private final UserDAO userDAO;
  private ObjectOutputStream out;
  private ObjectInputStream in;

  public ClientHandler(Socket socket) {
    this.socket = socket;
    this.userDAO = new UserDAO();
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
          case "TOP_UP":
            handleTopUp(request);
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
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
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

  // For top-up buttons (Dùng cho mấy nút nạp tiền ấy)
  private void handleTopUp(Message request) {
    // Dữ liệu Client gửi sang sẽ gồm: [userId, amount]
    Object[] data = (Object[]) request.getData();
    String userId = (String) data[0];
    double amount = (Double) data[1];

    User user = userDAO.getUserById(userId);
    if (user != null) {
      double newBalance = user.getBalance() + amount;
      if (userDAO.updateBalance(userId, newBalance)) {
        try {
          out.writeObject(new Message("SUCCESS", "TOP_UP", newBalance));
          out.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return;
      }
    }
    try {
      out.writeObject(new Message("FAIL", "TOP_UP", "Lỗi cập nhật số dư."));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void handleGetAllAuctions(Message request) {

    try {
      // Lấy toàn bộ danh sách từ AuctionManager
      List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();

      out.writeObject(new Message("SUCCESS", "GET_ALL_AUCTIONS", auctions));
      out.flush();
    } catch (IOException e) {
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
      // Quy ước dữ liệu gửi sang: Object[] { Item item, LocalDateTime endTime, imagebytes, filename }
      Object[] data = (Object[]) request.getData();
      Item item = (Item) data[0];
      LocalDateTime endTime = (LocalDateTime) data[1];
      byte[] imageBytes = (byte[]) data[2];
      String fileName = (String) data[3];
      // --- XỬ LÝ LƯU FILE ẢNH VÀO SERVER ---
      if (imageBytes != null && imageBytes.length > 0) {
        // Tạo thư mục "uploads" trên Server nếu chưa có
        java.io.File uploadDir = new java.io.File("server_uploads");
        if (!uploadDir.exists()) {
          uploadDir.mkdir();
        }

        // Tạo đường dẫn lưu file (Thêm timestamp để không bị trùng tên)
        String savePath = "server_uploads/" + item.getItemId() + "_" + fileName;
        java.nio.file.Files.write(java.nio.file.Paths.get(savePath), imageBytes);

        System.out.println("Đã lưu ảnh sản phẩm tại Server: " + savePath);
        // (Tùy chọn: Bạn có thể thêm thuộc tính 'imagePath' vào class Item và set nó ở đây)
        item.setImageUrl(savePath);
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
      List<Auction> sellerAuctions = new java.util.ArrayList<>();

      // 3. Lọc ra những phiên đấu giá có chứa Item do Seller này đăng bán
      for (Auction auction : allAuctions) {
        if (auction.getItem() != null &&
                auction.getItem().getSeller() != null &&
                auction.getItem().getSeller().getUserId().equals(sellerId)) {

          sellerAuctions.add(auction);
        }
      }

      // 4. Đóng gói và gửi trả về cho Client
      out.writeObject(new Message("SUCCESS", "GET_SELLER_AUCTIONS", sellerAuctions));
      out.flush();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        out.writeObject(new Message("FAIL", "GET_SELLER_AUCTIONS", "Lỗi khi tải danh sách kho hàng."));
        out.flush();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }

    private void handlePlaceBid(Message request) {
        try {
            // 1. Tách dữ liệu từ Client gửi lên (Quy ước: {auctionId, User, amount})
            Object[] data = (Object[]) request.getData();
            String auctionId = (String) data[0];
            User currentUser = (User) data[1];
            double bidAmount = (Double) data[2];

            // 2. Ép kiểu User sang Bidder (Đề phòng có ai đó cố tình dùng tài khoản Admin gọi lệnh này)
            if (!(currentUser instanceof Bidder)) {
                out.writeObject(new Message("FAIL", "PLACE_BID", "Chỉ tài khoản Người mua (Bidder) mới có quyền đặt giá!"));
                out.flush();
                return;
            }
            Bidder bidder = (Bidder) currentUser;

            // 3. Lấy phiên đấu giá từ RAM (AuctionManager)
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction == null) {
                out.writeObject(new Message("FAIL", "PLACE_BID", "Phiên đấu giá không tồn tại trên Server!"));
                out.flush();
                return;
            }

            // 4. Tạo đối tượng Bid mới (Truyền đúng 4 tham số theo thiết kế của bạn)
            Bid newBid =
                    new Bid(bidder, auction, bidAmount, LocalDateTime.now());

            // 5. KIỂM TRA BẢO MẬT BẰNG HÀM CỦA BẠN
            try {
                // Hàm này sẽ ném Exception nếu có lỗi (Ví dụ: InvalidBidException)
                newBid.validate();
            } catch (Exception validationException) {
                // Bắt thông báo lỗi từ Exception và gửi thẳng về cho Client hiển thị
                out.writeObject(new Message("FAIL", "PLACE_BID", validationException.getMessage()));
                out.flush();
                return;
            }



            // 6. LƯU XUỐNG DATABASE
            BidDAO bidDAO = new BidDAO();
            boolean isBidSaved = bidDAO.insertBid(newBid); // Gọi đúng 1 tham số

            if (!isBidSaved) {
                out.writeObject(new Message("FAIL", "PLACE_BID", "Lỗi Database khi lưu lịch sử đặt giá."));
                out.flush();
                return;
            }

            // Cập nhật giá hiện tại mới nhất vào bảng Auctions
            auction.setCurrentPrice(bidAmount);
            if(auction.getStatus() == AuctionStatus.OPEN) {
              auction.setStatus(AuctionStatus.RUNNING);
            }
            AuctionDAO auctionDAO = new AuctionDAO();
            boolean isAuctionUpdated = auctionDAO.updateAuctionState(auction); // Lưu lại mức giá mới xuống SQLite



            // 7. CẬP NHẬT ĐỒNG BỘ TRÊN RAM
            auction.getBids().add(newBid);

            // 8. Báo thành công
            out.writeObject(new Message("SUCCESS", "PLACE_BID", newBid));
            out.flush();

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
          out.writeObject(new Message(
                  "FAIL",
                  "UPDATE_PASSWORD",
                  "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (@#$%^&+=!)"
          ));
          out.flush();
          return;
        }

        // 2. Xác thực lại với Database (Bảo mật lớp 2)

        User user = userDAO.getUserById(userId);
        if (user == null) {
          out.writeObject(new Message("FAIL", "UPDATE_PASSWORD", "Tài khoản không tồn tại trên hệ thống!"));
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
          out.writeObject(new Message("FAIL", "UPDATE_PASSWORD", "Lỗi hệ thống khi cập nhật mật khẩu."));
        }
        out.flush();

      } catch (Exception e) {
        e.printStackTrace();
        try {
          out.writeObject(new Message("ERROR", "UPDATE_PASSWORD", "Định dạng dữ liệu gửi lên không hợp lệ."));
          out.flush();
        } catch (IOException ioException) {
          ioException.printStackTrace();
        }
      }
    }
}
