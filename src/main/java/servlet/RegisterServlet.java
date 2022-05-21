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
 * Servlet implementation class RegisterServlet
 */
@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static final String USER = "sa";
	private static final String PWD = "Riva96_shared_db";
	private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private static final String DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=examDB;encrypt=true;trustServerCertificate=true;";
    
	private static Connection conn;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RegisterServlet() {
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

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		// The replacement escapes apostrophe special character in order to store it in SQL
		String name = request.getParameter("name").replace("'", "''");
		String surname = request.getParameter("surname").replace("'", "''");;
		String email = request.getParameter("email").replace("'", "''");;
		String pwd = request.getParameter("password").replace("'", "''");;
		
		try (Statement st = conn.createStatement()) {
			ResultSet sqlRes = st.executeQuery(
				"SELECT * "
				+ "FROM [user] "
				+ "WHERE email='" + email + "'"
			);
			
			if (sqlRes.next()) {
				System.out.println("Email already registered!");
				request.getRequestDispatcher("register.html").forward(request, response);
				
			} else {
				st.execute(
					"INSERT INTO [user] ( name, surname, email, password ) "
					+ "VALUES ( '" + name + "', '" + surname + "', '" + email + "', '" + pwd + "' )"
				);
				
				request.setAttribute("email", email);
				request.setAttribute("password", pwd);
				
				System.out.println("Registration succeeded!");
				request.getRequestDispatcher("home.jsp").forward(request, response);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			request.getRequestDispatcher("register.html").forward(request, response);
		}
	}

}
