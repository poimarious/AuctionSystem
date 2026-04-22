package org.deptrai.auctionsystem.models.users;
import org.deptrai.auctionsystem.models.items.Item;

import java.util.List;

public class Seller extends User {
    private List<Item> listedItems;

    public Seller(String username, String password, String email) {
        super(username, password, email);
    }

    public Seller(String userId, String username, String password, String email) {
        super(userId, username, password, email);
    }

    public void addItem(Item item){
        listedItems.add(item);
    }

    public void removeItem(String id){
        for (Item item : listedItems){
            if (item.getItemId().equals(id)){
                listedItems.remove(item);
                break;
            }
        }
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

    //region Getter & Setter
    public List<Item> getListedItems(){
        return listedItems;
    }
    public void setListedItems(List<Item> listedItems) {
        this.listedItems = listedItems;
    }
    //endregion
}