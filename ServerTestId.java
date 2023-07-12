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
//This test will check if clients are having unique id
public class ServerTestId {
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
                    String username = reader.readLine();
                    if (Server.names.contains(username)) {
                        writer.println("The username you chose is already in use. Please enter a valid username:");
                    } else {
                        Server.names.add(username);
                        writer.println("Welcome, " + username + "!");
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
    public void testUniqueUsername() throws IOException {
        // Connect two clients with the same username
        Socket socket1 = new Socket("localhost", 8888);
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
        PrintWriter writer1 = new PrintWriter(socket1.getOutputStream(), true);
        writer1.println("username1");
        String response1 = reader1.readLine();

        Socket socket2 = new Socket("localhost", 8888);
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));
        PrintWriter writer2 = new PrintWriter(socket2.getOutputStream(), true);
        writer2.println("username1");
        String response2 = reader2.readLine();

        // Check if the second client was rejected and the server returned the correct response
        assertEquals("The username you chose is already in use. Please enter a valid username:", response2);

        // Close the sockets
        socket1.close();
        socket2.close();
    }
}
