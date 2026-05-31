# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

## Một ứng dụng Đấu giá trực tuyến thời gian thực *chạy Local* và hoạt động theo mô hình *Client - Server*

### Phạm vi hệ thống
* Hỗ trợ 3 nhóm người dùng riêng biệt: **Quản trị viên (Admin)** duy trì kỷ luật sàn, **Người bán (Seller)** tạo các phiên đấu giá với thời hạn cụ thể, và **Người mua (Bidder)** tham gia cạnh tranh giá.
* Triển khai theo mô hình Client - Server. Hệ thống chạy Local và có khả năng chịu tải đa luồng.
* Hệ thống tài chính chỉ là mô phỏng nội bộ, không tích hợp các cổng thanh toán thực tế.

### Công nghệ sử dụng
* **Ngôn ngữ cốt lõi:** Java (JDK 25).
* **Giao diện người dùng (GUI):** JavaFX 25.
* **Giao tiếp mạng:** Socket TCP/IP
* **Cơ sở dữ liệu:** SQLite.
* **Quản lý dự án & Đóng gói:** Apache Maven.
* **CI/CD:** GitHub Actions.
* **Thư viện khác:** SLF4J & Logback (Ghi log hệ thống), JUnit 5 & JaCoCo (Test & Coverage).

### Môi trường chạy & Yêu cầu cài đặt
* **Hệ điều hành:** Ứng dụng được đóng gói và tối ưu hóa riêng cho môi trường **Windows** (Windows 10/11).
* **Yêu cầu cài đặt:** Người dùng cài đặt môi trường Java (JRE/JDK 25) trên máy. Hệ thống không yêu cầu cài đặt thêm bất kỳ phần mềm hay cấu hình server Database third-party nào khác.

### Cấu trúc thư mục chính
Hệ thống được xây dựng trên kiến trúc **Client - Server**, áp dụng mô hình **MVC** để tổ chức mã nguồn và kết hợp giao diện trực quan bằng **JavaFX**.

```text
AuctionSystem/
├── .github/workflows/  # File cấu hình luồng chạy CI/CD cho GitHub Actions
├── docs/               # Chứa các tài liệu dự án, sơ đồ lớp.
├── logs/               # Thư mục chứa các file log hệ thống sinh ra khi chạy Server/Client
├── server_uploads/     # Nơi lưu trữ các file ảnh sản phẩm do người dùng tải lên Server (chỉ Server có)
├── src/
│   ├── main/java/org/deptrai/auctionsystem/
│   │   ├── client/     # Chứa logic Client (Các Controllers, Socket Listener, Main...)
│   │   ├── server/     # Chứa logic Server (Xử lý đa luồng, DAO, Client Handlers, Main...)
│   │   └── shared/     # Các Model và file Message dùng chung giữa Client và Server
│   └── main/resources/ # Tài nguyên tĩnh: Các file thiết kế giao diện .fxml, hình ảnh, CSS...
│   └── test/java/      # Kịch bản kiểm thử tự động (Unit Test, Stress Test đa luồng...)
├── target/             # Thư mục sinh ra sau khi build (các file .jar, báo cáo Jacoco,...)
├── auctionsystem.db    # File cơ sở dữ liệu SQLite của toàn bộ hệ thống
├── pom.xml             # File cấu hình Maven quản lý các thư viện được sử dụng
└── mvnw / mvnw.cmd     # Maven Wrapper: Hỗ trợ chạy build trên mọi máy không cần cài sẵn Maven
```

### Vị trí các file .jar
**Tải trực tiếp từ GitHub Actions:** Hệ thống CI/CD đã tự động build sẵn file mỗi khi có code mới.
1. Truy cập vào tab **Actions** trên giao diện GitHub của kho chứa.
2. Bấm vào lần chạy (Workflow run) thành công gần nhất (dấu tick xanh).
3. Cuộn xuống phần **Artifacts** ở dưới cùng và tải file nén `AuctionSystem-FatJARs.zip`.
4. Giải nén ra sẽ nhận được 2 file `.jar` sẵn sàng để chạy.

