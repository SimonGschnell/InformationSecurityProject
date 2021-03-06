package servlet;

import jakarta.servlet.http.HttpServlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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
    	
    	} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    
    private String equalizer(String stringToCheck) {
    	return stringToCheck.replaceAll("\\<.*?\\>", "");
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		
		String email = equalizer(request.getParameter("email").replace("'", "''"));;
		String pwd = equalizer(request.getParameter("password").replace("'", "''"));;
		String search="";
		if(request.getParameter("search")!=null){
			 search= equalizer(request.getParameter("search"));
		};;
				
		if (request.getParameter("newMail") != null)
			request.setAttribute("content", getHtmlForNewMail(email, pwd));
		else if (request.getParameter("inbox") != null)
			try {
				request.setAttribute("content", getHtmlForInbox(email, pwd,search));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		else if (request.getParameter("sent") != null)
			request.setAttribute("content", getHtmlForSent(email));
		
		request.setAttribute("email", email);
		request.getRequestDispatcher("home.jsp").forward(request, response);
	}
	
	static public HashMap<String,Integer> readPrivateKey(String email) {
		File catalinaBase = new File(System.getProperty("catalina.home")).getAbsoluteFile();
        
    	File file = new File(catalinaBase,"privateKeys/"+email+".txt");
    	Integer d = 0;
    	Integer n = 0;
    	try {
    	FileReader fr = new FileReader(file);
    	BufferedReader br = new BufferedReader(fr);
    	d = Integer.parseInt(br.readLine());
    	
    	n = Integer.parseInt(br.readLine());
    	
    	br.close();
    	}catch(Exception e ) {
    		e.printStackTrace();
    		
    	}
    	HashMap<String,Integer> publicKey = new HashMap<String,Integer>();
    	publicKey.put("private", d);
    	publicKey.put("n", n);
    	return publicKey;
    	
	}
	
	private String validateSignature(String sender, String encryptedSignature, String body) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		Integer e=0;
		Integer n=0;
		String publicKeyQuery = "SELECT e,n FROM [user]  WHERE email = ?";
		
		if(!encryptedSignature.isEmpty()) {
			try (PreparedStatement result = conn.prepareStatement(publicKeyQuery)){
				result.setString(1, sender);
				ResultSet set = result.executeQuery();
				while (set.next()) {
					e = Integer.parseInt(set.getString(1));
					n = Integer.parseInt(set.getString(2));
				}
			}catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			String[] cipherTextSig = encryptedSignature.split(",");
			int[] cipherSig = new int[cipherTextSig.length];
			for(int i =0; i<cipherTextSig.length; i++) {
				cipherSig[i] = Integer.parseInt(cipherTextSig[i]);
			}

			String signatureHash = DigitalSignature.decrypt(cipherSig,e,n);
			
			String bodyHash = SendMailServlet.simpleHasher(body);
			
			if(signatureHash.equals(bodyHash))
				return "Email has been validated";
			else
				return "Email failed validation";
		}
		else
			return "No signature";
	}

	private String getHtmlForInbox(String email, String pwd, String search_output ) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String query="SELECT * FROM mail WHERE receiver =? AND subject LIKE ? ORDER BY [time] DESC";
		try(PreparedStatement result = conn.prepareStatement(query)){
			
			String search = "<div ><form id=\"searchInbox\"  action=\"NavigationServlet\" method=\"post\">\r\n"
					+"<input type=\"hidden\" name=\"email\" value=\""+email+"\">"
			+"<input type=\"hidden\" name=\"password\" value=\""+pwd+"\">"
					+ "		<input class=\"single-row-input\" type=\"text\"  name=\"search\"  required>\r\n"
				
					+ "		<input type=\"submit\" name=\"inbox\" value=\"search\">\r\n"
					+ "	</form>";

			if(search_output !=null)
				search+= "<br><p style=\"font-weight:bold; text-decoration:underline; font-style:italic;\">you searched for: <span style=\"color:red;\">"+search_output+"</span></p>";
			
			if(search_output == null) 
				search_output="";
			
			result.setString(1, email);
			result.setString(2, "%"+search_output+"%");
			ResultSet res = result.executeQuery();
			
			StringBuilder output = new StringBuilder();
			output.append("<div>\r\n");
			
			HashMap<String,Integer> publicKey = readPrivateKey(email);
			while (res.next()) {
				String[] cipherText = res.getString(4).split(",");
				int[] cipher = new int[cipherText.length];
				for(int i =0; i<cipherText.length; i++) {
					cipher[i] = Integer.parseInt(cipherText[i]);
				}
				
				String body = DigitalSignature.decrypt(cipher
						,publicKey.get("private") 
						,publicKey.get("n"));
				output.append("<br><div style=\"white-space: pre-wrap;\"><span style=\"color:grey;\">");
				output.append("FROM:&emsp;" + equalizer(res.getString(1)) + "&emsp;&emsp;AT:&emsp;" + equalizer(res.getString(6) + "&emsp;&emsp;Signature:&emsp;" + equalizer(validateSignature(res.getString(1),res.getString(5),body))));
				output.append("</span>");
				output.append("<br><b>" + equalizer(res.getString(3)) + "</b>\r\n");
				output.append("<br>" + equalizer(body) + "\r\n");
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
			"<div class = \"flex-container\">"
				+	"<form id=\"submitForm\" class=\"form-resize\" action=\"SendMailServlet\" method=\"post\">\r\n"
			+ "		<input type=\"hidden\" name=\"email\" value=\""+email+"\">\r\n"
			+ "		<input type=\"hidden\" name=\"password\" value=\""+pwd+"\">\r\n"
			+ "		<input class=\"single-row-input\" type=\"email\" name=\"receiver\" placeholder=\"Receiver\" required>\r\n"
			+ "		<input class=\"single-row-input\" type=\"text\"  name=\"subject\" placeholder=\"Subject\" required>\r\n"
			+ "		<textarea class=\"textarea-input\" name=\"body\" placeholder=\"Body\" wrap=\"hard\" required></textarea>\r\n"
			+ "		<input type=\"submit\" name=\"sent\" value=\"Send\">\r\n"
			+ "<input type=\"checkbox\" id=\"digital\" name=\"digital\" value=\"true\" > Digital Signature"
			+ "	</form>\r\n"
			+" </div>";
	}
	
	private String getHtmlForSent(String email) {
		String query = "SELECT * FROM mail WHERE sender = ? ORDER BY [time] DESC";
		try(PreparedStatement result = conn.prepareStatement(query)){
			result.setString(1, email);
			ResultSet res = result.executeQuery();
			
			StringBuilder output = new StringBuilder();
			output.append("<div>\r\n");
			
			while (res.next()) {
				output.append("<div style=\"white-space: pre-wrap;\"><span style=\"color:grey;\">");
				output.append("TO:&emsp;" + equalizer(res.getString(2)) + "&emsp;&emsp;AT:&emsp;" + equalizer(res.getString(6)));
				output.append("</span>");
				output.append("<br><b>" + equalizer(res.getString(3)) + "</b>\r\n");
				output.append("<br>" + equalizer(res.getString(4)));
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