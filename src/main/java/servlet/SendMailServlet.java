package servlet;

import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    

    private String simpleHasher(String inputString) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] input = inputString.getBytes();
		byte[] bytes = md.digest(input);
			
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
			sb.append(String.format("%02x", bytes[i])); //transforms Hexadecimal to String
		}
		
		return sb.toString();
	}
   
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
				//new String(RegisterServlet.generateSalt(), StandardCharsets.UTF_8);
		String hashed_body = "";
		String sender = equalizer(request.getParameter("email").replace("'", "''"));;
		String receiver = equalizer(request.getParameter("receiver").replace("'", "''"));;
		String subject = equalizer(request.getParameter("subject").replace("'", "''"));;
		String body = equalizer(request.getParameter("body").replace("'", "''"));;
		// Ask how to do this stuff of getting parameter....
		//String checkBox = equalizer(request.getParameter("Digital").replace("'", "''"));;
		//System.out.println(checkBox);
		String timestamp = equalizer(new Date(System.currentTimeMillis()).toInstant().toString());
		
		try {
			hashed_body = simpleHasher(body);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		
		String query = "INSERT INTO mail ( sender, receiver, subject, body, digital_signature, [time] ) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement result = conn.prepareStatement(query)){
			result.setString(1, sender);
			result.setString(2, receiver);
			result.setString(3, subject);
			result.setString(4, body);
			result.setString(5, hashed_body);
			//result.setString(6, salt);
			result.setString(6, timestamp);
			result.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		request.setAttribute("email", sender);
		request.getRequestDispatcher("home.jsp").forward(request, response);
	}

}
