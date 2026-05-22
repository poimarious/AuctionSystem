package org.deptrai.auctionsystem.client.utils;

import java.io.IOException;
import java.util.Stack;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SceneManager {

  private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

  private static SceneManager instance;
  private final Stack<String[]> historyStack = new Stack<>();
  private Stage primaryStage;
  private String currentFxml;
  private String currentTitle;

  private SceneManager() {}

  public static SceneManager getInstance() {
    if (instance == null) {
      instance = new SceneManager();
    }
    return instance;
  }

  public void setPrimaryStage(Stage stage) {
    this.primaryStage = stage;
  }

  public void switchScene(String fxmlPath, String title) {
    if (currentFxml != null && currentTitle != null) {
      historyStack.push(new String[] {currentFxml, currentTitle});
    }
    loadScene(fxmlPath, title);
  }

  // Phương thức chuyên biệt để về Home và xóa lịch sử
  public void navigateToHome() {
    historyStack.clear();
    loadScene("/org/deptrai/auctionsystem/client/views/home-view.fxml", "Trang Chủ Auction.UET");
  }

  public void goBack() {
    if (!historyStack.isEmpty()) {
      String[] previousScene = historyStack.pop();
      loadScene(previousScene[0], previousScene[1]);
    }
  }

  public void clearHistory() {
    historyStack.clear();
    currentFxml = null;
    currentTitle = null;
  }

  private void loadScene(String fxmlPath, String title) {
    if (primaryStage == null) return;

    try {
      // 1. Lưu lại kích thước và trạng thái của cửa sổ hiện tại
      double currentWidth = primaryStage.getWidth();
      double currentHeight = primaryStage.getHeight();
      boolean isMaximized = primaryStage.isMaximized();

      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent root = loader.load();

      Scene scene;
      // 2. Kế thừa kích thước cũ để cửa sổ không bị giật/co rúm khi load trang
      if (!Double.isNaN(currentWidth) && currentWidth > 100) {
        scene = new Scene(root, currentWidth, currentHeight);
      } else {
        scene = new Scene(root);
      }

      primaryStage.setTitle(title);
      primaryStage.setScene(scene);

      // 3. Khôi phục lại trạng thái phóng to toàn màn hình (nếu có)
      primaryStage.setMaximized(isMaximized);
      primaryStage.show();

      this.currentFxml = fxmlPath;
      this.currentTitle = title;
    } catch (IOException e) {
      logger.error("Lỗi chuyển trang: ", e);
    }
  }
}