**Tự Build bằng Maven trên máy cục bộ:** Nếu bạn muốn tự biên dịch từ mã nguồn gốc:
1. Mở Terminal/CMD tại thư mục gốc của dự án.
2. Gõ lệnh: `mvn clean package` (hoặc `.\mvnw clean package`).
3. Sau khi chạy xong, các file thực thi sẽ xuất hiện tại thư mục `target/`:
   * `target/AuctionSystem-1.0-SNAPSHOT-client.jar`
   * `target/AuctionSystem-1.0-SNAPSHOT-server.jar`

### Hướng dẫn chạy chương trình

> **Lưu ý:**
> Máy chủ (Server) được cấu hình mặc định để hoạt động trên **cổng (port) 5000**. Trước khi khởi chạy, hãy đảm bảo cổng 5000 trên máy tính đang trống và không bị chặn bởi tường lửa (Firewall) hay các phần mềm diệt virus.

> **Cách đổi sang cổng khác (Tùy chọn):** Nếu cổng 5000 bị trùng với ứng dụng khác trên máy thì có thể tự đổi sang cổng mới bằng cách cập nhật 2 vị trí sau trong mã nguồn và chạy lệnh build lại:
> 1. **Bên Server:** Mở file `ServerMain.java`, tìm dòng `new ServerSocket(5000)` và sửa số.
> 2. **Bên Client:** Mở file `ClientApplication.java`, tìm dòng `SocketClient.connect("localhost", 5000)` và đổi tham số thứ hai cho khớp với Server.

**Bước 1:** Khởi động Server *(khởi động TRƯỚC Client)*
* Mở Command Prompt (CMD) hoặc Terminal tại thư mục chứa file .jar.
* Gõ lệnh khởi động Server:
```bash
java -jar AuctionSystem-1.0-SNAPSHOT-server.jar
```
* Giữ nguyên cửa sổ CMD này. Hệ thống sẽ in ra log báo hiệu cơ sở dữ liệu đã kết nối và máy chủ đang lắng nghe các luồng Socket.

**Bước 2:** Khởi động Client *(khởi động SAU Server)*
* Mở một cửa sổ Command Prompt (CMD) hoặc Terminal khác tại thư mục chứa file .jar.
* Gõ lệnh khởi động giao diện Client:
```bash
java -jar AuctionSystem-1.0-SNAPSHOT-client.jar
```
* Giao diện Đăng nhập/Đăng ký của hệ thống sẽ xuất hiện.

**Bước 3:** Khởi động nhiều Client (Để test tính năng đấu giá nhiều người)
* Lặp lại **Bước 2** bao nhiêu lần tùy thích. 
* Mỗi lần mở thêm một cửa sổ CMD và chạy lệnh Client, một màn hình ứng dụng mới sẽ hiện ra, hoạt động độc lập như một máy tính khác đang truy cập vào hệ thống.

***Bước 4***: Nếu chạy trên nhiều máy tính khác nhau (Mạng LAN / Radmin VPN)
* Mặc định, Client đang được cấu hình kết nối tới Server qua địa chỉ `localhost` (chạy chung trên 1 máy). Để test hệ thống trên nhiều máy khác nhau có thể mô phỏng mạng LAN:
1. Cài đặt mạng LAN ảo (khuyên dùng **Radmin VPN**) trên tất cả các máy tính tham gia.
2. Tại máy tính sẽ chạy Server, copy địa chỉ IP ảo (Ví dụ: `26.x.x.x`).
3. Trong mã nguồn của Client, vào file `ClientApplication.java`. Tìm lệnh:
```
SocketClient.connect("localhost", 5000);
```
* Đổi địa chỉ `localhost` thành địa chỉ IP `26.x.x.x` của Server.
4. Chạy lại lệnh Build để tạo ra file `client.jar` mới và phân phối cho các máy khác.
* Tất cả các máy tính cần phải join chung vào một Network trên Radmin VPN để có thể nhìn thấy nhau.

### Dữ liệu khởi tạo sẵn
Mặc định các tài khoản Admin đang không cho đăng ký mà phải cấp thủ công trong CSDL. Để thuận tiện cho việc kiểm thử tính năng quản trị mà không cần can thiệp vào CSDL, hệ thống (`auctionsystem.db`) đã được nhóm thiết lập sẵn các tài khoản quản trị cấp cao.

