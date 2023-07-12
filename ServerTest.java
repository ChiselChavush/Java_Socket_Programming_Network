import org.junit.Test;
//This test will check if clients can connect to the server by typing the IP address and port.
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.assertNotNull;

public class ServerTest {

    @Test
    public void testClientConnection() throws IOException {
        int port = 8888;
        String ipAddress = "localhost";
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = new Socket(ipAddress, port);

        assertNotNull(socket);

        socket.close();
        serverSocket.close();
    }
}
