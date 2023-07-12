//Importing the packages
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    // List of client threads and usernames
    static ArrayList<ClientThread> clientThreads = new ArrayList<>();
    static ArrayList<String> names = new ArrayList<>();
    public static String coordinator = null;
    static boolean coordinatorStatus;
    static ScheduledExecutorService coordinatorTimer;



    public static void main(String[] args) {
        int port = 8080;
        String ipAddress = "172.19.53.65";
        coordinatorTimer = Executors.newSingleThreadScheduledExecutor();
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            ServerSocket serverSocket = new ServerSocket(port, 50, inetAddress);
            System.out.println("Server started on port 8080");

            // Accept incoming connections
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket);

                ClientThread clientThread = new ClientThread(socket);
                clientThreads.add(clientThread);
                clientThread.start();
            }
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    // Broadcast a message to all connected clients, except the sender
    public static void broadcast(String message, ClientThread sender) {
        for (ClientThread clientThread : clientThreads) {
            if (clientThread != sender) {
                clientThread.sendMessage(message);
            } else {
                // Handle the "You have been disconnected from the Server" message
                if (message.equals("Disconnecting Inactive Users")) {
                    clientThread.sendMessage("You have been kicked out.");
                    try {
                        clientThread.getSocket().close(); // close the client's socket
                        broadcast(clientThread.getUsername()+" has left the chat due to inactivity", clientThread);
                        if (Server.coordinator.equals(clientThread.getUsername())) {
                            Server.coordinator = Server.names.get(1);
                            Server.sendPrivateMessage("You are the new Coordinator!\nThere is an active" +
                                            " check timer for the Coordinator, if you fail to respond within" +
                                            " 60 seconds you will be disconnected due to inactivity.",
                                    Server.coordinator, sender);
                            Server.startCoordinatorTimer(Server.clientThreads.get(0));
                            Server.broadcast("Server: " + Server.coordinator + " is the new coordinator", null);
                            System.out.println("The coordinator changed from " + sender.getUsername() + " to " + Server.coordinator);
                        }
                        names.remove(clientThread.getUsername());
                        clientThreads.remove(clientThread);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    // Start the coordinator timer
    public static void startCoordinatorTimer(ClientThread sender){
        coordinatorTimer.schedule(() ->{
            var coordinatorStatus = Server.coordinatorStatus;
            if (!coordinatorStatus){
                Server.broadcast("Disconnecting Inactive Users", sender);
            }
            else {
                Server.coordinatorStatus = false;
                startCoordinatorTimer(sender);
            }
        },60, TimeUnit.SECONDS);
    }

    // Send a private message to a specific recipient
    public static void sendPrivateMessage(String message, String recipientName, ClientThread sender) {
        for (ClientThread clientThread : clientThreads) {
            if (clientThread.getUsername().equals(recipientName)) {
                clientThread.sendMessage(sender.getUsername() + " (private): " + message);
                sender.sendMessage("To " + recipientName + " (private): " + message);
                return;
            }
        }
        sender.sendMessage(recipientName + " is not currently online.");
    }

    // Remove a client thread from the list of connected clients
    public static void removeClientThread(ClientThread clientThread) {
        clientThreads.remove(clientThread);
    }
    // Remove a client's username from the list of usernames
    public static void removeClientUsername(String user) {
        names.remove(user);
    }

    // Print a list of active clients
    public static void printActiveClients(ClientThread sender) {
        StringBuilder sb = new StringBuilder();
        sb.append("Active clients:");
        for (ClientThread clientThread : clientThreads) {
            Socket socket = clientThread.getSocket();
            String ipAddress = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            String username = clientThread.getUsername();
            if (username == coordinator) {
                sb.append("\n- ").append(username).append(" (IP Address: ").append(ipAddress).append(" :: " +
                        "Port: ").append(port).append(") (Coordinator)");

            } else {
                sb.append("\n- ").append(username).append(" (IP Address: ").append(ipAddress).append(" :: " +
                        "Port: ").append(port).append(")");
            }
        }
        System.out.println(sb);
        sender.sendMessage(sb.toString());
    }
}


// ClientThread class to handle client connections
class ClientThread extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private ScheduledExecutorService disconnectTimer;
    private AtomicBoolean responded;

    // Constructor to initialize the client thread
    public ClientThread(Socket socket) throws IOException {
        // Initialize instance variables
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        disconnectTimer = Executors.newSingleThreadScheduledExecutor();
        responded = new AtomicBoolean(true);
        // Assign a unique username to the client
        boolean check = false;
        while (true){
            if (check){
                writer.println("The username you chose is already in use. Please enter a valid username:");
            }
            else {
                writer.println("Enter a unique valid username:");
                check = true;
            }
            username = reader.readLine();
            if (username == null) {
                return;
            }
            synchronized (Server.names) {
                if (!username.isEmpty() && !Server.names.contains(username)) {
                    Server.names.add(username);
                    break;
                }
            }
        }
        // If there's no coordinator, make the current client the coordinator
        if (Server.coordinator == null){
            Server.coordinator = username;
            writer.println("Welcome, "+ username + "! You are the Coordinator.\n" +
                    "There is an active check timer for the Coordinator, if you fail to respond within" +
                    " 60 seconds you will be disconnected due to inactivity.");
            Server.coordinatorStatus = false;
            Server.startCoordinatorTimer(this);
            System.out.println("The new client is the coordinator and named themselves: " + username);
        }else{
            // Welcome message for non-coordinator clients
            writer.println("Welcome, " + username + "!\nYou can type '/users' command to check the members.");
            Server.broadcast(username+" has joined the chat!", this);
            System.out.println("The new client named themselves: " + username);
        }
    }

    // The main run method for the client thread
    @Override
    public void run() {
        // Process client messages
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                // Handle different commands and messages from clients
                if (username == Server.coordinator){
                    Server.coordinatorStatus = true;
                }
                if (message.equalsIgnoreCase("/yes")) {
                    responded.set(true);
                } else if (message.startsWith("@")) {
                    int spaceIndex = message.indexOf(' ');
                    if (spaceIndex != -1) {
                        String recipientName = message.substring(1, spaceIndex);
                        String privateMessage = message.substring(spaceIndex + 1);
                        Server.sendPrivateMessage(privateMessage, recipientName, this);
                    }
                } else if (message.equals("/users")) { // handle active client command
                    Server.printActiveClients(this);
                } else if (message.equals("/active") && Server.coordinator.equals(username)) {
                    Server.broadcast("The Coordinator is asking for the active users.\n" +
                            "Please respond with '/yes' within 20 seconds!", this);
                    for (ClientThread clientThread : Server.clientThreads) {
                        if (clientThread != this) {
                            clientThread.startDisconnectTimer();
                        }
                    }
                } else if (message.equals("/quit")) {
                    try {
                        reader.close();
                        writer.close();
                        socket.close();
                    } catch (IOException ex) {
                        System.err.println("Error: " + ex.getMessage());
                    }
                    Server.removeClientThread(this);
                    Server.broadcast(username + " has left the chat.", null);
                    System.out.println(username + " has left the chat.");
                    if (Server.coordinator.equals(username)) {
                        Server.coordinator = Server.names.get(1);
                        Server.sendPrivateMessage("You are the new Coordinator!",
                                Server.coordinator, this);
                        Server.broadcast("Server: " + Server.coordinator + " is the new coordinator", null);
                        System.out.println("The coordinator changed from " + username + " to " + Server.coordinator);
                    }
                    Server.removeClientUsername(username);
                    return;
                } else {// broadcasting messages
                    if (message.startsWith("/")) {
                        System.out.println(username + " entered invalid Command request!");
                    } else {
                        Server.broadcast(username + ": " + message, this);
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(username + " : " + ex.getMessage());
        }finally{
            // Close resources and remove the client from server data structures
            try {
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
            Server.removeClientThread(this);
            Server.broadcast(username + " has left the chat.", null);
            System.out.println(username + " has left the chat.");
            if (Server.coordinator.equals(username)) {
                Server.coordinator = Server.names.get(1);
                Server.sendPrivateMessage("You are the new Coordinator!",
                        Server.coordinator, this);
                Server.broadcast("Server: " + Server.coordinator + " is the new coordinator", null);
                System.out.println("The coordinator changed from " + username + " to " + Server.coordinator);
            }
            Server.removeClientUsername(username);
            return;
        }
    }
    // Send a message to the client
    public void sendMessage(String message) {
        writer.println(message);
    }
    // Get the client's username
    public String getUsername() {
        return username;
    }
    // Get the client's socket
    public Socket getSocket() {
        return socket;
    }
    // Start the disconnect timer for the client
    public void startDisconnectTimer() {
        responded.set(false);
        disconnectTimer.schedule(() -> {
            if (!responded.get()) {
                Server.broadcast("Disconnecting Inactive Users", this);
            }
        }, 20, TimeUnit.SECONDS);
    }
}
