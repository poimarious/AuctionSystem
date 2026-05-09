package org.deptrai.auctionsystem.shared.network;

import java.io.Serializable;

public class Message implements Serializable {
  private String command; // "LOGIN", "REGISTER", "GET_AUCTIONS"
  private Object data;    // Chứa bất kỳ thứ gì: String[], User, List<Auction>...
  private String status;  // "SUCCESS", "FAIL"

  // Constructor gửi yêu cầu (Client -> Server)
  public Message(String command, Object data) {
    this.command = command;
    this.data = data;
  }

  // Constructor trả kết quả (Server -> Client)
  public Message(String status, String command, Object data) {
    this.status = status;
    this.command = command;
    this.data = data;
  }

  // Getters
  public String getCommand() { return command; }
  public Object getData() { return data; }
  public String getStatus() { return status; }
}