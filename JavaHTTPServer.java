import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

//Each client connection will be managed in a dedicated thread
public class JavaHTTPServer implements Runnable {

	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	
	//port to listen Connection
	static final int PORT = 8081;
	
	//verbose mode
	static final boolean verbose = true;
	
	//Client Connection via Socket Class
	private Socket connect;

	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for Connection on port : " + PORT + "...\n");
		
			//We listen until user halts server execution
			while(true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
			
				if(verbose) {
					System.out.println("Connection opened. (" + new Date() + ")");
				}
				
				//create dedicated thread to manage the client connection
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.out.println("Server Connection error : " + e.getMessage());
		}
 	}
	
	@Override
	public void run() {
		// We manage our particular client Connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			//we read characters from the client via input stream on the socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// We get character output stream to client (for header)
			out = new PrintWriter(connect.getOutputStream());
			//get binary output stream to client (for requested data)
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			//get first line of the request of the client
			String input = in.readLine();
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			//we get the HTTP method of the client
			String method = parse.nextToken().toUpperCase();
			//we get file requested
			fileRequested = parse.nextToken().toUpperCase();
			
			//we only support GET an HEAD methods, we chek
			if(!method.equals("GET") && !method.equals("HEAD")) {
				if(verbose) {
					System.out.println("501 Not implemented :" + method + "method");
				}
				
				//we return the supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client 
				byte[] fileData = readFileData(file, fileLength);
			
				//we send HTTP headers with data to client
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: Java HTTP Server from FSchill : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); // blank line between header and content, very important!
				out.flush(); //flush character output stream buffered
				//file
				dataOut.write(fileData,0,fileLength);
				dataOut.flush();
			}
			else {
				//GET or HEAD method
				if(fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
				
				File file = new File(WEB_ROOT,fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);
				
				if(method.equals("GET")) {	//GET method so we return contents
					byte[] fileData = readFileData(file, fileLength);
				
					//send HTTP headers
					out.println("HTTP/1.1 200 OK");
					out.println("Server: Java HTTP Server from FSchill : 1.0");
					out.println("Date: " + new Date());
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println(); // blank line between header and content, very important!
					out.flush(); //flush character output stream buffered
					
					dataOut.write(fileData,0,fileLength);
					dataOut.flush();
				}
				
				if(verbose) {
					System.out.println("File : " + fileRequested + "of Type : " + content + "returned");
				}
				
			}
			
		}
		catch(FileNotFoundException fnfe) {
			try {
				fileNotFound(out,dataOut,fileRequested);
			}
			catch (IOException ioe) {
					System.err.println("Error with File not found Exception : " + ioe.getMessage());
				}
			} 
		
		catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		}
		finally {
			try {
				in.close();	//close Character Input Stream
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if(verbose) {
				System.out.println("Connection closed. \n");
			}
		}
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if(fileIn != null) {
				fileIn.close();
			}
		}
		
		return fileData;
	}
	
	private String getContentType(String fileRequested) {
		if(fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) {
			return "text/html";
		}
		else {
			return "text/plain";
		}
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData (file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from FSchill : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between header and content, very important!
		out.flush(); //flush character output stream buffered
		
		dataOut.write(fileData,0,fileLength);
		dataOut.flush();
		
		if(verbose) {
			System.out.println("File " + fileRequested + "not found.");
		}
	}
}
