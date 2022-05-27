package servlet;

import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SendMailServlet
 */
@WebServlet("/SendMailServlet")
public class SendMailServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final String USER = "sa";
	private static final String PWD = "Riva96_shared_db";
	private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=examDB;encrypt=true;trustServerCertificate=true;";
    
	private static Connection conn;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SendMailServlet() {
        super();
        // TODO Auto-generated constructor stub
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

    private String equalizer(String stringToCheck) {
    	return stringToCheck.replaceAll("\\<.*?\\>", "");
    }
   
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		String sender = equalizer(request.getParameter("email").replace("'", "''"));;
		String receiver = equalizer(request.getParameter("receiver").replace("'", "''"));;
		String subject = equalizer(request.getParameter("subject").replace("'", "''"));;
		String body = equalizer(request.getParameter("body").replace("'", "''"));;
		String timestamp = equalizer(new Date(System.currentTimeMillis()).toInstant().toString());
		
		String query = "INSERT INTO mail ( sender, receiver, subject, body, [time] ) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement result = conn.prepareStatement(query)){
			result.setString(1, sender);
			result.setString(2, receiver);
			result.setString(3, subject);
			result.setString(4, body);
			result.setString(5, timestamp);
			result.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		request.setAttribute("email", sender);
		request.getRequestDispatcher("home.jsp").forward(request, response);
	}

}
