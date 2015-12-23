import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class ConsoleClient
{
	private static int PORT = 9000;
	private static String HOST = "localhost";
	Socket m_socket = null;
	PrintWriter m_out = null;
	BufferedReader m_in = null;

	public static void main(String[] args) throws IOException {

		if (args.length == 1)
		{
			ConsoleClient client = new ConsoleClient();
			client.listen(args[0]);
			client.close();
		}
		else
		{
			printUsage();
		}
	}

	private static void printUsage() {
		System.out.println("java ConsoleClient <host>");
	}

	private void close() 
	{
		try {
			m_out.close();
			m_in.close();
			m_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void listen(String host) {
		try {
			// Set up the socket and readers/writers
			m_socket = new Socket(host, PORT);
			m_out = new PrintWriter(m_socket.getOutputStream(), true);
			m_in = new BufferedReader(new InputStreamReader(
					m_socket.getInputStream()));
			
			System.out.println("CLIENT: Connected to server");

			SocketReaderThread readerThread = new SocketReaderThread(m_in);
			readerThread.start();

			BufferedReader stdIn = new BufferedReader(
					new InputStreamReader(System.in));

			String userInput;

			System.out.println("CLIENT: Listening for user input");
			while ((userInput = stdIn.readLine()) != null) {
				// Listen for a quit
				if (userInput.equals("q"))
				{
					break;
				}
				m_out.println(userInput);
			}
		} catch (UnknownHostException e) {
			System.err.println("CLIENT: Don't know about host: " + HOST);
			System.exit(1);
		} catch (IOException e) {
			System.err.println("CLIENT: Couldn't get I/O for "
					+ "the connection to: " + HOST);
			System.exit(1);
		}
	
	}

	public class SocketReaderThread extends Thread
	{
		BufferedReader m_in;
		public SocketReaderThread(BufferedReader in)
		{
			m_in = in;
		}

		@Override
		public void run() {
			String strRead;
			System.out.println("CLIENT: Listening to server");
			try {
				while((strRead = m_in.readLine()) != null)
				{
					System.out.println(strRead);
				}
			} catch (IOException e) {
				System.out.println("CLIENT: Terminating console reader");
			}
			System.out.println("CLIENT: Done listening");
		}	
	}
}
