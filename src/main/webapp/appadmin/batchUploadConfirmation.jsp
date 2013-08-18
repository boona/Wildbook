<%--
	~ The Shepherd Project - A Mark-Recapture Framework
	~ Copyright (C) 2011 Jason Holmberg
	~
	~ This program is free software; you can redistribute it and/or
	~ modify it under the terms of the GNU General Public License
	~ as published by the Free Software Foundation; either version 2
	~ of the License, or (at your option) any later version.
	~
	~ This program is distributed in the hope that it will be useful,
	~ but WITHOUT ANY WARRANTY; without even the implied warranty of
	~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
	~ GNU General Public License for more details.
	~
	~ You should have received a copy of the GNU General Public License
	~ along with this program; if not, write to the Free Software
	~ Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA	02110-1301, USA.
--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page contentType="text/html; charset=iso-8859-1" language="java"
				 import="org.ecocean.CommonConfiguration"
				 import="org.ecocean.Shepherd"
				 import="org.ecocean.batch.BatchData"
				 import="org.ecocean.batch.BatchProcessor"
				 import="org.ecocean.servlet.BatchUpload"
				 import="java.io.File"
         import="java.text.MessageFormat"
         import="java.util.List"
         import="java.util.Locale"
         import="java.util.Properties"
         import="java.util.ResourceBundle"
%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
	response.setHeader("Cache-Control", "no-cache"); //Forces caches to obtain a new copy of the page from the origin server
	response.setHeader("Cache-Control", "no-store"); //Directs caches not to store the page under any circumstance
	response.setDateHeader("Expires", 0); //Causes the proxy cache to see the page as "stale"
	response.setHeader("Pragma", "no-cache"); //HTTP 1.0 backward compatibility

  // Page internationalization.
  String langCode = "en";
  if (session.getAttribute("langCode") != null) {
    langCode = (String)session.getAttribute("langCode");
  } else {
    Locale loc = request.getLocale();
    langCode = loc.getLanguage();
  }
  Properties bundle = new Properties();
  bundle.load(getClass().getResourceAsStream("/bundles/batchUpload_" + langCode + ".properties"));

  List<String> warnings = (List<String>)session.getAttribute(BatchUpload.SESSION_KEY_WARNINGS);
  boolean hasWarnings = warnings != null && !warnings.isEmpty();

  BatchData data = (BatchData)session.getAttribute(BatchUpload.SESSION_KEY_DATA);
  if (data == null) {
    session.removeAttribute("BatchTaskFuture");
    session.removeAttribute("BatchTask");
    response.sendRedirect("batchUpload.jsp");
  }
%>
<html>
<head>
	<title><%=CommonConfiguration.getHTMLTitle() %></title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
	<meta name="Description" content="<%=CommonConfiguration.getHTMLDescription() %>"/>
	<meta name="Keywords" content="<%=CommonConfiguration.getHTMLKeywords() %>"/>
	<meta name="Author" content="<%=CommonConfiguration.getHTMLAuthor() %>"/>
	<link href="<%=CommonConfiguration.getCSSURLLocation(request) %>" rel="stylesheet" type="text/css"/>
	<link rel="shortcut icon" href="<%=CommonConfiguration.getHTMLShortcutIcon() %>"/>
	<link href="../css/batchUpload.css" rel="stylesheet" type="text/css"/>
</head>

<body>
<div id="wrapper">
	<div id="page">
		<jsp:include page="../header.jsp" flush="true">
			<jsp:param name="isAdmin" value="<%=request.isUserInRole(\"admin\")%>"/>
		</jsp:include>
		<div id="main">

			<h1><%=bundle.getProperty("gui.summary.title")%></h1>
			<p><%=bundle.getProperty("gui.summary.overview")%></p>

      <table id="dataSummary">
        <thead></thead>
        <tbody>
          <tr>
            <th><%=bundle.getProperty("gui.summary.label.ind")%></th>
            <td><%=MessageFormat.format(bundle.getProperty("gui.summary.value.ind"), data.listInd.size())%></td>
          </tr>
          <tr>
            <th><%=bundle.getProperty("gui.summary.label.enc")%></th>
            <td><%=MessageFormat.format(bundle.getProperty("gui.summary.value.enc"), data.listEnc.size(), data.getUnassignedEncounterCount())%></td>
          </tr>
          <tr>
            <th><%=bundle.getProperty("gui.summary.label.mea")%></th>
            <td><%=MessageFormat.format(bundle.getProperty("gui.summary.value.mea"), data.listMea == null ? 0 : data.listMea.size())%></td>
          </tr>
          <tr>
            <th><%=bundle.getProperty("gui.summary.label.med")%></th>
            <td><%=MessageFormat.format(bundle.getProperty("gui.summary.value.med"), data.listMed == null ? 0 : data.listMed.size())%></td>
          </tr>
          <tr>
            <th><%=bundle.getProperty("gui.summary.label.sam")%></th>
            <td><%=MessageFormat.format(bundle.getProperty("gui.summary.value.sam"), data.listSam == null ? 0 : data.listSam.size())%></td>
          </tr>
        </tbody>
      </table>
			<p><%=bundle.getProperty("gui.summary.text")%></p>

			<form name="batchSummary" method="post" accept-charset="utf-8" action="../BatchUpload/confirmBatchDataUpload">
			<table id="batchTable">
				<tr>
					<td>
            <input type="submit" id="confirm" value="<%=bundle.getProperty("gui.summary.form.confirm")%>" />
            <input type="button" id="cancel" value="<%=bundle.getProperty("gui.summary.form.cancel")%>" onclick="window.location.href='../appadmin/batchUpload.jsp';return true;" />
          </td>
				</tr>
			</table>
			</form>

<%  if (hasWarnings) { %>
      <div id="warnings">
  			<h2><%=bundle.getProperty("gui.warnings.title")%></h2>
        <ul id="warningList">
        <c:forEach var="msg" items="${batchWarnings}">
          <li>${msg}</li>
        </c:forEach>
        </ul>
      </div>
<%  } %>

			<jsp:include page="../footer.jsp" flush="true"/>
		</div>
	</div>
	<!-- end page --></div>
<!--end wrapper -->
</body>
</html>


