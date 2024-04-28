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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLOutput;
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
    HashMap<Integer, ArrayList<NodeData>> netMap = new HashMap<>(); // mapping

    public static class NodeData {
        // this is required to calculate nearest I THINK. i'd be damned if i told you in confidence it works
        String emailName, address;
        int dist;

        public NodeData(String emailName, String address, int dist) {
            this.emailName = emailName;
            this.address = address;
            this.dist = dist;
        }
    }

    public FullNode() {
        KVPairs = new HashMap<>();
        this.hashID = generateHashID();
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

    private void acceptRequest(Socket clientSocket, String startingNodeName, String startingNodeAddress) throws Exception {
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
                    System.out.println("Formatting error with PUT request!");
                    continue;
                }

                int keyLines = Integer.parseInt(requestParts[1]);
                int valLines = Integer.parseInt(requestParts[2]);

                StringBuilder keyBuilder = new StringBuilder();
                StringBuilder valBuilder = new StringBuilder();

                // keys, values
                for (int i = 0; i < keyLines; i++) {
                    String keyLine = reader.readLine();
                    keyBuilder.append(keyLine).append("\n");
                }

                for (int i = 0; i < valLines; i++) {
                    String valueLine = reader.readLine();
                    valBuilder.append(valueLine).append("\n");
                }

                String key = keyBuilder.toString().trim();
                String val = valBuilder.toString().trim();

                KVPairs.put(key, val);
                System.out.println("Store successful");

                writer.write("SUCCESS\n");
                writer.flush();
                System.out.println("Success");
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
                String reason = parts.length > 1 ? parts[1] : "Unknown reason"; // cheeky way to validate reason
                clientSocket.close();
                System.out.println("connection terminated by client: " + startingNodeName + " with reason: " + reason);
            } else if (request.startsWith("GET?")) {
                System.out.println("Full Node request is " + request);
                String[] requestParts = request.split(" ");
                if (requestParts.length != 2) {
                    System.out.println("Invalid GET request format!");
                    return; // or handle the error appropriately
                }
                String keyLength = requestParts[1];
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = 0; i < Integer.parseInt(keyLength); i++) {
                    String str = reader.readLine();
                    System.out.println("String is " + str);
                    keyBuilder.append(str).append("\n");
                }

                String key = keyBuilder.toString();

                if (KVPairs.containsKey(key)) {
                    String value = KVPairs.get(key);
                    System.out.println(value);
                    writer.write("VALUE " + value.split("\n").length + "\n" + value);
                    writer.flush();
                } else {
                    System.out.println("NOPE");
                    writer.write("NOPE\n");
                    writer.flush();
                }
        } else if (request.startsWith("NEAREST")) {
            String[] parts = request.split(" ");
            String convAddress = parts[1];
            System.out.println("convAddress is " + convAddress);
            List<NodeData> nearestNodes = getNearestNodes(convAddress);
            String nodes = "";
            for (NodeData n : nearestNodes) {
                System.out.println(n.emailName);
                System.out.println(n.address);
                System.out.println(n.dist);
            }
            writer.write("NODES " + nearestNodes.size() + "\n" + nodes);
            writer.flush();
        } else {
            System.out.println("invalid request!");
        }
    }

}

    private List<NodeData> getNearestNodes(String hexID) {
        List<NodeData> nearestNodes = new ArrayList<>();
        String thisNodeHex = stringToHex(this.startingNodeName + "\n");
        int distance = calculateDistanceBetweenNodes(thisNodeHex, hexID);
        System.out.println("Distance between nodes " + thisNodeHex + " and " + hexID + " is " + distance);
        for (int i = distance; i >= 0 && nearestNodes.size() < 3; i--) {
            List<NodeData> n = netMap.get(distance);
            System.out.println("Test print of n is " + n.toString());
            for (NodeData nodeInfo : n) {
                nearestNodes.add(nodeInfo);
                System.out.println("node " + nodeInfo + " added!");
                if (nearestNodes.size() == 3) {
                    break;
                }
            }
        }
        System.out.println("nearest nodes is " + nearestNodes);
        return nearestNodes;
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

    public static int calculateDistanceBetweenNodes(String hashID1, String hashID2) {
        String binaryHashID1 = hexToBinary(hashID1);
        String binaryHashID2 = hexToBinary(hashID2);

        int matchingBits = 0;
        for (int i = 0; i < binaryHashID1.length(); i++) {
            if (binaryHashID1.charAt(i) == binaryHashID2.charAt(i)) {
                matchingBits++;
            } else {
                break;
            }
        }

        System.out.println("Distance between two nodes is " + (256 - matchingBits));
        return 256 - matchingBits;
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

    public static String stringToHex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

