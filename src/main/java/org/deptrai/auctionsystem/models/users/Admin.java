package org.deptrai.auctionsystem.models.users;

import org.deptrai.auctionsystem.models.items.Item;

public class Admin extends User {
    private int adminLevel;

    public Admin(String username, String password, String email) {
        super(username, password, email);
        adminLevel = 0; //Default admin level
    }

    public Admin(String userId, String username, String password, String email, int adminLevel) {
        super(userId, username, password, email);
        this.adminLevel = adminLevel;
    }

    public void closeAuction(String id) {

    }

    public void removeItem(Item item) {

    }

    @Override
    public boolean login() {
        return false;
    }

    @Override
    public void logout() {

    }

    @Override
    public String getInfo() {
        return "";
    }

    //region Getter and Setter
    public int getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(int adminLevel) {
        this.adminLevel = adminLevel;
    }
    //endregion
}
