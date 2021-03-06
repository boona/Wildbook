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
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with this program; if not, write to the Free Software
  ~ Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html; charset=utf-8" language="java"
         import="org.ecocean.servlet.ServletUtilities,org.ecocean.*,org.ecocean.grid.*, java.util.ArrayList,java.util.Iterator, java.util.Properties, java.util.concurrent.ThreadPoolExecutor" %>
<%

String context="context0";
context=ServletUtilities.getContext(request);
  //concurrency examination for creation and removal threads
  ThreadPoolExecutor es = SharkGridThreadExecutorService.getExecutorService();

//get a shepherd
  Shepherd myShepherd = new Shepherd(context);

//summon thee a gridManager!
  GridManager gm = GridManagerFactory.getGridManager();
  if (request.getParameter("numAllowedNodes") != null) {
    try {
      int newThrottle = (new Integer(request.getParameter("numAllowedNodes"))).intValue();
      gm.setNumAllowedNodes(newThrottle);
    } catch (NumberFormatException nfe) {
    }
  }
  if (request.getParameter("nodeTimeout") != null) {
    try {
      int newTimeout = (new Integer(request.getParameter("nodeTimeout"))).intValue();
      gm.setNodeTimeout(newTimeout);
    } catch (NumberFormatException nfe) {
    }
  }
  if (request.getParameter("checkoutTimeout") != null) {
    try {
      int newTimeout = (new Integer(request.getParameter("checkoutTimeout"))).intValue();
      gm.setCheckoutTimeout(newTimeout);
    } catch (NumberFormatException nfe) {
    }
  }
  if (request.getParameter("scanTaskLimit") != null) {
    try {
      int limit = (new Integer(request.getParameter("scanTaskLimit"))).intValue();
      gm.setScanTaskLimit(limit);
    } catch (NumberFormatException nfe) {
    }
  }
  if (request.getParameter("maxGroupSize") != null) {
    try {
      int limit = (new Integer(request.getParameter("maxGroupSize"))).intValue();
      gm.setMaxGroupSize(limit);
    } catch (NumberFormatException nfe) {
    }
  }


//setup our Properties object to hold all properties
  Properties props = new Properties();
  String langCode = "en";

  //check what language is requested
  if (request.getParameter("langCode") != null) {
    if (request.getParameter("langCode").equals("fr")) {
      langCode = "fr";
    }
    if (request.getParameter("langCode").equals("de")) {
      langCode = "de";
    }
    if (request.getParameter("langCode").equals("es")) {
      langCode = "es";
    }
  }

  //props.load(getClass().getResourceAsStream("/bundles/" + langCode + "/submit.properties"));
  props=ShepherdProperties.getProperties("submit.properties", langCode);

  //load our variables for the submit page
  String title = props.getProperty("submit_title");
  String submit_maintext = props.getProperty("submit_maintext");
  String submit_reportit = props.getProperty("reportit");
  String submit_language = props.getProperty("language");
  String what_do = props.getProperty("what_do");
  String read_overview = props.getProperty("read_overview");
  String see_all_encounters = props.getProperty("see_all_encounters");
  String see_all_sharks = props.getProperty("see_all_sharks");
  String report_encounter = props.getProperty("report_encounter");
  String log_in = props.getProperty("log_in");
  String contact_us = props.getProperty("contact_us");
  String search = props.getProperty("search");
  String encounter = props.getProperty("encounter");
  String shark = props.getProperty("shark");
  String join_the_dots = props.getProperty("join_the_dots");
  String menu = props.getProperty("menu");
  String last_sightings = props.getProperty("last_sightings");
  String more = props.getProperty("more");
  String ws_info = props.getProperty("ws_info");
  String about = props.getProperty("about");
  String contributors = props.getProperty("contributors");
  String forum = props.getProperty("forum");
  String blog = props.getProperty("blog");
  String area = props.getProperty("area");
  String match = props.getProperty("match");


%>

<html>
<head>
  <title><%=CommonConfiguration.getHTMLTitle(context) %>
  </title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
  <meta name="Description"
        content="<%=CommonConfiguration.getHTMLDescription(context) %>"/>
  <meta name="Keywords"
        content="<%=CommonConfiguration.getHTMLKeywords(context) %>"/>
  <meta name="Author" content="<%=CommonConfiguration.getHTMLAuthor(context) %>"/>
  <link href="<%=CommonConfiguration.getCSSURLLocation(request,context) %>"
        rel="stylesheet" type="text/css"/>
  <link rel="shortcut icon"
        href="<%=CommonConfiguration.getHTMLShortcutIcon(context) %>"/>

  <style type="text/css">
    <!--
    .style1 {
      font-size: x-small;
      font-weight: bold;
    }

    .style2 {
      font-size: x-small;
    }

    -->
  </style>
