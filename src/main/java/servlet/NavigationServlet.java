package servlet;

import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class NavigationServlet
 */
@WebServlet("/NavigationServlet")
public class NavigationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final String USER = "sa";
	private static final String PWD = "Riva96_shared_db";
	private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=examDB;encrypt=true;trustServerCertificate=true;";
    
	private static Connection conn;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NavigationServlet() {
        super();
    }
    
    public void init() throws ServletException {
    	try {
			Class.forName(DRIVER_CLASS);
			
		    Properties connectionProps = new Properties();
		    connectionProps.put("user", USER);
		    connectionProps.put("password", PWD);
	
	        conn = DriverManager.getConnection(DB_URL, connectionProps);
		    
		    //System.out.println("User \"" + USER + "\" connected to database.");
    	
    	} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		String email = request.getParameter("email").replace("'", "''");;
		String pwd = request.getParameter("password").replace("'", "''");;
		String search = request.getParameter("search");;
				
		if (request.getParameter("newMail") != null)
			request.setAttribute("content", getHtmlForNewMail(email, pwd));
		else if (request.getParameter("inbox") != null)
			request.setAttribute("content", getHtmlForInbox(email, pwd,search));
		
		else if (request.getParameter("sent") != null)
			request.setAttribute("content", getHtmlForSent(email));
		
		request.setAttribute("email", email);
		request.getRequestDispatcher("home.jsp").forward(request, response);
	}

	private String getHtmlForInbox(String email, String pwd, String s ) {
		try (Statement st = conn.createStatement()) {
			String search = "<div ><form id=\"searchInbox\"  action=\"NavigationServlet\" method=\"post\">\r\n"
					+"<input type=\"hidden\" name=\"email\" value=\""+email+"\">"
			+"<input type=\"hidden\" name=\"password\" value=\""+pwd+"\">"
					+ "		<input class=\"single-row-input\" type=\"text\"  name=\"search\"  required>\r\n"
				
					+ "		<input type=\"submit\" name=\"inbox\" value=\"search\">\r\n"
					+ "	</form>";
			
			if(s !=null)
				search+= "<br><p style=\"font-weight:bold; text-decoration:underline; font-style:italic;\">you searched for: <span style=\"color:red;\">"+s+"</span></p>";
			
			if(s == null) 
				s="";
			
			ResultSet sqlRes = st.executeQuery(
					"SELECT * FROM mail "
					+ "WHERE receiver='" + email + "' AND subject LIKE'%"+ s+"%' " 
					+ "ORDER BY [time] DESC"
				);
				
				StringBuilder output = new StringBuilder();
				output.append("<div>\r\n");
				
				while (sqlRes.next()) {
					output.append("<br><div style=\"white-space: pre-wrap;\"><span style=\"color:grey;\">");
					output.append("FROM:&emsp;" + sqlRes.getString(1) + "&emsp;&emsp;AT:&emsp;" + sqlRes.getString(5));
					output.append("</span>");
					output.append("<br><b>" + sqlRes.getString(3) + "</b>\r\n");
					output.append("<br>" + sqlRes.getString(4));
					output.append("</div></div>\r\n");
					
					output.append("<hr style=\"border-top: 2px solid black;\">\r\n");
				}
				
				output.append("</div>");
			return search +output.toString();
			
		} catch (SQLException e) {
			e.printStackTrace();
			return "ERROR IN FETCHING INBOX MAILS!";
		}
	}
	
	
	
	private String getHtmlForNewMail(String email, String pwd) {
		return 
			"<form id=\"submitForm\" class=\"form-resize\" action=\"SendMailServlet\" method=\"post\">\r\n"
			+ "		<input type=\"hidden\" name=\"email\" value=\""+email+"\">\r\n"
			+ "		<input type=\"hidden\" name=\"password\" value=\""+pwd+"\">\r\n"
			+ "		<input class=\"single-row-input\" type=\"email\" name=\"receiver\" placeholder=\"Receiver\" required>\r\n"
			+ "		<input class=\"single-row-input\" type=\"text\"  name=\"subject\" placeholder=\"Subject\" required>\r\n"
			+ "		<textarea class=\"textarea-input\" name=\"body\" placeholder=\"Body\" wrap=\"hard\" required></textarea>\r\n"
			+ "		<input type=\"submit\" name=\"sent\" value=\"Send\">\r\n"
			+ "	</form>";
	}
	
	
	
	private String getHtmlForSent(String email) {
		try (Statement st = conn.createStatement()) {
			ResultSet sqlRes = st.executeQuery(
				"SELECT * FROM mail "
				+ "WHERE sender='" + email + "'"
				+ "ORDER BY [time] DESC"
			);
			
			StringBuilder output = new StringBuilder();
			output.append("<div>\r\n");
			
			while (sqlRes.next()) {
				output.append("<div style=\"white-space: pre-wrap;\"><span style=\"color:grey;\">");
				output.append("TO:&emsp;" + sqlRes.getString(2) + "&emsp;&emsp;AT:&emsp;" + sqlRes.getString(5));
				output.append("</span>");
				output.append("<br><b>" + sqlRes.getString(3) + "</b>\r\n");
				output.append("<br>" + sqlRes.getString(4));
				output.append("</div>\r\n");
				
				output.append("<hr style=\"border-top: 2px solid black;\">\r\n");
			}
			
			output.append("</div>");
			
			return output.toString();
			
		} catch (SQLException e) {
			e.printStackTrace();
			return "ERROR IN FETCHING INBOX MAILS!";
		}
	}
}
