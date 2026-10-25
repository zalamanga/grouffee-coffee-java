<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="id_ID"/>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!doctype html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Grouffee Coffee</title>
<link rel="stylesheet" href="${ctx}/assets/app.css">
<script src="${ctx}/assets/app.js" defer>
</script>
</head>
<body>
<a class="skip" href="#main">Lewati navigasi</a>
<header>
<div class="header-inner">
<a class="brand" href="${ctx}/">
<img src="${ctx}/assets/coffee.clipart.png" alt="" width="44" height="44">
<span>Grouffee Coffee</span>
</a>
<button class="menu-toggle" aria-expanded="false" aria-controls="navigation" hidden>Menu</button>
<nav id="navigation" aria-label="Navigasi utama">
<a href="${ctx}/menu">Menu kopi</a>
<a href="${ctx}/about">Tentang demo</a>
<c:choose>
<c:when test="${not empty account}">
<a href="${ctx}/cart">Keranjang</a>
<a href="${ctx}/orders">Pesanan demo</a>
<a href="${ctx}/profile">Akun</a>
<c:if test="${account.admin}">
<a href="${ctx}/admin">Kelola data</a>
</c:if>
<form action="${ctx}/logout" method="post">
<%@ include file="csrf.jspf" %>
<button class="quiet">Keluar</button>
</form>
</c:when>
<c:otherwise>
<a href="${ctx}/login">Masuk</a>
<a href="${ctx}/register">Daftar</a>
</c:otherwise>
</c:choose>
</nav>
</div>
</header>
<main id="main" tabindex="-1">
<c:if test="${not empty error}">
<div class="notice error" role="alert" tabindex="-1">
<c:out value="${error}"/>
</div>
</c:if>
<c:if test="${param.saved == '1'}">
<p class="notice" role="status">Perubahan tersimpan.</p>
</c:if>
<c:choose>
<c:when test="${page == 'home'}">
<section class="hero">
<div>
<h1>Kopi pilihanmu,<br>di meja yang sama.</h1>
<p>Pilih menu, isi jumlah dan nomor meja, lalu simpan pesanan contoh dari akunmu.</p>
<p class="demo-note">Demo lokal Grouffee Coffee. Tidak ada pembayaran atau pengiriman pesanan ke kasir.</p>
<a class="button" href="${ctx}/menu">Lihat menu kopi</a>
</div>
<img src="${ctx}/assets/coffebg.jpeg" alt="Secangkir kopi di atas tumpukan buku">
</section>
<section class="intro">
<h2>Dari menu ke pesanan demo</h2>
<div>
<p>Menu dan harga dibaca dari database. Setiap akun mempunyai keranjang sesi dan riwayat pesanan contohnya sendiri.</p>
<p>Nama Grouffee Coffee, warna hangat, dan gambar kopi dipertahankan dari proyek asal.</p>
<a href="${ctx}/about">Baca batas demo dan data akun</a>
</div>
</section>
</c:when>
<c:when test="${page == 'menu'}">
<h1>Menu kopi &amp; teman duduk</h1>
<p>Menu dan harga berikut adalah data contoh untuk demo lokal.
<c:choose>
<c:when test="${empty account}">Masuk untuk mencoba keranjang.</c:when>
<c:otherwise>Atur jumlah, lalu tambahkan menu ke keranjang.</c:otherwise>
</c:choose>
</p>
<c:if test="${empty menu}">
<p class="notice">Belum ada menu. Admin dapat menambahkan menu melalui Kelola data.</p>
</c:if>
<div class="products">
<c:forEach var="item" items="${menu}">
<article class="product">
<img src="${ctx}/assets/${item.image}" alt="" width="240" height="200">
<div>
<p class="category">
<c:out value="${item.category}"/>
</p>
<h2>
<c:out value="${item.name}"/>
</h2>
<p class="price">Rp<fmt:formatNumber value="${item.price}" minFractionDigits="2" maxFractionDigits="2"/>
</p>
<c:choose>
<c:when test="${not empty account}">
<form action="${ctx}/cart/add" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="id" value="${item.id}">
<label for="qty-${item.id}">Jumlah</label>
<div class="inline">
<input class="quantity" id="qty-${item.id}" name="quantity" type="number" min="1" max="99" value="1" required>
<button>Tambah ke keranjang</button>
</div>
</form>
</c:when>
<c:otherwise>
<a href="${ctx}/login">Masuk untuk menambah</a>
</c:otherwise>
</c:choose>
</div>
</article>
</c:forEach>
</div>
</c:when>
<c:when test="${page == 'login'}">
<section class="form-page">
<h1>Masuk ke Grouffee</h1>
<c:if test="${param.registered == '1'}">
<p class="notice" role="status">Akun tersimpan. Masuk dengan username dan kata sandimu.</p>
</c:if>
<form action="${ctx}/login" method="post">
<%@ include file="csrf.jspf" %>
<label for="username">Username</label>
<input id="username" name="username" autocomplete="username" minlength="4" maxlength="40" required value="<c:out value='${param.username}'/>">
<label for="password">Kata sandi</label>
<input id="password" name="password" type="password" autocomplete="current-password" maxlength="128" required>
<button>Masuk</button>
</form>
<p>
<a href="${ctx}/register">Daftar akun demo</a> · <a href="${ctx}/help">Bantuan akun</a>
</p>
</section>
</c:when>
<c:when test="${page == 'register'}">
<section class="form-page">
<h1>Daftar akun demo</h1>
<p>Gunakan nama dan email contoh. Email tidak diverifikasi atau dikirimi pesan.</p>
<form action="${ctx}/register" method="post">
<%@ include file="csrf.jspf" %>
<label for="name">Nama tampilan</label>
<input id="name" name="name" maxlength="100" autocomplete="name" required value="<c:out value='${param.name}'/>">
<label for="email">Email contoh</label>
<input id="email" name="email" type="email" maxlength="160" autocomplete="email" required value="<c:out value='${param.email}'/>">
<label for="username">Username</label>
<input id="username" name="username" pattern="[A-Za-z0-9_]{4,40}" minlength="4" maxlength="40" autocomplete="username" required value="<c:out value='${param.username}'/>">
<p class="hint">4 sampai 40 huruf, angka, atau garis bawah.</p>
<label for="password">Kata sandi</label>
<input id="password" name="password" type="password" minlength="12" maxlength="128" autocomplete="new-password" required>
<p class="hint">12 sampai 128 karakter.</p>
<label for="confirm">Ulangi kata sandi</label>
<input id="confirm" name="confirm" type="password" minlength="12" maxlength="128" autocomplete="new-password" required>
<button>Simpan akun demo</button>
</form>
</section>
</c:when>
<c:when test="${page == 'cart'}">
<h1>Keranjang akunmu</h1>
<p>Harga dibaca ulang dari database saat pesanan contoh disimpan. Maksimum 20 jenis menu dan 99 item per menu.</p>
<c:choose>
<c:when test="${not hasCart}">
<p class="notice">Keranjang masih kosong. <a href="${ctx}/menu">Pilih menu kopi</a> untuk memulai.</p>
</c:when>
<c:otherwise>
<c:forEach var="item" items="${rows}">
<article class="data-row">
<div>
<h2>
<c:out value="${item.name}"/>
</h2>
<p>${item.quantity} × Rp<fmt:formatNumber value="${item.price}" minFractionDigits="2"/> = Rp<fmt:formatNumber value="${item.subtotal}" minFractionDigits="2"/>
</p>
</div>
<form action="${ctx}/cart/remove" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="id" value="${item.id}">
<button class="quiet">Hapus <c:out value="${item.name}"/>
</button>
</form>
</article>
</c:forEach>
<form action="${ctx}/cart/clear" method="post">
<%@ include file="csrf.jspf" %>
<button class="quiet">Kosongkan keranjang</button>
</form>
<c:if test="${not invalidCart}">
<section class="checkout">
<h2>Total Rp<fmt:formatNumber value="${total}" minFractionDigits="2"/>
</h2>
<p>Pesanan disimpan sebagai simulasi. Tidak ada pembayaran, stok, atau pesanan ke kasir yang diproses.</p>
<form action="${ctx}/checkout" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="checkoutToken" value="<c:out value='${requestScope.checkoutToken}'/>">
<label for="table">Nomor meja contoh</label>
<input id="table" name="table" type="number" min="1" max="999" required value="<c:out value='${param.table}'/>">
<button>Simpan pesanan demo</button>
</form>
</section>
</c:if>
</c:otherwise>
</c:choose>
</c:when>
<c:when test="${page == 'orders'}">
<h1>Pesanan demo milikmu</h1>
<p>Menampilkan paling banyak 100 pesanan terbaru akun ini.</p>
<c:if test="${empty orders}">
<p class="notice">Belum ada pesanan demo. <a href="${ctx}/menu">Pilih menu</a> lalu lanjutkan melalui keranjang.</p>
</c:if>
<c:forEach var="order" items="${orders}">
<article class="data-row">
<div>
<h2>Pesanan #${order.id}</h2>
<p>Meja ${order.table_number} · Rp<fmt:formatNumber value="${order.total}" minFractionDigits="2"/> · <c:out value="${order.created_at}"/>
</p>
</div>
<a href="${ctx}/order?id=${order.id}">Lihat rincian #${order.id}</a>
</article>
</c:forEach>
</c:when>
<c:when test="${page == 'order'}">
<h1>Pesanan demo #${order.id}</h1>
<p class="notice">Simulasi tersimpan untuk meja ${order.table_number}. Tidak ada pesanan ke kasir atau pembayaran.</p>
<c:forEach var="line" items="${order.items}">
<article class="data-row">
<div>
<h2>
<c:out value="${line.name}"/>
</h2>
<p>${line.quantity} × Rp<fmt:formatNumber value="${line.unit_price}" minFractionDigits="2"/>
</p>
</div>
<strong>Rp<fmt:formatNumber value="${line.subtotal}" minFractionDigits="2"/>
</strong>
</article>
</c:forEach>
<h2>Total Rp<fmt:formatNumber value="${order.total}" minFractionDigits="2"/>
</h2>
<a href="${ctx}/orders">Kembali ke pesanan demo</a>
</c:when>
<c:when test="${page == 'profile'}">
<section class="form-page">
<h1>Akun Grouffee</h1>
<dl>
<dt>Nama</dt>
<dd><c:out value="${account.name}"/></dd>
<dt>Username</dt>
<dd><c:out value="${account.username}"/></dd>
<dt>Email</dt>
<dd><c:out value="${account.email}"/></dd>
<dt>Peran</dt>
<dd><c:out value="${account.role}"/></dd>
</dl>
<p>Admin dapat mengubah data akun melalui Kelola data. Gunakan data contoh untuk demo ini.</p>
<h2>Ganti kata sandi</h2>
<form action="${ctx}/password" method="post">
<%@ include file="csrf.jspf" %>
<label for="current">Kata sandi saat ini</label>
<input id="current" name="currentPassword" type="password" maxlength="128" autocomplete="current-password" required>
<label for="password">Kata sandi baru</label>
<input id="password" name="password" type="password" minlength="12" maxlength="128" autocomplete="new-password" required>
<p class="hint">12 sampai 128 karakter.</p>
<button>Ganti kata sandi</button>
</form>
</section>
</c:when>
<c:when test="${page == 'admin'}">
<h1>Kelola data Grouffee</h1>
<p>Menu, harga contoh, dan akun disimpan di database.</p>
<div class="tabs">
<a href="${ctx}/admin">Menu</a>
<a href="${ctx}/admin?tab=accounts">Akun</a>
</div>
<c:choose>
<c:when test="${param.tab == 'accounts'}">
<div class="section-title">
<h2>Akun demo</h2>
<a class="button" href="${ctx}/admin/account-edit">Tambah akun</a>
</div>
<c:forEach var="member" items="${accounts}">
<article class="data-row">
<div>
<h3>
<c:out value="${member.name}"/>
</h3>
<p>
<c:out value="${member.username}"/> · <c:out value="${member.email}"/> · <c:out value="${member.role}"/>
</p>
</div>
<div class="actions">
<a href="${ctx}/admin/account-edit?id=${member.id}">Ubah akun</a>
<a href="${ctx}/admin/delete?kind=account&amp;id=${member.id}">Hapus akun</a>
</div>
</article>
</c:forEach>
</c:when>
<c:otherwise>
<div class="section-title">
<h2>Menu contoh</h2>
<a class="button" href="${ctx}/admin/menu-edit">Tambah menu</a>
</div>
<c:if test="${empty menu}">
<p class="notice">Menu kosong. Tambahkan menu contoh menggunakan tombol Tambah menu.</p>
</c:if>
<c:forEach var="item" items="${menu}">
<article class="data-row">
<div>
<h3>
<c:out value="${item.name}"/>
</h3>
<p>
<c:out value="${item.category}"/> · Rp<fmt:formatNumber value="${item.price}" minFractionDigits="2"/>
</p>
</div>
<div class="actions">
<a href="${ctx}/admin/menu-edit?id=${item.id}">Ubah menu</a>
<a href="${ctx}/admin/delete?kind=menu&amp;id=${item.id}">Hapus menu</a>
</div>
</article>
</c:forEach>
</c:otherwise>
</c:choose>
</c:when>
<c:when test="${page == 'menu-edit'}">
<section class="form-page">
<h1>${empty item ? 'Tambah' : 'Ubah'} menu</h1>
<form action="${ctx}/admin/menu-save" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="id" value="${empty item ? 0 : item.id}">
<label for="name">Nama menu</label>
<input id="name" name="name" maxlength="120" required value="<c:out value='${item.name}'/>">
<label for="category">Kategori</label>
<select id="category" name="category">
<option ${item.category == 'Minuman' ? 'selected' : ''}>Minuman</option>
<option ${item.category == 'Makanan' ? 'selected' : ''}>Makanan</option>
</select>
<label for="price">Harga contoh (rupiah)</label>
<input id="price" name="price" type="number" min="0.01" max="9999999.99" step="0.01" required value="<c:out value='${item.price}'/>">
<label for="image">Gambar sumber</label>
<select id="image" name="image">
<c:forEach var="image" items="${images}">
<option value="${image}" ${item.image == image ? 'selected' : ''}>${image}</option>
</c:forEach>
</select>
<button>Simpan menu</button>
<a href="${ctx}/admin">Batal</a>
</form>
</section>
</c:when>
<c:when test="${page == 'account-edit'}">
<section class="form-page">
<h1>${empty editAccount ? 'Tambah' : 'Ubah'} akun</h1>
<form action="${ctx}/admin/account-save" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="id" value="${empty editAccount ? 0 : editAccount.id}">
<label for="name">Nama</label>
<input id="name" name="name" maxlength="100" required value="<c:out value='${editAccount.name}'/>">
<label for="username">Username</label>
<input id="username" name="username" pattern="[A-Za-z0-9_]{4,40}" maxlength="40" required value="<c:out value='${editAccount.username}'/>">
<label for="email">Email contoh</label>
<input id="email" name="email" type="email" maxlength="160" required value="<c:out value='${editAccount.email}'/>">
<label for="role">Peran</label>
<select id="role" name="role">
<option value="member" ${editAccount.role == 'member' ? 'selected' : ''}>Member</option>
<option value="admin" ${editAccount.role == 'admin' ? 'selected' : ''}>Admin</option>
</select>
<label for="password">${empty editAccount ? 'Kata sandi' : 'Kata sandi baru (opsional)'}</label>
<input id="password" name="password" type="password" minlength="12" maxlength="128" autocomplete="new-password" ${empty editAccount ? 'required' : ''}>
<p class="hint">12 sampai 128 karakter. Untuk akun yang ada, kosongkan agar kata sandi tetap.</p>
<button>Simpan akun</button>
<a href="${ctx}/admin?tab=accounts">Batal</a>
</form>
</section>
</c:when>
<c:when test="${page == 'delete'}">
<section class="form-page">
<h1>Konfirmasi hapus</h1>
<p>Hapus <strong>
<c:out value="${selected.name}"/>
</strong>?</p>
<c:if test="${param.kind == 'account'}">
<p>Semua pesanan demo akun ini juga akan dihapus. Admin terakhir tetap dilindungi.</p>
</c:if>
<form action="${ctx}/admin/delete" method="post">
<%@ include file="csrf.jspf" %>
<input type="hidden" name="kind" value="<c:out value='${param.kind}'/>">
<input type="hidden" name="id" value="<c:out value='${param.id}'/>">
<button>Ya, hapus data</button>
<a href="${ctx}/admin">Batal</a>
</form>
</section>
</c:when>
<c:when test="${page == 'help'}">
<section class="prose">
<h1>Bantuan akun demo</h1>
<p>Daftar menggunakan username, email contoh, dan kata sandi pilihanmu. Email tidak dikirimi kode atau tautan.</p>
<p>Jika masih mengetahui kata sandi, masuk lalu buka Akun untuk menggantinya. Jika lupa, buat akun demo baru atau minta pengelola demo mengubah kata sandi melalui Kelola data.</p>
<a href="${ctx}/login">Kembali ke Masuk</a>
</section>
</c:when>
<c:when test="${page == 'about'}">
<section class="prose">
<h1>Tentang demo &amp; data</h1>
<p>Grouffee Coffee melanjutkan proyek latihan Servlet/JSP tahun 2023. Nama, gambar kopi, dan arah warna berasal dari sumber tersebut.</p>
<h2>Yang tersimpan</h2>
<p>Database menyimpan nama tampilan, username, email contoh, hash kata sandi, peran akun, menu, dan pesanan demo. Keranjang tersimpan di sesi server dan berakhir ketika keluar atau sesi habis. Admin dapat mengelola akun dan menu; setiap pengguna hanya dapat membaca pesanan demonya sendiri.</p>
<h2>Batas penggunaan</h2>
<p>Gunakan data sintetis. Ini bukan kedai yang menerima transaksi. Tidak ada pembayaran, QRIS, verifikasi email/OTP, pengiriman email, pemulihan otomatis, pengelolaan stok, atau pesanan ke kasir.</p>
<p>Menghapus akun melalui admin menghapus pesanan contoh yang terkait. Pengelola instalasi bertanggung jawab atas backup dan penghapusan database lokal.</p>
<h2>Atribusi sumber</h2>
<p>Footer sumber menyebut Kelompok 8, sementara penjelasan nama menyebut Group Four Coffee. Atribusi keduanya dicatat tanpa mengklaim kepemilikan seluruh kode dan gambar.</p>
</section>
</c:when>
<c:otherwise>
<section class="prose">
<h1>Halaman belum dapat ditampilkan</h1>
<p>Coba muat ulang halaman setelah layanan siap.</p>
<a class="button" href="${ctx}/menu">Coba buka menu</a>
<a href="${ctx}/">Kembali ke beranda</a>
</section>
</c:otherwise>
</c:choose>
</main>
<footer>
<div>
<strong>Grouffee Coffee</strong>
<p>Demo lokal berdasarkan sumber Kelompok 8, 2023.</p>
</div>
<a href="${ctx}/about">Data &amp; batas demo</a>
</footer>
<p class="submit-status" role="status" aria-live="polite" hidden>Menyimpan perubahan…</p>
</body>
</html>
