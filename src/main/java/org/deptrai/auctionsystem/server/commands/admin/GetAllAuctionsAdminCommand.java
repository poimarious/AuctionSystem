// GetAllAuctionsAdminCommand.java
package org.deptrai.auctionsystem.server.commands.admin;
import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.dao.UserDAO;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.network.Message;
import java.io.ObjectOutputStream;

public class GetAllAuctionsAdminCommand implements Command {
  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    clientHandler.sendMessage(new Message("SUCCESS", "GET_ALL_AUCTIONS_ADMIN", AuctionManager.getInstance().getAllAuctions()));
  }
}