package raf.rs.chatapp.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Server {

    private static final int port = 5700;
    private static final String chatLog = "chatlog.txt";
    private static final String counterLog = "counterlog.txt";
    private static final String senderMap = "sendermap.txt";

    //private static Map<String, PrintWriter> userWriters = new HashMap<>();
    private static Map<String, PrintWriter> userWriters = new ConcurrentHashMap<>();
    //private static Set<String> usernames = new HashSet<>();
    private static Set<String> usernames = new CopyOnWriteArraySet<>();

    private static Map<Integer, String> messageSenders = new ConcurrentHashMap<>();

    private static Set<String> sentMessages = new CopyOnWriteArraySet<>();

    private static PrintWriter editPrintWriter;

    public static int messageID = 0;

    public static void main(String[] args) throws IOException {
        System.out.println("Server je online");
        loadCounterLog();
        loadSenderMap();
        saveSenderMap();
        saveToCounterLog(messageID);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while(true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch(IOException e) {
            System.out.println("Greska!" + e.getMessage());
        } finally {
            saveSenderMap();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter printWriter;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream(), true);

                while(true) {
                    printWriter.println("Unesite korisnicko ime: ");
                    username = bufferedReader.readLine();
                    if (username == null) {
                        return;
                    }
                    synchronized (usernames) {
                        if (!usernames.contains(username)) {
                            usernames.add(username);
                            break;
                        }
                    }
                }
                printWriter.println("Dobrodosao/la: " + username);

                synchronized (userWriters) {
                    userWriters.put(username, printWriter);
                }

                String message;

                while((message = bufferedReader.readLine()) != null) {
                    if(message.startsWith("/")) {

                        //MOD

                        if(message.startsWith("/edit")) {
                            String[] receivedMessage = message.split("\\{");

                            editPrintWriter = new PrintWriter(socket.getOutputStream(), true);

                            if(receivedMessage.length == 2) {
                                String returnMessage = returnMessage(chatLog, receivedMessage[1]);

                                editPrintWriter.println(returnMessage);
                            }
                            /*
                            if(receivedMessage.length == 3) {

                                editMessage(chatLog, receivedMessage[1], receivedMessage[2]);

                                for(PrintWriter writer : userWriters.values()) {

                                    sendChatLog(chatLog);
                                }
                            }*/
                            if(receivedMessage.length == 3) {
                                //System.out.println(receivedMessage[1]);
                                String sender = messageSenders.get(Integer.parseInt(receivedMessage[1]));
                                //System.out.println(messageSenders);
                                if(sender != null && sender.equals(username)) {
                                    editMessage(chatLog, receivedMessage[1], receivedMessage[2]);
                                    sendChatLog(chatLog);
                                }
                                else {
                                    System.out.println("Error");
                                }
                            }
                        }

                        if(message.startsWith("/reply")) {
                            String[] receivedMessage = message.split("\\{", 3);



                            //saveToChatLog(++messageID + "{" + username + "{" + receivedMessage[1] + "(Odgovor): " + receivedMessage[2]);
                            saveToChatLog(++messageID + "{" + username + "{" + "}" + "(Odgovor): " + returnMessage(chatLog, receivedMessage[1]) + receivedMessage[2]);
                            saveToCounterLog(messageID);

                            messageSenders.put(messageID, username);
                            saveSenderMap();

                            for(PrintWriter writer : userWriters.values()) {
                                //writer.println("(" + messageID + ") " + username + ": " + "(" + returnMessage(chatLog, receivedMessage[1]) + ") \n" + "(Odgovor):" + receivedMessage[2]);
                                writer.println(returnLastMessage(chatLog));
                            }
                        }

                        //MOD


                        if(message.equalsIgnoreCase("/quit")) {
                            return;
                        }
                    }
                    else {
                        synchronized (userWriters) {

                            String chatLogMessage = ++messageID + "{" +  username + "{" + message;

                            saveToChatLog(chatLogMessage);
                            saveToCounterLog(messageID);

                            messageSenders.put(messageID, username);
                            saveSenderMap();

                            for(PrintWriter writer : userWriters.values()) {
                                writer.println("("+ messageID + ")" + username + ": " + message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Greska: " + e.getMessage());
            } finally {
                if(username != null) {
                    usernames.remove(username);
                }
                if(printWriter != null) {
                    userWriters.remove(username);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Greska: " + e.getMessage());
                }
            }
        }

        private void saveToChatLog(String message) {
            try (FileWriter fileWriter = new FileWriter(chatLog, true);
                 BufferedWriter bw = new BufferedWriter(fileWriter);
                 PrintWriter pw = new PrintWriter(bw)) {
                String[] parts = message.split("}");
                if(parts.length == 3) {
                    pw.println(parts[0] + "}" + parts[1] + "}" + parts[2] + "}");
                }
                else if(parts.length == 5) {
                    pw.println(parts[0] + "}" + parts[1] + parts[3] + "}" + parts[4] + "}");
                }
                else {
                    pw.println(message + "}");
                }
            } catch (IOException e) {
                System.out.println("Greska prilikom upisivanja u chat log: " + e.getMessage());
            }
        }

        private static String returnLastMessage(String filename) {
            String lastMessage = null;

            try(BufferedReader br = new BufferedReader(new FileReader("chatlog.txt"))) {
                String line;
                while((line = br.readLine()) != null) {
                    String[] parts = line.split("[{}]");
                    if(parts.length == 5) {
                        lastMessage = "(" + parts[0] + ")" + parts[3] + "\n" + parts[1] + ": " +  parts[4];
                    }
                    //lastMessage = "(" + parts[0] + ")" + parts[1] + parts[3] + parts[4];
                    //lastMessage = line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return lastMessage;
        }

        private static String returnMessage(String filename, String messageID) throws IOException {
            Scanner scanner = new Scanner(new File(filename));
            String message = "";

            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();

                String[] parts = line.split("\\{");

                if(parts.length > 2 && parts[0].trim().equals(messageID)) {
                    message = parts[2].trim();
                    if(message.startsWith("(Odgovor)")) {
                        message = message.substring(11);
                    }
                    break;
                }
            }
            scanner.close();

            return message;
        }
    }

    private static void editMessage(String filename, String messageID, String newMessage) throws IOException {
        System.out.println(messageID);
        System.out.println(newMessage);
        File originalFile = new File(filename);
        File temporaryFile = new File("temp.txt");

        BufferedReader br = new BufferedReader(new FileReader(originalFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(temporaryFile));

        String line;
        boolean isEdited = false;

        while((line = br.readLine()) != null) {
            String[] parts = line.split("\\{");

            if(parts.length == 3 && parts[0].trim().equals(messageID)) {
                line = parts[0] + "{" + parts[1] + "{" + "(Izmena) " + newMessage + "}";
                isEdited = true;
            }

            bw.write(line + System.getProperty("line.separator"));
        }

        br.close();
        bw.close();

        if(isEdited == true) {
            if(originalFile.delete()){
                System.out.println("Obrisao sam originalnu datoteku");
                if(temporaryFile.renameTo(new File(filename))) {
                    System.out.println("Uspesno sam preimenovao datoteku");
                }
                else System.out.println("Nisam mogao da preimenujem datoteku");
            }
            else System.out.println("Ne");
        }
    }
/*
    private static void sendChatLog(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));

        String line;

        while((line = br.readLine()) != null) {
            for(PrintWriter writer : userWriters.values()) {
                String[] parts = line.split("\\{");
                if(parts.length == 3) {
                    writer.println("(" + parts[0] + ") " + parts[1] + ": " + parts[2]);
                }
            }
        }

        br.close();
    }*/

    private static void sendChatLog(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));

        String line;

        StringBuilder chatLog = new StringBuilder();
        while((line = br.readLine()) != null) {/*
            String[] parts = line.split("\\{");
            if(parts.length == 3) {
                chatLog.append("(").append(parts[0]).append(") ").append(parts[1]).append(": ").append(parts[2]).append("\n");
            }*/

            String[] parts = line.split("[{}]");
            if(parts.length == 3) {
                chatLog.append("(").append(parts[0]).append(") ").append(parts[1]).append(": ").append(parts[2]).append("\n");
            }
            else if(parts.length == 5) {
                chatLog.append("(").append(parts[0]).append(") ").append(parts[3]).append("\n").append(parts[1]).append(": ").append(parts[4]).append("\n");
            }
            else if(parts.length != 3 || parts.length != 5) {
                System.out.println("Duzina: " + parts.length);
                chatLog.append("Greska u formatiranju");
            }

        }
        br.close();

        for(PrintWriter writer : userWriters.values()) {
            writer.println("Clear Chat Log");
            writer.print(chatLog);
            writer.flush();
        }
    }

    private static void saveToCounterLog(Integer counter) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(counterLog))) {
            bw.write(counter.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadCounterLog() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(counterLog));

        String line;

        while((line = br.readLine()) != null) {
            messageID = Integer.parseInt(line);
        }
    }

    private static void saveSenderMap() {
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(senderMap))) {
            objectOutputStream.writeObject(messageSenders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked") // AJDE SAD SE BUNI BRE
    private static void loadSenderMap() {
        File file = new File(senderMap);
        if(file.exists()) {
            try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(senderMap))) {
                messageSenders = (Map<Integer, String>) objectInputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
