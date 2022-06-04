package servlet;

import jakarta.servlet.http.HttpServlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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
    	
    	} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    }

    private String equalizer(String stringToCheck) {
    	return stringToCheck.replaceAll("\\<.*?\\>", "");
    }
    
    public static String getDigest(String inputString, String hashAlgorithm, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md;
		String completerInput = inputString+salt;
		md = MessageDigest.getInstance(hashAlgorithm);
		byte[] input = completerInput.getBytes();
		byte[] bytes = md.digest(input);
			
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
			sb.append(String.format("%02x", bytes[i])); //transforms Hexadecimal to String
		}
		
		return sb.toString();
	}
    
    public byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[7];
        random.nextBytes(bytes);
        return bytes;
    }
    
    private void savePrivateKey(Integer key, Integer n, String user) throws IOException {
    	
        File catalinaBase = new File(System.getProperty("catalina.home")).getAbsoluteFile();
        new File(System.getProperty("catalina.home")+"/privateKeys").mkdirs();
       
    	String webSettingFileName = user+".txt"; 
    	File file = new File(catalinaBase,"privateKeys/"+webSettingFileName);
    	
    	FileWriter fw = new FileWriter(file);
    	fw.write(key+"\n"+n);
    	
    	fw.close();
    	
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		// The replacement escapes apostrophe special character in order to store it in SQL
		String name = equalizer(request.getParameter("name").replace("'", "''"));
		String surname = equalizer(request.getParameter("surname").replace("'", "''"));;
		String email = equalizer(request.getParameter("email").replace("'", "''"));;
		String pwd = equalizer(request.getParameter("password").replace("'", "''"));;
		
		String salt = new String(generateSalt(), StandardCharsets.UTF_8);
		String pwd_hash = "";
		
		try {
			pwd_hash = getDigest(pwd,"SHA-256", salt);
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		HashMap<String, Integer> generatedKeys = new HashMap<>();
		try {
			generatedKeys= DigitalSignature.generateKeys();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		String alreadyRegisteredQuery = "SELECT * FROM [user] WHERE email = ?";
		String registerQuery = "INSERT INTO [user] ( name, surname, email, password, salt, e, n) VALUES (?,?,?,?,?,?,?)";
		try (PreparedStatement result = conn.prepareStatement(alreadyRegisteredQuery)) {
			result.setString(1, email);
			ResultSet sqlRes = result.executeQuery();
			
			if (sqlRes.next()) {
				System.out.println("Email already registered!");
				request.getRequestDispatcher("register.html").forward(request, response);
				
			} else {
				PreparedStatement res = conn.prepareStatement(registerQuery);
				res.setString(1, name);
				res.setString(2, surname);
				res.setString(3, email);
				res.setString(4, pwd_hash);
				res.setString(5, salt);
				res.setInt(6, generatedKeys.get("public"));
				res.setInt(7, generatedKeys.get("n"));
				res.executeUpdate();
				
				request.setAttribute("email", email);
				request.setAttribute("password", pwd_hash);
				
				System.out.println("Registration succeeded!");
				
				savePrivateKey(generatedKeys.get("private"),generatedKeys.get("n"),email);
				
				request.getRequestDispatcher("home.jsp").forward(request, response);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			request.getRequestDispatcher("register.html").forward(request, response);
		}
	}
}