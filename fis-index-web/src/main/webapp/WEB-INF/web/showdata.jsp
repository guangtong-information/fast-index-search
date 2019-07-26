<%@page import="com.fis.web.tools.DateTime" %>
<%@page import="java.text.SimpleDateFormat" %>

<%@ page language="java" contentType="text/html; charset=utf-8"
         pageEncoding="utf-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title>框架速度测试</title>
</head>
<body>
<span style="font-size: 15px; font-weight: bold">框架速度测试:</span><br/><br/>

<%SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS"); %>
执行开始时间：<% out.print(sf.format(DateTime.getNowDate()) + "<br/>"); %><br/>

<form action="/index">
    歌名：<input id="songName" name="songName" type="text" value="${songName}"/>
    排序字段(stime、sortnum)：<input id="sort" name="sort" type="text" value="${sort}"/>
    升序降序(A or D)：<input id="dirct" name="dirct" type="text" value="${dirct}"/>
    <input type="submit" value="搜索">
</form>
<br/><br/>
<table border="1">
    <tr>
        <td>编号</td>
        <td>歌名</td>
        <td>发布时间</td>
        <td>自定义排序</td>
    </tr>
    <c:forEach items="${list}" var="aa">
        <tr>
            <td>${aa.idRank}</td>
            <td>${aa.songName}</td>
            <td><fmt:formatDate value="${aa.stime}" type="date" pattern="yyyy-MM-dd HH:mm"/></td>
            <td>${aa.sortNum}</td>
        </tr>
    </c:forEach>
</table>

<span>
    <a href="/index?pg=1&songName=${songName}&sort=${sort}&dirct=${dirct}">首页</a>
    <a href="/index?pg=${pg-1}&songName=${songName}&sort=${sort}&dirct=${dirct}">上一页</a>
    <a href="/index?pg=${pg+1}&songName=${songName}&sort=${sort}&dirct=${dirct}">下一页</a>
    <a href="/index?pg=${page}&songName=${songName}&sort=${sort}&dirct=${dirct}">尾页</a>
</span>
总条数:${total},总页数:${page},当前页:${pg},一页大小:${pgsize}<br/><br/>

执行结束时间：<% out.print(sf.format(DateTime.getNowDate()) + "<br/>"); %><br/>

</body>
</html>