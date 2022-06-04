package servlet;

import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Servlet implementation class HelloWorldServlet
 */
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final String USER = "sa";
	private static final String PWD = "Riva96_shared_db";
	private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=examDB;encrypt=true;trustServerCertificate=true;";
    
	private static Connection conn;
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
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
    
    private String equalizer (String stringToCheck) {
    	return stringToCheck.replaceAll("\\<.*?\\>", "");
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		String email = request.getParameter("email");
		String pwd = request.getParameter("password");
		String resSalt = "";
		String pwd_hash= "";
		String queryForSalt = "SELECT salt FROM [user] WHERE email = ?";
		try (PreparedStatement result = conn.prepareStatement(queryForSalt)){
			result.setString(1, email);
			ResultSet sqlRes = result.executeQuery();
			if (sqlRes.next()) {
				resSalt = sqlRes.getString(1);
			}
		}catch(SQLException e) {
			e.printStackTrace();
			request.getRequestDispatcher("login.html").forward(request, response);
		}
		
		if(! resSalt.equals("")) {
			try {
				pwd_hash = RegisterServlet.getDigest(pwd,"SHA-256", resSalt);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		String query = "SELECT * FROM [user] WHERE email = ? AND password= ?";
		try (PreparedStatement result = conn.prepareStatement(query)){
			result.setString(1, email);
			result.setString(2, pwd_hash);
			ResultSet sqlRes = result.executeQuery();
			
			if (sqlRes.next()) {
				request.setAttribute("email", equalizer(sqlRes.getString(3)));
				request.setAttribute("password", equalizer(sqlRes.getString(4)));
				
				System.out.println("Login succeeded!");
				request.setAttribute("content", "");
				request.getRequestDispatcher("home.jsp").forward(request, response);
				
				
			} else {
				System.out.println("Login failed!");
				request.getRequestDispatcher("login.html").forward(request, response);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			request.getRequestDispatcher("login.html").forward(request, response);
		}
	}
}
