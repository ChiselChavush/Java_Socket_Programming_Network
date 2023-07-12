import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.Assert.assertEquals;

//This test will check if the first clients becoming a coordinator directly
//Also if first coordinator disconnects from the server next client becomes a coordinator.
public class ServerTestCoordinator {
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    @Before
    public void setUp() throws IOException {
        int port = 8888;
        String ipAddress = "localhost";
        serverSocket = new ServerSocket(port);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    String message = reader.readLine();
                    if (Server.coordinator == null) {
                        Server.coordinator = message;
                        writer.println("You are the new Coordinator!");
                    } else {
                        writer.println("Welcome, " + message + "!");
                    }
                }
            } catch (IOException e) {
                // Ignore
            }
        });
    }

    @After
    public void tearDown() throws IOException {
        serverSocket.close();
        executorService.shutdown();
    }

    @Test
    public void testFirstClientBecomesCoordinator() throws IOException {
        // Connect two clients
        Socket socket1 = new Socket("localhost", 8888);
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
        PrintWriter writer1 = new PrintWriter(socket1.getOutputStream(), true);

        Socket socket2 = new Socket("localhost", 8888);
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
        PrintWriter writer2 = new PrintWriter(socket2.getOutputStream(), true);

        // Send a message from the first client
        writer1.println("Client 1");

        // Check if the first client becomes the coordinator
        String receivedMessage1 = reader1.readLine();
        assertEquals("You are the new Coordinator!", receivedMessage1);

        // Send a message from the second client
        writer2.println("Client 2");

        // Check if the second client is welcomed as a regular client
        String receivedMessage2 = reader2.readLine();
        assertEquals("Welcome, Client 2!", receivedMessage2);

        // Close the sockets
        socket1.close();
        socket2.close();
    }
}
