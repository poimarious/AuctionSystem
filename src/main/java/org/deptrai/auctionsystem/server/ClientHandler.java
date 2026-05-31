package org.deptrai.auctionsystem.server;

import org.deptrai.auctionsystem.server.commands.*;
import org.deptrai.auctionsystem.server.commands.admin.BanUserCommand;
import org.deptrai.auctionsystem.server.commands.admin.GetAllAuctionsAdminCommand;
import org.deptrai.auctionsystem.server.commands.admin.GetAllUsersCommand;
import org.deptrai.auctionsystem.server.commands.admin.UnbanUserCommand;
import org.deptrai.auctionsystem.server.commands.auction.CloseAuctionCommand;
import org.deptrai.auctionsystem.server.commands.auction.CreateAuctionCommand;
import org.deptrai.auctionsystem.server.commands.auction.DeleteAuctionCommand;
import org.deptrai.auctionsystem.server.commands.auction.FinishAuctionCommand;
import org.deptrai.auctionsystem.server.commands.auction.GetAllAuctionsCommand;
import org.deptrai.auctionsystem.server.commands.auction.GetAuctionByIdCommand;
import org.deptrai.auctionsystem.server.commands.auction.GetSellerAuctionsCommand;
import org.deptrai.auctionsystem.server.commands.auth.LoginCommand;
import org.deptrai.auctionsystem.server.commands.auth.LogoutCommand;
import org.deptrai.auctionsystem.server.commands.auth.RegisterCommand;
import org.deptrai.auctionsystem.server.commands.auth.UpdatePasswordCommand;
import org.deptrai.auctionsystem.server.commands.bid.GetBidsHistoryCommand;
import org.deptrai.auctionsystem.server.commands.bid.PlaceBidCommand;
import org.deptrai.auctionsystem.server.commands.bid.StartAutoBidCommand;
import org.deptrai.auctionsystem.server.commands.bid.StopAutoBidCommand;
import org.deptrai.auctionsystem.server.commands.payment.ChangeBalanceCommand;
import org.deptrai.auctionsystem.server.commands.payment.CheckoutCommand;
import org.deptrai.auctionsystem.server.commands.system.GetImageCommand;
import org.deptrai.auctionsystem.server.commands.system.GetNotificationsCommand;
import org.deptrai.auctionsystem.shared.models.users.User;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
  private final Socket socket;
  private ObjectOutputStream out;
  private User authenticatedUser;

  // BỘ ĐỊNH TUYẾN LỆNH (ROUTER)
  private final Map<String, Command> commandMap = new HashMap<>();

  public ClientHandler(Socket socket) {
    this.socket = socket;

    // Đăng ký các chức năng vào hệ thống
    commandMap.put("LOGIN", new LoginCommand());
    commandMap.put("REGISTER", new RegisterCommand());
    commandMap.put("LOGOUT", new LogoutCommand());
    commandMap.put("GET_ALL_AUCTIONS", new GetAllAuctionsCommand());
    commandMap.put("GET_AUCTION_BY_ID", new GetAuctionByIdCommand());
    commandMap.put("GET_SELLER_AUCTIONS", new GetSellerAuctionsCommand());
    commandMap.put("PLACE_BID", new PlaceBidCommand());
    commandMap.put("CHANGE_BALANCE", new ChangeBalanceCommand());
    commandMap.put("CHECKOUT", new CheckoutCommand());
    commandMap.put("FINISH_AUCTION", new FinishAuctionCommand());
    commandMap.put("CREATE_AUCTION", new CreateAuctionCommand());
    commandMap.put("DELETE_AUCTION", new DeleteAuctionCommand());
    commandMap.put("CLOSE_AUCTION", new CloseAuctionCommand());
    commandMap.put("GET_IMAGE", new GetImageCommand());
    commandMap.put("UPDATE_PASSWORD", new UpdatePasswordCommand());
    commandMap.put("GET_BIDS_HISTORY", new GetBidsHistoryCommand());
    commandMap.put("GET_NOTIFICATIONS", new GetNotificationsCommand());
    commandMap.put("GET_ALL_USERS", new GetAllUsersCommand());
    commandMap.put("GET_ALL_AUCTIONS_ADMIN", new GetAllAuctionsAdminCommand());
    commandMap.put("BAN_USER", new BanUserCommand());
    commandMap.put("UNBAN_USER", new UnbanUserCommand());
    commandMap.put("START_AUTOBID", new StartAutoBidCommand());
    commandMap.put("STOP_AUTOBID", new StopAutoBidCommand());
  }

  public Socket getSocket() { return socket; }
  public User getAuthenticatedUser() { return authenticatedUser; }
  public void setAuthenticatedUser(User authenticatedUser) { this.authenticatedUser = authenticatedUser; }

  @Override
  public void run() {
    try {
      out = new ObjectOutputStream(socket.getOutputStream());
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

      Message request;
      while ((request = (Message) in.readObject()) != null) {
        Command command = commandMap.get(request.getCommand());

        if (command != null) {
          command.execute(this, request, out);
        } else {
          out.writeObject(new Message("FAIL", "COMMAND", "Lệnh không hợp lệ hoặc chưa được Server hỗ trợ!"));
          out.flush();
        }
      }
    } catch (Exception e) {
      String errorType = e.getClass().getSimpleName();
      String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Mất kết nối đột ngột";

      // Lấy thông tin ai vừa rớt mạng (Nếu đã đăng nhập thì lấy Username, chưa thì lấy IP)
      String clientInfo = (authenticatedUser != null) ?
          authenticatedUser.getUsername() :
          socket.getInetAddress().toString();

      // Phân loại: Nếu rớt mạng bình thường thì in INFO, nếu lỗi lạ thì in ERROR
      if (e instanceof java.io.EOFException || e instanceof java.net.SocketException) {
        logger.info("Client [{}] đã ngắt kết nối. ({})", clientInfo, errorType);
      } else {
        logger.error("Client [{}] văng lỗi mạng: {} - {}", clientInfo, errorType, errorMsg);
      }
    } finally {
      try {
        ServerMain.activeClients.remove(this);
        socket.close();
      } catch (IOException e) {
        logger.error(e.getMessage());
      }
    }
  }

  public void sendMessage(Message msg) {
    try {
      synchronized (out) {
        out.reset();
        out.writeObject(msg);
        out.flush();
      }
    } catch (IOException e) {
      ServerMain.activeClients.remove(this);

      String clientInfo = (authenticatedUser != null) ?
          authenticatedUser.getUsername() :
          socket.getInetAddress().toString();
      String errorType = e.getClass().getSimpleName();

      logger.warn("Client [{}] rớt mạng khi đang nhận dữ liệu ({}). Đã xóa khỏi danh sách.", clientInfo, errorType);
    }
  }
}