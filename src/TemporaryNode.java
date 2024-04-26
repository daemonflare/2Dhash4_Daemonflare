// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Arber Isufi
// 210016038
// arber.isufi@city.ac.uk

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// DO NOT EDIT starts
interface TemporaryNodeInterface {
    public boolean start(String startingNodeName, String startingNodeAddress) throws IOException;

    public boolean store(String key, String value);

    public String get(String key);
}
// DO NOT EDIT ends


public class TemporaryNode implements TemporaryNodeInterface {
    String name, address;
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;
    Map<String, String> visitedNodes = new HashMap<>();

    public boolean start(String startingNodeName, String startingNodeAddress) {
        this.name = startingNodeName;
        this.address = startingNodeAddress;
        try {
            // socket splicing so that it doesn't get angry over string/proxy conflicts:
            String[] segments = startingNodeAddress.split(":");
            String ip = segments[0];
            int port = Integer.parseInt(segments[1]);

            socket = new Socket(ip, port);
            System.out.println(0);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            System.out.println(1);

            writer.write("START 1 " + name + "\n");
            writer.flush();

            System.out.println(reader.readLine());

            return true;
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean store(String key, String value) {
        try {
            writer.write("PUT? 1 " + value.split("\n").length + "\n");
            writer.write(key + "\n");
            writer.write(value + "\n");
            writer.flush();

            String response = reader.readLine();

            String hexString = stringToHex(key);
            List<FullNode.NodeData> nodes = getNearestNodes(reader,writer,hexString);
            for (FullNode.NodeData ignored : nodes) {
                invokeNearest(hexString);
            }
            if (response.equals("SUCCESS")) {
                return true;
            } else if (response.equals("FAILED")) {
                return false;
            } else {
                System.out.println("invalid: " + response);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String get(String key) {
        try {
            if (!key.endsWith("\n")){
                key += "\n";
            }

            int keyNo = key.split("\n").length;

            writer.write("GET? " + keyNo + "\n");
            writer.write(key);
            writer.flush();

            System.out.println("Sent: GET? " + keyNo);

            String response = reader.readLine();

            if (response.startsWith("VALUE")) {
                int valueNo = Integer.parseInt(response.split(" ")[1]);
                StringBuilder value = new StringBuilder();

                for (int i = 0; i < valueNo; i++){
                    value.append(reader.readLine());
                    if (i < valueNo - 1){
                        value.append("\n");
                    }
                }

                String result = value.toString();

                System.out.println("Received: VALUE " + valueNo + "\n" + result);
                return result;

            } else if (response.startsWith("NOPE")){
                System.out.println("Received: NOPE");

                String keyID = stringToHex(key);

                String nearestResult = invokeNearest(keyID);
                List<FullNode.NodeData> closestNodes = new ArrayList<>();
                String[] lines = nearestResult.split("\\r?\\n");

                for (String line : lines) {
                    String[] parts = line.split(" ");
                    if (parts.length == 2) {
                        String nodeName = parts[0];
                        String nodeAddress = parts[1];
                        closestNodes.add(new FullNode.NodeData(nodeName, nodeAddress,0));
                    }
                }

                for (FullNode.NodeData node : closestNodes){
                    node.dist = calculateDistanceBetweenNodes(keyID, stringToHex(node.emailName+"\n"));
                }

                FullNode.NodeData closest = closestNodes.get(0);

                for (FullNode.NodeData node : closestNodes){
                    if (node.dist <= closest.dist && !visitedNodes.containsKey(node.emailName)){
                        closest = node;
                    }
                }

                System.out.println("Visited nodes:");
                for (String node : visitedNodes.keySet()){
                    System.out.println(node);
                    System.out.println(visitedNodes.get(node));
                }

                if (visitedNodes.containsKey(closest.emailName)){
                    System.out.println("All nodes visited");
                    return null;
                }

                System.out.println("Closest node: " + closest.emailName + " " + closest.address);

                this.terminateConnection("Received nope from node");

                if (start(closest.emailName, closest.address)) {
                    if (get(key) != null){
                        visitedNodes = new HashMap<>();
                    }
                    visitedNodes.put(this.name, this.address);
                    return get(key);
                }
                visitedNodes.put(this.name, this.address);
                return null;
            } else {
                visitedNodes.put(this.name, this.address);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error during GET operation: " + e.getMessage());
            return null;
        }
    }

    public String invokeNearest(String hexString) throws IOException {
        String[] segments = hexString.split(":");
        String ip = segments[0];
        int port = Integer.parseInt(segments[1]);

        Socket loopsocket = new Socket(ip, port);
        BufferedReader loopreader = new BufferedReader(new InputStreamReader(loopsocket.getInputStream()));
        BufferedWriter loopwriter = new BufferedWriter(new OutputStreamWriter(loopsocket.getOutputStream()));

        loopwriter.write("START 1 " + name + "\n");
        loopwriter.flush();

        loopreader.readLine();

        loopwriter.write("GET? 1\n");
        loopwriter.write(hexString + "\n"); // this is wrong? idk
        loopwriter.flush();

        String loopedResponse = loopreader.readLine();

        if (loopedResponse.startsWith("VALUE")){
            String[] valueSplitter = loopedResponse.split(":");
            return String.valueOf(valueSplitter[1]);
        } else {
            // not here, keep looking
            System.out.println("nothing here!");
        }
        return ip;
    }


    public List<FullNode.NodeData> getNearestNodes(BufferedReader lr, BufferedWriter lw, String hex) throws IOException {
        List<FullNode.NodeData> searchedNodes = new ArrayList<>();
        lw.write("NEAREST? " + hex + "\n");
        lw.flush();

        String nodesText = lr.readLine();

        int numNodes = Integer.parseInt(nodesText.split(" ")[1]);

        for (int i = 0; i < numNodes; i++) {
            String nodeName = lr.readLine();
            String nodeAddress = lr.readLine();

            FullNode.NodeData nodeData = new FullNode.NodeData(nodeName, nodeAddress,0 );
            searchedNodes.add(nodeData);
        }
        return searchedNodes;
    }


    public static String stringToHex(String input) {
        byte[] byteArray = input.getBytes();
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : byteArray) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexBuilder.append('0');
            }
            hexBuilder.append(hex);
        }
        return hexBuilder.toString();
    }

    public static int calculateDistanceBetweenNodes(String hashID1, String hashID2) {
        String binaryHashID1 = hexToBinary(hashID1);
        String binaryHashID2 = hexToBinary(hashID2);
        // string to hex, hex to binary

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

    public void terminateConnection(String reason) { // hook, line and sinker >:)
        try {
            System.out.println("REACHES HERE :)");
            writer.write("END " + reason);
            System.out.println("END " + reason);
            writer.flush();
            System.out.println("REACHES END :)");
            socket.close();
            reader.close();
            writer.close();

            System.out.println("Connection terminated: " + reason);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
