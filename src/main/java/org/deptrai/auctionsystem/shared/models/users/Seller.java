package org.deptrai.auctionsystem.shared.models.users;

import java.util.ArrayList;
import java.util.List;
import org.deptrai.auctionsystem.shared.models.items.Item;

public class Seller extends User {
  private List<Item> listedItems;

  public Seller(String username, String password, String email) {
    super(username, password, email);
    listedItems = new ArrayList<>();
  }

  public Seller(String userId, String username, String password, String email) {
    super(userId, username, password, email);
    listedItems = new ArrayList<>();
  }

  public void addItem(Item item) {
    listedItems.add(item);
  }

  public void removeItem(String id) {
    for (Item item : listedItems) {
      if (item.getItemId().equals(id)) {
        listedItems.remove(item);
        break;
      }
    }
  }

  // region Getter & Setter
  public List<Item> getListedItems() {
    return listedItems;
  }

  public void setListedItems(List<Item> listedItems) {
    this.listedItems = listedItems;
  }
  // endregion
}
