import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextInputDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ChatroomClient extends Application {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private TextArea messageArea;
    private TextField inputField;
    private Button sendButton;
    private Label typingLabel;
    private ListView<String> userListView;
    private ObservableList<String> userList;
    private String username;
    private boolean isTyping = false;

    @Override
    public void start(Stage primaryStage) {
        // connect to the server
        connectToServer();

        // set up ui
        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.getStyleClass().add("text-area");


        inputField = new TextField();
        inputField.setPromptText("Enter your message...");
        inputField.getStyleClass().add("text-field");

        typingLabel = new Label();
        typingLabel.setVisible(false);
        typingLabel.getStyleClass().add("label");

        sendButton = new Button("Send");
        sendButton.getStyleClass().add("button");
        sendButton.setOnAction(event -> sendMessage());


        userList = FXCollections.observableArrayList();
        userListView = new ListView<>(userList);
        userListView.getStyleClass().add("list-view");
        VBox.setVgrow(userListView, Priority.ALWAYS);
        VBox.setVgrow(messageArea, Priority.ALWAYS);


        inputField.setOnKeyTyped(event -> handleTyping());
        inputField.setOnKeyReleased(event -> handleStopTyping());

        VBox vbox = new VBox(10, messageArea, typingLabel, inputField, sendButton);
        vbox.getStyleClass().add("root");
        VBox.setVgrow(vbox, Priority.ALWAYS);

        HBox hbox = new HBox(10, userListView, vbox);
        HBox.setHgrow(vbox, Priority.ALWAYS);
        HBox.setHgrow(userListView, Priority.ALWAYS);

        Scene scene = new Scene(hbox, 600, 300);
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

        primaryStage.setTitle("Retro Chatroom");
        primaryStage.setScene(scene);
        primaryStage.show();

        // thread to handle incoming messages
        new Thread(new IncomingMessageHandler()).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345); //  connect to localhost
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // prompt for username
            username = promptForUsername();
            out.println(username); // send the username to the server

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String promptForUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Username");
        dialog.setHeaderText("Enter your username:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse("Anonymous");
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.clear();
            isTyping = false;
            out.println("/stoptyping");
        }
    }

    private void handleTyping() {
        if (!isTyping) {
            isTyping = true;
            out.println("/typing");
        }
    }

    private void handleStopTyping() {
        if (inputField.getText().isEmpty() && isTyping) {
            isTyping = false;
            out.println("/stoptyping");
        }
    }

    private class IncomingMessageHandler implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    Platform.runLater(() -> {
                        if (finalMessage.startsWith("/typing")) {
                            String userTyping = finalMessage.substring(8);
                            typingLabel.setText(userTyping + " is typing...");
                            typingLabel.setVisible(true);
                        } else if (finalMessage.startsWith("/stoptyping")) {
                            typingLabel.setVisible(false); // hide the typing notification
                        } else if (finalMessage.startsWith("/updateusers")) {
                            updateUserList(finalMessage.substring(13)); // update the user list
                        } else {
                            messageArea.appendText(finalMessage + "\n");
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUserList(String userListString) {
        List<String> users = Arrays.asList(userListString.split(","));
        userList.setAll(users); // update active users
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
