<%--
 * UniTime 3.1 (University Timetabling Application)
 * Copyright (C) 2008, UniTime LLC
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
--%>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8" errorPage="/error.jsp"%>
<%@ page import="org.unitime.timetable.ApplicationProperties" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
	<TITLE>Session Expired</TITLE>
	<META http-equiv="pragma" content="no-cache">
	<META http-equiv="cache-control" content="no-cache">
	<META http-equiv="expires" content="0">
	<LINK rel="stylesheet" type="text/css" href="styles/timetabling.css">
	<link rel="shortcut icon" href="images/timetabling.ico" />
	<SCRIPT language="javascript" type="text/javascript">
           <!--
           if(parent.frames.length!=0)
	           top.location.href = '<%=request.getContextPath()%>/loginRequired.jsp?message=<%=request.getParameter("message")%>'
           //-->
     </SCRIPT>
     <meta http-equiv="X-UA-Compatible" content="chrome=1">
</HEAD>

<BODY class="bodyMain">

<DIV align="center" class="H1">
	<BR> 
	<IMG src="images/timetabling-nocopy.jpg" alt="Timetabling" title="Timetabling Log In">
	<BR>
	<BR>
	<% if (request.getParameter("message")!=null && !"null".equals(request.getParameter("message"))) { %> 
		<%=request.getParameter("message")%>
		<BR>
		<BR>
	<% } %>
	</logic:notEmpty>
	<A class="l7" href="<%=request.getContextPath()%>/login.do" target="_top">LOG IN</A>
	<BR><BR>
</DIV>

	<%@ include file="/initializationError.jspf"%>


</BODY>
</HTML>