| Tên đăng nhập | Mật khẩu     | Quyền hạn                                                                                           |
|:--------------|:-------------|:----------------------------------------------------------------------------------------------------|
| `admin1`      | `Admin1@123` | **Admin cấp 1:** Quyền giám sát và xóa các phiên đấu giá                                            |
| `admin2`      | `Admin2@123` | **Admin cấp 2:** Quyền giám sát và xóa các phiên đấu giá, Quyền ban (cấm) tài khoản người dùng khác |

> **Lưu ý:** Đối với tài khoản Người mua (Bidder) và Người bán (Seller), có thể sử dụng tính năng **Đăng ký** trực tiếp trên màn hình khởi động của Client để tự tạo tài khoản mới.

## Danh sách chức năng đã hoàn thành

**1. Hệ thống Chung (Khách & Người dùng)**
* **Giao diện Trang chủ (Home View):** Hiển thị danh sách các sản phẩm đang lên sàn với thiết kế Card. Cập nhật giá real-time trực tiếp.
* **Tìm kiếm & Lọc (Search & Filter):** Hỗ trợ thanh tìm kiếm sản phẩm và lọc theo danh mục ngay trên thanh điều hướng.
* **Quản lý Tài khoản:** Form Đăng ký/Đăng nhập an toàn, quản lý hồ sơ cá nhân (Profile) và thay đổi mật khẩu.
* **Hệ thống Chuông Thông báo (Notification Bell):** Nút chuông góc phải màn hình cập nhật real-time các sự kiện (bị vượt giá cược, phiên đấu giá kết thúc, thanh toán thành công).

**2. Người mua (Bidder)**
* **Phòng Đấu giá Trực tiếp (Bidding Detail):** Giao diện chuyên biệt để theo dõi một phiên đấu giá. Cập nhật giá real-time trực tiếp trên màn hình của mọi Client ngay khi có người cược. Tích hợp hiển thị đồ thị giúp phiên đấu giá dễ theo dõi hơn.
* **Đặt giá Nhanh/Thủ công & Auto-Bid:** Cho phép đặt giá nhanh qua ấn nút hoặc nhập giá cược cụ thể. Tích hợp chức năng bật/tắt chế độ Đấu giá tự động (Auto-bid). Hệ thống tự động giam tiền để đảm bảo khả năng thanh toán.
* **Lịch sử Đặt giá (Bid History):** Tra cứu lại toàn bộ các lệnh đặt cược của bản thân.
* **Xử lý Đơn hàng (Checkout/Transaction):** Phân hệ "Cần thanh toán" giúp người mua xem lại các phiên đã thắng và thực hiện thanh toán an toàn cho người bán, quản lý lịch sử giao dịch.
* **Ví điện tử ảo:** Hiển thị số dư trực tiếp trên Header, hỗ trợ giả lập nạp tiền vào ví.

**3. Người bán (Seller)**
* **Kênh Người Bán Độc lập:** Giao diện Dashboard (Seller View) dành riêng cho tài khoản có quyền bán hàng.
* **Đăng tải Sản phẩm mới (Add Product):** Form nhập thông tin chi tiết, tải ảnh trực tiếp từ máy tính lên Server và hẹn giờ kết thúc phiên đấu giá.
* **Quản lý Kho hàng (Inventory):** Xem và quản lý tình trạng các sản phẩm của mình (Đang chạy, Đã kết thúc, Đã thanh toán).
* **Thống kê Doanh thu:** Xem biến động số dư và dòng tiền nhận được từ các phiên đấu giá thành công được mô hình hóa bằng đồ thị.

**4. Quản trị viên (Admin)**
* Bảng điều khiển riêng biệt chỉ hiển thị khi tài khoản có cấp độ Admin.
* Quản lý danh sách toàn bộ người dùng, can thiệp khóa/gỡ khóa (Ban/Unban) các tài khoản vi phạm (Admin level 2)
* Quản lý, kiểm duyệt hoặc xóa các phiên đấu giá không hợp lệ khỏi hệ thống (Admin level 1/2)

### Link báo cáo PDF và Video demo

* Video demo: https://drive.google.com/file/d/1fiYmBQntVEdknnPiybV0k0gmfIYrT6c7/view?usp=sharing
* Báo cáo PDF: https://drive.google.com/file/d/1VuS6x-0JAS1BBiR7eE-1sWjoQn2Hcq5Q/view?usp=sharing