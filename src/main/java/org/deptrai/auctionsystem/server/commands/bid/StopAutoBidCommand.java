package org.deptrai.auctionsystem.server.commands.bid;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.server.managers.AuctionManager;
import org.deptrai.auctionsystem.shared.network.Message;

import java.io.ObjectOutputStream;

public class StopAutoBidCommand implements Command {


  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    Object[] data = (Object[]) request.getData();
    String userId = (String)data[0];
    String auctionId = (String) data[1];

    AuctionManager.getInstance().unregisterAutoBid(userId, auctionId);

    clientHandler.sendMessage(new Message("SUCCESS", "STOP_AUTOBID", "Đã giải phóng tiền giam Auto-Bid"));
  }
}
