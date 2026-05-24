package org.deptrai.auctionsystem.server.commands.system;

import org.deptrai.auctionsystem.server.ClientHandler;
import org.deptrai.auctionsystem.server.commands.Command;
import org.deptrai.auctionsystem.shared.network.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.ObjectOutputStream;
import java.nio.file.Files;

public class GetImageCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(GetImageCommand.class);

  @Override
  public void execute(ClientHandler clientHandler, Message request, ObjectOutputStream out) throws Exception {
    try {
      String imagePath = (String) request.getData();
      File file = new File(imagePath);

      if (file.exists()) {
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        clientHandler.sendMessage(new Message("SUCCESS", "GET_IMAGE", imageBytes));
      } else {
        clientHandler.sendMessage(new Message("FAIL", "GET_IMAGE", null));
      }
    } catch (Exception e) {
      logger.warn("Không thể tải ảnh cho Client, Path có thể không hợp lệ.");
      clientHandler.sendMessage(new Message("ERROR", "GET_IMAGE", "Lỗi hệ thống khi đọc file ảnh."));
    }
  }
}