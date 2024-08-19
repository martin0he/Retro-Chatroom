import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatroomServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static List<String> activeUsers = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine();

                synchronized (activeUsers) {
                    activeUsers.add(username); // add user to the active users list
                    broadcastActiveUsers(); // broadcast the updated list of active users
                }

                // display the user's joining message with timestamp
                String joinMessage = String.format("%s joined the chat at %s",
                        username, new SimpleDateFormat("HH:mm:ss").format(new Date()));
                System.out.println(joinMessage);
                broadcastMessage(joinMessage);

                String message;
                while ((message = in.readLine()) != null) {
                    // handle typing status
                    if (message.equals("/typing")) {
                        broadcastTypingStatus("/typing " + username);
                    } else if (message.equals("/stoptyping")) {
                        broadcastTypingStatus("/stoptyping " + username);
                    } else {
                        String formattedMessage = String.format("[%s] %s: %s",
                                new SimpleDateFormat("HH:mm:ss").format(new Date()),
                                username,
                                message);
                        System.out.println(formattedMessage);
                        broadcastMessage(formattedMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (activeUsers) {
                    activeUsers.remove(username); // remove user from the active users list
                    broadcastActiveUsers(); // broadcast the updated list of active users
                }

                clientHandlers.remove(this);
                String leaveMessage = String.format("%s left the chat at %s",
                        username, new SimpleDateFormat("HH:mm:ss").format(new Date()));
                System.out.println(leaveMessage);
                broadcastMessage(leaveMessage);
            }
        }

        private void broadcastMessage(String message) {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    handler.out.println(message);
                }
            }
        }

        private void broadcastTypingStatus(String message) {
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    handler.out.println(message);
                }
            }
        }

        private void broadcastActiveUsers() {
            synchronized (clientHandlers) {
                String userListMessage = "/updateusers " + String.join(",", activeUsers);
                for (ClientHandler handler : clientHandlers) {
                    handler.out.println(userListMessage);
                }
            }
        }
    }
}
