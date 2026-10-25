# Alur kode Grouffee Coffee

Tomcat memuat driver JDBC melalui `DriverLifecycle`, lalu `AppServlet.init` menyiapkan admin demo bila belum ada. Schema/seed MySQL dibuat Compose pada volume baru; membuka halaman tidak mereset data.

```mermaid
flowchart TD
  A["Request browser"] --> B["SecurityFilter: UTF-8, headers, sesi, CSRF POST"]
  B -->|Logout POST valid| L["AppServlet: invalidate sesi tanpa perlu database"]
  B --> C["Accounts.find: muat akun dan peran terbaru"]
  C --> D["AppServlet.guard: akun dan akses admin"]
  D -->|GET| E["Repository: menu, akun, atau pesanan milik sesi"]
  E --> F["JSP: escape teks dan tampilkan keadaan"]
  D -->|POST| G["Validasi formulir dan jalankan operasi"]
  G --> H["Repository JDBC dan transaksi yang diperlukan"]
  H --> I["Commit sebelum redirect sukses"]
  I --> A
  G -->|Input tidak valid| F
  H -->|Database gagal| J["503 dan jalur coba kembali"]
```

## Keranjang sampai pesanan demo

`Cart` menyimpan jumlah item dalam sesi. Tambah/hapus/kosongkan keranjang dan penghapusan token checkout berada dalam kunci `HttpSession` yang sama dengan checkout. Snapshot serta token untuk halaman keranjang ditangkap bersama; JSP memakai token request tersebut.

```mermaid
flowchart TD
  A["POST checkout: akun, CSRF, meja, token"] --> B["Kunci sesi dan cocokkan checkoutToken"]
  B --> C["CoffeeRepository.checkout: transaksi"]
  C --> D["Cari pesanan akun dengan token yang sama"]
  D -->|Sudah ada| E["Gunakan ID pesanan tersebut"]
  D -->|Belum| F["Baca harga menu dari DB dengan FOR SHARE"]
  F --> G["INSERT header dan snapshot rincian pesanan"]
  G --> H["Commit"]
  H --> E
  E --> I["Kosongkan cart/token lalu redirect ke rincian milik akun"]
```

Harga/total dari browser tidak menjadi dasar transaksi. Pesanan dibaca dengan pasangan ID pesanan dan ID akun sesi. Admin terakhir dilindungi dengan penguncian baris akun dalam transaksi saat penghapusan atau perubahan peran.

| Bagian | Sumber |
| --- | --- |
| Registrasi/cleanup driver | [DriverLifecycle.java](../src/main/java/id/demo/DriverLifecycle.java), [Db.java](../src/main/java/id/demo/Db.java) |
| Sesi, CSRF dan pemuatan peran | [SecurityFilter.java](../src/main/java/id/demo/SecurityFilter.java) |
| Route, guard dan kunci sesi | [AppServlet.java](../src/main/java/id/demo/AppServlet.java) |
| Akun dan password | [Accounts.java](../src/main/java/id/demo/Accounts.java), [Passwords.java](../src/main/java/id/demo/Passwords.java) |
| Batas keranjang dan akses data | [Cart.java](../src/main/java/id/demo/Cart.java), [CoffeeRepository.java](../src/main/java/id/demo/CoffeeRepository.java) |
| Tampilan dan data | [app.jsp](../src/main/webapp/WEB-INF/views/app.jsp), [schema.sql](../database/schema.sql) |
| Regresi domain dan konkurensi sesi | [DomainTest.java](../src/test/java/id/demo/DomainTest.java) |

Checkout menyimpan simulasi, tanpa pembayaran atau pengiriman ke kasir. Setup, screenshot, dan batas operasional ada di [README](../README.md).
