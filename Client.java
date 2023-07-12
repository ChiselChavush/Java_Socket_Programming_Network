import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Client class to connect to the server and communicate with it
public class Client {

    public static void main(String[] args) {
        // Get server port and IP address from user input
        System.out.println("Enter the server Port:");
        Scanner port = new Scanner(System.in);
        int sPort = Integer.parseInt(port.nextLine());
        System.out.println("Enter the Server IP Address:");
        Scanner address = new Scanner(System.in);
        String ipAddress = address.nextLine();
        // Connect to the server and set up input/output streams
        try (Socket socket = new Socket(ipAddress, sPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {

            // Get username from user input and send it to the server
            String username = null;
            while (username == null) {
                System.out.println(reader.readLine());
                String input = consoleReader.readLine();
                writer.println(input);
                username = input;
            }
            // Start a new thread to listen for incoming messages from the server
            new Thread(new ServerListener(socket)).start();

            // Read user input from the console and send it to the server
            while (true) {
                String input = consoleReader.readLine();
                if (input.equalsIgnoreCase("/active")) {
                }
                writer.println(input);
                if (input.equalsIgnoreCase("yes")) {
                }
            }

        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
}
// ServerListener class to handle incoming messages from the server
class ServerListener implements Runnable {
    public Socket socket;
    private boolean isRunning = true;

    // Constructor to initialize the ServerListener with a socket
    public ServerListener(Socket socket) {
        this.socket = socket;
    }

    // Run method to read messages from the server and display them
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while (isRunning && (message = reader.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
}
