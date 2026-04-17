module org.deptrai.auctionsystem {
    // Yêu cầu nạp thư viện JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Cho phép JavaFX truy cập vào package chứa giao diện để đọc file FXML
    opens org.deptrai.auctionsystem.views to javafx.fxml;

    // Xuất các package để JavaFX có thể chạy được
    exports org.deptrai.auctionsystem.views;
    exports org.deptrai.auctionsystem;
}