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
    Map<String, String> KVPairs; // KV pair per node
    ServerSocket serverSocket;
    BufferedReader reader;
    BufferedWriter writer;
    String hashID; // hex ID of this node
    String startingNodeName;
    HashMap<Integer, ArrayList<NodeData>> netMap = new HashMap<>();

    public static class NodeData {
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
            System.out.println("FullNode is active @ " + ip + ":" + port);
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
                try {
                    //TODO
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (request.startsWith("ECHO?")) {
                writer.write("OHCE\n");
                writer.flush();
                System.out.println("OHCE fired");
            } else if (request.equals("NOTIFY?")) {
                try {
                    String nodeName = reader.readLine();
                    String nodeAddress = reader.readLine();

                    writer.write("NOTIFIED\n");
                    writer.flush();

                    System.out.println("Received notification for node: " + nodeName + " at address: " + nodeAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (request.startsWith("END")) {
                String[] parts = request.split(" ", 2);
                String reason = parts.length > 1 ? parts[1] : "Unknown reason"; // cheeky way to validate reason
                clientSocket.close();
                System.out.println("connection terminated by client: " + startingNodeName + " with reason: " + reason);
            } else if (request.startsWith("GET?")) { // this one kinda useless lol all it does is print the values
                System.out.println("Full Node request is " + request);
                int keyLength = Integer.parseInt(request.split(" ")[1]);
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = 0; i < keyLength; i++) {
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
                String hashID = parts[1];
                TemporaryNode.stringToHex(hashID);
                List<NodeData> nearestNodes = getNearestNodes(hashID);
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
        String thisNodeHex = TemporaryNode.stringToHex(this.startingNodeName + "\n");
        int distance = calculateDistanceBetweenNodes(thisNodeHex, hexID);
        for (int i = distance; i >= 0 && nearestNodes.size() < 3; i--) {
            List<NodeData> n = netMap.get(distance);
            for (NodeData nodeInfo : n) {
                nearestNodes.add(nodeInfo);
                if (nearestNodes.size() == 3) {
                    break;
                }
            }
        }
        System.out.println(nearestNodes);
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
}

