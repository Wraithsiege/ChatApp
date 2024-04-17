package raf.rs.chatapp.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Application {

    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private TextArea chatArea;
    private TextField textInput;
    private Button replyButton;
    private Button editButton;

    TextField editMessageIDField = new TextField();

    private int flag = 0;


    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        chatArea = new TextArea();
        chatArea.setEditable(false);
        textInput = new TextField();
        textInput.setOnAction(event -> sendMessage());

        replyButton = new Button("Odgovori");
        replyButton.setOnAction(event -> openReplyWindow());

        editButton = new Button("Izmeni");
        editButton.setOnAction(event -> openEditWindow());

        root.setTop(chatArea);
        root.setCenter(textInput);
        root.setBottom(replyButton);
        root.setRight(editButton);

        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Poruke");
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 5700);
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    while(true) {
                        String message = bufferedReader.readLine();
                        if(message == null) {
                            break;
                        }
                        if(message.equals("Clear Chat Log")) {
                            Platform.runLater(() -> chatArea.clear());
                        }
                        if(flag == 0) {
                            Platform.runLater(() -> chatArea.appendText(message + "\n"));
                        }
                        if(flag == 1) {
                            editMessageIDField.setText(message);
                            flag = 0;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = textInput.getText();
        if(!message.isEmpty()) {
            printWriter.println(message);
            textInput.clear();
        }
    }

    public void stop() throws Exception {
        super.stop();
        printWriter.close();
        bufferedReader.close();
    }

    private void openReplyWindow() {
        Stage replyStage = new Stage();
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label messageIDLabel = new Label("Unesite ID poruke na koju hocete da odgovorite");
        TextField messageIDField = new TextField();

        Label messageLabel = new Label("Unesite odgovor");
        TextField messageField = new TextField();

        Button sendButton = new Button("Posalji");
        sendButton.setOnAction(event -> {
            printWriter.println("/reply{" + messageIDField.getText() + "{" + messageField.getText());
            replyStage.close();
        });

        gridPane.add(messageIDLabel, 0, 0);
        gridPane.add(messageIDField, 1, 0);
        gridPane.add(messageLabel, 0, 1);
        gridPane.add(messageField, 1, 1);
        gridPane.add(sendButton, 1, 2);

        Scene replyScene = new Scene(gridPane, 300, 300);
        replyStage.setScene(replyScene);
        replyStage.setTitle("Odgovori");
        replyStage.show();
    }

    private void openEditWindow() {
        Stage editStage = new Stage();
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label messageIDLabel = new Label("Unesite ID poruke koju hocete da izmenite");
        TextField messageIDField = new TextField();

        Label messageLabel = new Label("Unesite novu poruku");
        TextField messageField = new TextField();

        Button sendEditedMessage = new Button("Izmeni");
        sendEditedMessage.setOnAction(event -> {
            printWriter.println("/edit{" + messageIDField.getText() + "{" + editMessageIDField.getText());
            editStage.close();
        });

        Button getMessage = new Button("Ispisi poruku");
/*
        getMessage.setOnAction(event -> {
            printWriter.println("/edit{" + messageIDField.getText());
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    String newMessage = bufferedReader.readLine();
                    System.out.println("AA");
                    if(newMessage != null) {
                        Platform.runLater(() -> messageField.setText(newMessage));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ;
            }).start();
        });*/

/*
        getMessage.setOnAction(event -> {
            printWriter.println("/edit{" + messageIDField.getText());
            try {
                System.out.println("1");
                String newMessage = bufferedReader.readLine();
                System.out.println("2");
                if(newMessage != null) {
                    Platform.runLater(() -> messageField.setText(newMessage));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });*/

        getMessage.setOnAction(event -> {
            flag = 1;
            printWriter.println("/edit{" + messageIDField.getText());
        });

        gridPane.add(messageIDLabel, 0, 0);
        gridPane.add(messageIDField, 1, 0);
        gridPane.add(messageLabel, 0, 1);
        gridPane.add(editMessageIDField, 1, 1);
        gridPane.add(sendEditedMessage, 1, 2);
        gridPane.add(getMessage, 2, 2);

        Scene editScene = new Scene(gridPane, 300, 150);
        editStage.setScene(editScene);
        editStage.setTitle("Izmeni");
        editStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
