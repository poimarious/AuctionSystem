package org.deptrai.auctionsystem.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Parent;
import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea; // Đây là cái ô trống CENTER chúng ta tạo lúc đầu

    // Hàm này để đổi nội dung ở giữa màn hình mà không làm mất Menu bên trái
    @FXML
    private void showBiddingFloor() {
        loadView("bidding-detail.fxml");
    }

    private void loadView(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().removeAll();
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
