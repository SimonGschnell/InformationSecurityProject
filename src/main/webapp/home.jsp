<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="ISO-8859-1">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<link rel="stylesheet" href="style.css" type="text/css" />
	
	<title>Home page</title>
</head>
<body>
	<nav class="navbar">
	  <div class="box">
	  	<div>
			<img src="images/email_icon.jpg" align="left" />
			<p>E-MAIL CLIENT
				<br><% out.println(request.getAttribute("email")); %>
			</p>
	  	</div>
	  	<div id="right"><a href="login.html">Logout</a></div>
	  </div>
	</nav>
	
	<div class="grid-container">
		<form class="btn-group" action="NavigationServlet" method="post">
			<input type="hidden" name="email" value="<%= request.getAttribute("email") %>">
			<input type="hidden" name="password" value="<%= request.getAttribute("password") %>">
			<input type="submit" name="newMail" value="New Mail">
			<input type="submit" name="inbox" value="Inbox">
			<input type="submit" name="sent" value="Sent">
		</form>
		
		<%= request.getAttribute("content")!=null ? request.getAttribute("content") : "" %>
	</div>
</body>
</html>