package org.deptrai.auctionsystem.shared.models.users;

import java.io.Serializable;

public abstract class User implements Serializable {
  private String userId; // Database automatically assigns this.
  private String username;
  private String password;
  private String email;
  private double balance = 0.0;
  private boolean isBanned = false;
  private String banReason = "";

  public User(String username, String password, String email) {
    this.username = username;
    this.password = password;
    this.email = email;
  } // For creating a new object

  public User(String userId, String username, String password, String email) {
    this.userId = userId;
    this.username = username;
    this.password = password;
    this.email = email;
  } // For loading an object

  public abstract boolean login();

  public abstract void logout();

  public abstract String getInfo();

  // region Getters
  public String getUserId() {
    return userId;
  }

  // region Setters
  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public boolean isBanned() {
    return isBanned;
  }

  public String getBanReason() {
    return banReason;
  }

  // endregion

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public void setBanned(boolean banned) {
    this.isBanned = banned;
  }

  public void setBanReason(String banReason) {
    this.banReason = banReason;
  }

  // endregion
}
