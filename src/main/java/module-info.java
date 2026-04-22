module org.deptrai.auctionsystem {
    // Yêu cầu nạp thư viện JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // Cho phép JavaFX truy cập vào package chứa giao diện để đọc file FXML
    opens org.deptrai.auctionsystem.views to javafx.fxml;

    // Xuất các package để JavaFX có thể chạy được
    exports org.deptrai.auctionsystem.views;
    exports org.deptrai.auctionsystem;
    exports org.deptrai.auctionsystem.controllers;
    opens org.deptrai.auctionsystem.controllers to javafx.fxml;
}