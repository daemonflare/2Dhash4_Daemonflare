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
        this.hashID = generateHashID(); // Generate hashID when node is instantiated
        //AllNodes.addNode(this); // Add the node to the network
    }

    // Method to generate hashID for the node
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

        // bulk of the code!
        // this is where commands are listened for and executed
        String request;
        while ((request = reader.readLine()) != null) {
            System.out.println("Request received: " + request);
            if (request.startsWith("START")) {
                System.out.println("START " + startingNodeName + " " + startingNodeAddress);
            } else if (request.startsWith("PUT?")) {
                try {
                    /*String[] parts = request.split(" ");
                    if (parts.length >= 3) {
                        int numLinesKey = Integer.parseInt(parts[1]);
                        int numLinesValue = Integer.parseInt(parts[2]);

                        // read key
                        System.out.println("Reading key...");
                        StringBuilder keyBuilder = new StringBuilder();
                        for (int i = 0; i < numLinesKey; i++) {
                            String line = reader.readLine();
                            System.out.println("Key line " + (i + 1) + ": " + line);
                            keyBuilder.append(line);
                            if (i < numLinesKey - 1) {
                                keyBuilder.append("\n");  // dirty failsafe to ensure newline allows another command
                            }
                        }
                        String key = keyBuilder.toString().trim();
                        System.out.println("Key read: " + key);

                        // read value
                        System.out.println("Reading value...");
                        StringBuilder valueBuilder = new StringBuilder();
                        for (int i = 0; i < numLinesValue; i++) {
                            String line = reader.readLine();
                            System.out.println("Value line " + (i + 1) + ": " + line);
                            valueBuilder.append(line);
                            if (i < numLinesValue - 1) {
                                valueBuilder.append("\n");  // i really shouldn't use this lol
                            }
                        }
                        String value = valueBuilder.toString().trim();
                        System.out.println("Value read: " + value);

                        // compute hashID for the key
                        byte[] hashBytes = HashID.computeHashID(key + "\n");
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : hashBytes) {
                            String hex = Integer.toHexString(0xff & b);
                            if (hex.length() == 1) {
                                hexString.append('0');
                            }
                            hexString.append(hex);
                        }
                        String hashID = hexString.toString();

                        List<FullNode> closestNodes = AllNodes.findClosestNodes(hashID, 3);

                        if (closestNodes.contains(this)) {
                            KVPairs.put(key, value);
                            writer.write("SUCCESS\n");
                            writer.flush();
                            System.out.println("Success!");
                            System.out.println(KVPairs);
                        } else {
                            writer.write("FAILED\n");
                            writer.flush();
                            System.out.println("Failed!");
                        }
                    } else {
                        writer.write("Invalid PUT? request\n");
                        writer.flush();
                    }*/
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
            } else if (request.startsWith("GET?")) {
                try {
                    String[] parts = request.split(" ");
                    if (parts.length >= 2) {
                        int numLinesKey = Integer.parseInt(parts[1]);
                        StringBuilder keyBuilder = new StringBuilder();
                        for (int i = 0; i < numLinesKey; i++) {
                            keyBuilder.append(reader.readLine()).append("\n");
                        }
                        String key = keyBuilder.toString().trim();
                        System.out.println("Key: " + key);

                        // searches across ALL nodes, not just this instantiated one :)
                        boolean found = false;
                        /*for (FullNode node : AllNodes.getAllNodes()) {
                            String value = node.get(key);
                            System.out.println("Searching in node: " + node.hashID);
                            if (value != null) {
                                System.out.println("Value found in node " + node.hashID);
                                writer.write("VALUE " + value.split("\n").length + "\n" + value + "\n");
                                writer.flush();
                                found = true;
                                break; // value found!
                            }
                        }*/

                        if (!found) {
                            System.out.println("Value not found in any node");
                            writer.write("NOPE\n");
                            writer.flush();
                        }
                    } else {
                        System.out.println("Invalid request!");
                        writer.write("Invalid GET? request\n");
                        writer.flush();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
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

    public void setStartingNodeName(String startingNodeName) {
        this.startingNodeName = startingNodeName;
        this.hashID = generateHashID();
    }

    public String getStartingNodeName() {
        return startingNodeName;
    }
}

