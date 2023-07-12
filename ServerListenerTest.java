import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
//This test will check Server Listener class.
public class ServerListenerTest {
    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private int port;

    @BeforeEach
    public void setUp() throws IOException {
        executorService = Executors.newSingleThreadExecutor();
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
    }

    @AfterEach
    public void tearDown() throws IOException {
        serverSocket.close();
        executorService.shutdown();
    }

    @Test
    public void testServerListener() throws IOException, InterruptedException {
        String expectedMessage = "Hello, Client!";
        Socket clientSocket = new Socket("localhost", port);

        executorService.submit(() -> {
            try (Socket socket = serverSocket.accept();
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.println(expectedMessage);
            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        });

        ServerListener serverListener = new ServerListener(clientSocket);
        Thread listenerThread = new Thread(serverListener);
        listenerThread.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String receivedMessage = reader.readLine();
            assertEquals(expectedMessage, receivedMessage);
        }

        clientSocket.close();
    }
}