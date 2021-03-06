package servlet;

import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    }
    
    public void init() throws ServletException {
    	try {
			Class.forName(DRIVER_CLASS);
			
		    Properties connectionProps = new Properties();
		    connectionProps.put("user", USER);
		    connectionProps.put("password", PWD);
	
	        conn = DriverManager.getConnection(DB_URL, connectionProps);
    	
    	} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    }

    private String equalizer(String stringToCheck) {
    	return stringToCheck.replaceAll("\\<.*?\\>", "");
    }

    public static String simpleHasher(String inputString) throws NoSuchAlgorithmException, UnsupportedEncodingException {
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
				
		String hashed_body = null;
		boolean checkBox = false;
		String sender = equalizer(request.getParameter("email").replace("'", "''"));;
		String receiver = equalizer(request.getParameter("receiver").replace("'", "''"));;
		String subject = equalizer(request.getParameter("subject").replace("'", "''"));;
		String body = equalizer(request.getParameter("body").replace("'", "''"));;

		if(request.getParameter("digital") != null) {
			checkBox = Boolean.parseBoolean(request.getParameter("digital"));
		}
		String timestamp = equalizer(new Date(System.currentTimeMillis()).toInstant().toString());
		
		if(checkBox) {
			try {
				hashed_body = simpleHasher(body);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}	
		
		String query = "INSERT INTO mail ( sender, receiver, subject, body, digitalSignature, [time] ) VALUES (?, ?, ?, ?, ?, ?)";

		Integer e=0;
		Integer n=0;
		String encriptedBody = "";
		String encryptedSignature = "";
		String publicKeyQuery = "SELECT e,n FROM [user]  WHERE email = ?";
		try (PreparedStatement result = conn.prepareStatement(publicKeyQuery)){
			result.setString(1, receiver);
			ResultSet set = result.executeQuery();
			while (set.next()) {
				e = Integer.parseInt(set.getString(1));
				n = Integer.parseInt(set.getString(2));
				int[] list  = DigitalSignature.encrypt(body, e, n);
				
				for(int i : list) {
					
						encriptedBody+=i+",";
					
				}
				encriptedBody = encriptedBody.substring(0, encriptedBody.length()-1);
				
				if(hashed_body != null) {
					int[] list2 = DigitalSignature.encrypt(hashed_body, NavigationServlet.readPrivateKey(sender).get("private"), NavigationServlet.readPrivateKey(sender).get("n"));
					for(int i : list2) {
						encryptedSignature+=i+",";
					}
					
					encryptedSignature = encryptedSignature.substring(0, encryptedSignature.length()-1);
				}

			}
		}catch (SQLException e1) {
			e1.printStackTrace();
		}

		try (PreparedStatement result = conn.prepareStatement(query)){
			result.setString(1, sender);
			result.setString(2, receiver);
			result.setString(3, subject);
			result.setString(4, encriptedBody);
			result.setString(5, encryptedSignature);

			result.setString(6, timestamp);
			result.executeUpdate();
			
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		
		request.setAttribute("email", sender);
		request.getRequestDispatcher("home.jsp").forward(request, response);
	}
}