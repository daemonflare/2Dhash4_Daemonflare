// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Arber Isufi
// 210016038
// arber.isufi@city.ac.uk

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends


public class FullNode implements FullNodeInterface {
    Map<String, String> KVPairs; // local data pair per node
    ServerSocket serverSocket; // linking socket
    BufferedReader reader; // reader for incoming data
    BufferedWriter writer; // writer for outgoing data
    String hashID; // hex ID of this node
    String startingNodeName; // starting name
    List<FullNode> netMap = new ArrayList<>(); // mapping

    public FullNode() {
        KVPairs = new HashMap<>();
        this.hashID = generateHashID();
        netMap.add(this);
    }

    private String generateHashID() {
        try {
            StringBuilder hexString = new StringBuilder();
            byte[] hashBytes = HashID.computeHashID(startingNodeName + "\n");

            // Convert from an array of bytes to a relevant hexstring
            for (byte bit : hashBytes) {
                String hex = Integer.toHexString(0xff & bit);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean listen(String ip, int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("FullNode is active @" + ip + ":" + port);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String acceptRequest(Socket clientSocket, String startingNodeName, String startingNodeAddress) throws Exception {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        String request;
        while ((request = reader.readLine()) != null) {
            System.out.println("Request received: " + request);
            if (request.startsWith("START")) {
                System.out.println("START " + startingNodeName + " " + startingNodeAddress);
            } else if (request.startsWith("PUT?")) {
                String[] requestParts = request.split(" ", 3);
                if (requestParts.length != 3) {
                    System.out.println("Formatting inconsistency! Try retyping");
                    continue;
                }

                int keyLineCount = Integer.parseInt(requestParts[1]);
                int valLines = Integer.parseInt(requestParts[2]);

                StringBuilder keyConstructor = new StringBuilder();
                StringBuilder valBuilder = new StringBuilder();

                for (int i = 0; i < keyLineCount; i++) {
                    String keyLine = reader.readLine();
                    keyConstructor.append(keyLine).append("\n");
                }

                for (int i = 0; i < valLines; i++) {
                    String valueLine = reader.readLine();
                    valBuilder.append(valueLine).append("\n");
                }

                String key = keyConstructor.toString().trim();
                String val = valBuilder.toString().trim();

                KVPairs.put(key, val);
                System.out.println("Store successful");
                System.out.println(KVPairs);

                writer.write("SUCCESS\n");
                writer.flush();
            } else if (request.startsWith("ECHO?")) {
                writer.write("OHCE\n");
                writer.flush();
                System.out.println("OHCE sent!");
            } else if (request.startsWith("NOTIFY?")) {
                try {
                    String nodeName = reader.readLine();
                    String nodeAddr = reader.readLine();

                    writer.write("NOTIFIED\n");
                    writer.flush();

                    System.out.println("Received notification for node: " + nodeName + " at address: " + nodeAddr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (request.startsWith("END")) {
                String[] parts = request.split(" ", 2);
                String reason = parts.length > 1 ? parts[1] : "Unknown reason"; // fancy way to check reasoning
                clientSocket.close();
                System.out.println("connection terminated by client: " + startingNodeName + " with reason: " + reason);
            } else if (request.startsWith("GET")) {
                String[] requestParts = request.split(" ", 2);
                if (requestParts.length != 2) {
                    System.out.println("Format is invalid! Do keycount, stringtocheck on a newline");
                    continue;
                }

                int keyLineCount;

                try {
                    keyLineCount = Integer.parseInt(requestParts[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid!");
                    continue;
                }

                if (keyLineCount < 1) {
                    System.out.println("Invalid!");
                    continue;
                }

                System.out.println("Key lines count: " + keyLineCount);

                StringBuilder keyConstructor = new StringBuilder();
                for (int i = 0; i < keyLineCount; i++) {
                    String keyLine = reader.readLine();
                    System.out.println("Key line " + (i + 1) + ": " + keyLine);
                    keyConstructor.append(keyLine).append("\n");
                }

                String key = keyConstructor.toString().trim();
                System.out.println("Received key: " + key);

                if (KVPairs.containsKey(key)) {
                    String value = KVPairs.get(key);
                    System.out.println("Value for key: " + key);
                    writer.write("VALUE " + value.split("\n").length);
                    writer.newLine();
                    writer.write(value);
                } else {
                    System.out.println("No value found for key: " + key);
                    writer.write("NOPE");
                }
                writer.newLine();
                writer.flush();
            } else if (request.startsWith("NEAREST")) {
                return "not implemented =(";
            } else {
                System.out.println("invalid request!");
            }
        }
        return null;
    }

    private static String hexToBinary(String hexString) {
        if (hexString == null) {
            return "";
        }
        StringBuilder binaryStringBuilder = new StringBuilder();
        for (int i = 0; i < hexString.length(); i += 2) {
            String hexPair = hexString.substring(i, Math.min(i + 2, hexString.length()));
            String binary = Integer.toBinaryString(Integer.parseInt(hexPair, 16));
            binaryStringBuilder.append(String.format("%8s", binary).replace(' ', '0'));
        }
        return binaryStringBuilder.toString();
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        try {
                            acceptRequest(clientSocket, startingNodeName, startingNodeAddress);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

