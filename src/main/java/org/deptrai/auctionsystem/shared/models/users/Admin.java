package org.deptrai.auctionsystem.shared.models.users;

public class Admin extends User {
  private int adminLevel;

  public Admin(String userId, String username, String password, String email, int adminLevel) {
    super(userId, username, password, email);
    this.adminLevel = adminLevel;
  }

  // region Getter and Setter
  public int getAdminLevel() {
    return adminLevel;
  }

  public void setAdminLevel(int adminLevel) {
    this.adminLevel = adminLevel;
  }
  // endregion
}