</head>

<body>
<div id="wrapper">
<div id="page">
<jsp:include page="../header.jsp" flush="true">
  <jsp:param name="isAdmin" value="<%=request.isUserInRole(\"admin\")%>" />
</jsp:include>
<div id="main">

<div id="maincol-wide">

<div id="maintext">
<h1 class="intro">Grid Administration
  <a href="<%=CommonConfiguration.getWikiLocation(context)%>sharkgrid" target="_blank"><img
    src="../images/information_icon_svg.gif" alt="Help" border="0" align="absmiddle"></a></h1>

<%
  myShepherd.beginDBTransaction();
  try {

%>

<h3>Pending scanTasks</h3>
<table border="1" cellpadding="2">
  <tr>
    <td bgcolor="#CCCCCC"><strong>Identifier</strong></td>
    <td bgcolor="#CCCCCC"><strong>User</strong></td>
    <td bgcolor="#CCCCCC"><strong>Completion</strong></td>
    <td bgcolor="#CCCCCC"><strong>Actions</strong></td>
  </tr>
  <%
    Iterator it = myShepherd.getAllScanTasksNoQuery();
    int scanNum = 0;
    while (it.hasNext()) {
      ScanTask st = (ScanTask) it.next();
      if (!st.hasFinished()) {
        scanNum++;
        int numTotal = st.getNumComparisons();

        int numComplete = gm.getNumWorkItemsCompleteForTask(st.getUniqueNumber());

        int numGenerated = gm.getNumWorkItemsIncompleteForTask(st.getUniqueNumber());

        int numTaskTot = numComplete + numGenerated;
        if ((st.getUniqueNumber().equals("TuningTask")) || (st.getUniqueNumber().equals("FalseMatchTask"))) {
          numTaskTot = numGenerated;
        }

  %>
  <tr>
    <td><%=scanNum%>. <%=st.getUniqueNumber()%>
    </td>
    <td><%=st.getSubmitter()%>
    </td>
    <td><%=numComplete%>/<%=numTaskTot%>
    </td>
    <td>
      <%if ((numComplete > 0) && (numComplete >= numTaskTot)) {%>
      <form name="scanNum<%=scanNum%>_writeOut" method="post"
            action="../WriteOutScanTask"><input name="number" type="hidden"
                                                id="number" value="<%=st.getUniqueNumber()%>"> <%
        if (st.getUniqueNumber().equals("TuningTask")) {
      %> Boost weight for failed matches<br/>
        (default is 1): <input name="boostWeight" type="text" id="boostWeight"
                               size="5" maxlength="10"/> <br/>
        <%
          }
        %> <input name="scanNum<%=scanNum%>_WriteResult" type="submit"
                  id="scanNum<%=scanNum%>_WriteResult" value="Write Result"></form>
      <br> <%
      }
      boolean hasPermissionForThisEncounter=false;
      if ((request.isUserInRole("admin")) || (request.getRemoteUser().equals(st.getSubmitter()))) {hasPermissionForThisEncounter=true;}
      else if(myShepherd.isEncounter(st.getUniqueNumber().replaceAll("scanL", "").replaceAll("scanR", ""))){
    	Encounter scanEnc=myShepherd.getEncounter(st.getUniqueNumber().replaceAll("scanL", "").replaceAll("scanR", ""));
    	if((scanEnc.getLocationID()!=null)&&(request.isUserInRole(scanEnc.getLocationID()))){hasPermissionForThisEncounter=true;}  	
      }
      if (hasPermissionForThisEncounter) {%>
      <form name="scanNum<%=scanNum%>" method="post"
            action="../ScanTaskHandler"><input name="action" type="hidden"
                                               id="action" value="removeTask"><input name="taskID"
                                                                                     type="hidden"
                                                                                     id="taskID"
                                                                                     value="<%=st.getUniqueNumber()%>"><input
        name="delete" type="submit" id="delete" value="Delete"></form>
        <br />

        
      <%
        }
      %>

    </td>
  </tr>
  <%
      }
    }
  %>
</table>


<h3>Completed scanTasks</h3>
<table border="1" cellpadding="2">
  <tr>
    <td width="62" bgcolor="#CCCCCC"><strong>Identifier</strong></td>
    <td width="32" bgcolor="#CCCCCC"><strong>User</strong></td>
    <td bgcolor="#CCCCCC"><strong>Results</strong></td>
    <td bgcolor="#CCCCCC"><strong>Actions</strong></td>
	<td bgcolor="#CCCCCC"><strong>ID</strong></td>
  </tr>
  <%
    Iterator it2 = myShepherd.getAllScanTasksNoQuery();
    scanNum = 0;
    while (it2.hasNext()) {
      ScanTask st = (ScanTask) it2.next();
      Encounter scanEnc=new Encounter();
      if(myShepherd.isEncounter(st.getUniqueNumber().replaceAll("scanL", "").replaceAll("scanR", ""))){
      	scanEnc=myShepherd.getEncounter(st.getUniqueNumber().replaceAll("scanL", "").replaceAll("scanR", ""));
      }
      if (st.hasFinished()) {

        //determine if left or right-side scan
        //scanWorkItem[] swis9=st.getWorkItems();
        //scanWorkItem swi9=(scanWorkItem)myShepherd.getScanWorkItemsForTask(st.getUniqueNumber(), 1).next();
        String sideAddition = "false";
        if (st.getUniqueNumber().indexOf("scanR") != -1) {
          sideAddition = "true";
        }

        scanNum++;
  %>
  <tr>

    <td><%=st.getUniqueNumber()%>
    </td>
    <td><%=st.getSubmitter()%>
    </td>
    <%
      String gotoURL = "http://" + CommonConfiguration.getURLLocation(request) + "/encounters/scanEndApplet.jsp";
      if (st.getUniqueNumber().equals("TuningTask")) {
        gotoURL = "endTuningTask.jsp";
      }
    %>

    <td>
      <form name="scanNumJoin<%=scanNum%>" method="get"
            action="<%=gotoURL%>"><input name="rightSide" type="hidden"
                                         id="rightSide" value="<%=sideAddition%>"><input
        name="writeThis" type="hidden" id="writeThis" value="true"><input
        name="number" type="hidden" id="number"
        value="<%=st.getUniqueNumber().substring(5)%>"><input
        name="viewresult" type="submit" id="viewresult" value="View"></form>
    </td>
    <td>
      <%      
      boolean hasPermissionForThisEncounter=false;
      if ((request.isUserInRole("admin")) || (request.getRemoteUser().equals(st.getSubmitter()))) {hasPermissionForThisEncounter=true;}
      else if(myShepherd.isEncounter(st.getUniqueNumber().replaceAll("scanL", "").replaceAll("scanR", ""))){
    	if((scanEnc.getLocationID()!=null)&&(request.isUserInRole(scanEnc.getLocationID()))){hasPermissionForThisEncounter=true;}  	
      }
      if (hasPermissionForThisEncounter) {%>
      <form name="scanNum<%=scanNum%>" method="post"
            action="../ScanTaskHandler"><input name="action" type="hidden"
                                               id="action" value="removeTask"><input name="taskID"
                                                                                     type="hidden"
                                                                                     id="taskID"
                                                                                     value="<%=st.getUniqueNumber()%>"><input
        name="delete" type="submit" id="delete" value="Delete"></form>
      <%
      } else {%> N/A <%
      }
    %>
    </td>



						
						<td>
						<%
						if((scanEnc.getIndividualID()!=null)&&(!scanEnc.getIndividualID().equals("Unassigned"))){
						%>
						<a href="../individuals.jsp?number=<%=scanEnc.getIndividualID()%>"><%=scanEnc.getIndividualID()%></a>
						<%
      					}
      					else{
						%>
						&nbsp;
						<%
      					}
						%>
				
						</td>

  </tr>
  <%
      }
    }
  %>
</table>

<h3>gridManager statistics</h3>

<p>Number of nodes: <%=gm.getNumNodes()%> of <%=gm.getNumAllowedNodes()%>
  allowed*.<br> <span class="style2">*Nodes working on a
single scan are allowed to exceed the total.</span>
</p>
<%
  if (gm.getNumNodes() > 0) {
%>
<table border="1">
  <tr>
    <td width="18" bgcolor="#CCCCCC"><span class="style1">IP</span></td>
    <td width="38" bgcolor="#CCCCCC"><span class="style1">NodeID</span></td>
    <td width="30" bgcolor="#CCCCCC"><span class="style1">#CPU</span></td>
    <td width="51" bgcolor="#CCCCCC"><span class="style1">Targeted?</span></td>
    <td width="62" bgcolor="#CCCCCC"><span class="style1">#
		Finished</span></td>
    <td width="71" bgcolor="#CCCCCC">
      <div align="left"><span class="style1">Chunk size</span></div>
    </td>

  </tr>
  <%
    ArrayList nodes = gm.getNodes();
    int numNodes = nodes.size();
    for (int y = 0; y < numNodes; y++) {
      GridNode nd = (GridNode) nodes.get(y);
      long currenTime = System.currentTimeMillis();
      long nodeTimeout = gm.getNodeTimeout();
      if ((currenTime - nd.getLastHeartbeat()) < nodeTimeout) {
  %>
  <tr>
    <td><span class="style2"><%=nd.ipAddress()%></span></td>
    <td><span class="style2"><%=nd.getNodeIdentifier()%></span></td>
    <td><span class="style2"><%=nd.numProcessors%></span></td>
    <td><span class="style2"><%=nd.isTargeted()%></span></td>
    <td><span class="style2"><%=nd.getNumComparisons()%></span></td>
    <td><span class="style2"><%=nd.groupSize%></span></td>



  </tr>
  <%
      } //end if
    } //end for
  %>
</table>
<%}%>
<p>% inefficent collisions (nodes checking in duplicate work) since
  startup: <%=gm.getCollisionRatePercentage()%>
</p>

<p>Total work items completed since startup: <%=gm.getNumCompletedWorkItems()%>
  (<%=gm.getNumCollisions()%> collisions)</p>

<p>Total work items and results in queue: <%=gm.getNumWorkItemsAndResults()%>
  (To-Do: <%=gm.getToDoSize()%> Done: <%=gm.getDoneSize()%>)</p>

<%
  if (request.isUserInRole("admin")) {
%>
<h3>gridManager adjustment</h3>
<table>
  <tr>
    <form name="setNumAllowedNodes" id="setNumAllowedNodes" method="get"
          action="scanTaskAdmin.jsp">
      <td>Set number of allowed nodes (does not limit targeted scans):
      </td>
      <td><input name="numAllowedNodes" type="text"
                 id="numAllowedNodes" value="<%=gm.getNumAllowedNodes()%>" size="5"
                 maxlength="5"/> <input type="submit" name="Submit" value="Set"/>
      </td>
    </form>
  </tr>
  <tr>
    <form name="setNodeTimeout" id="setNodeTimeout" method="get"
          action="scanTaskAdmin.jsp">
      <td>Set node timeout (milliseconds):</td>
      <td><input name="nodeTimeout" type="text" id="nodeTimeout"
                 value="<%=gm.getNodeTimeout()%>" size="10" maxlength="15"/> <input
        type="submit" name="Submit2" value="Set"/></td>
    </form>
  </tr>
  <tr>
    <form name="setCheckoutTimeout" id="setCheckoutTimeout" method="get"
          action="scanTaskAdmin.jsp">
      <td>Set checkout timeout (milliseconds):</td>
      <td><input name="checkoutTimeout" type="text"
                 id="checkoutTimeout" value="<%=gm.getCheckoutTimeout()%>" size="10"
                 maxlength="15"/> <input type="submit" name="Submit3" value="Set"/>
      </td>
    </form>
  </tr>
  <tr>
    <form name="setScanTaskLimit" id="setScanTaskLimit" method="get"
          action="scanTaskAdmin.jsp">
      <td>Set number allowed scanTasks:</td>
      <td><input name="scanTaskLimit" type="text" id="scanTaskLimit"
                 value="<%=gm.getScanTaskLimit()%>" size="5" maxlength="5"/> <input
        type="submit" name="Submit4" value="Set"/></td>
    </form>
  </tr>
  <tr>
    <form name="setMaxGroupSize" id="setMaxGroupSize" method="get"
          action="scanTaskAdmin.jsp">
      <td>Set maximum chunk/group size sent to nodes:</td>
      <td><input name="maxGroupSize" type="text" id="maxGroupSize"
                 value="<%=gm.maxGroupSize%>" size="5" maxlength="5"/> <input
        type="submit" name="Submit5" value="Set"/></td>
    </form>
  </tr>
</table>
<h3>Creation/deletion threads</h3>

<p>Number of tasks creating/deleteing: <%=es.getActiveCount()%>
  (<%=(es.getTaskCount() - es.getCompletedTaskCount())%>
  total in queue)<br> <br>


  <%}%>

</p>
<%

  } catch (Exception e) {
    System.out.println("Error in scanTaskAdmin.jsp!");
    e.printStackTrace();

  }
  myShepherd.rollbackDBTransaction();
  myShepherd.closeDBTransaction();

%>
</div>
<!-- end maintext --></div>
<!-- end maincol -->
<jsp:include page="../footer.jsp" flush="true">
  <jsp:param name="noscript" value="noscript"/>
</jsp:include>
</div>
<!-- end page --></div>
<!--end wrapper -->
</body>
</html>
