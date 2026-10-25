<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${empty error}"><c:set var="error" value="Permintaan tidak dapat diproses. Muat ulang halaman dan periksa akses akunmu." scope="request"/></c:if>
<c:set var="page" value="error" scope="request"/>
<%@ include file="app.jsp" %>
