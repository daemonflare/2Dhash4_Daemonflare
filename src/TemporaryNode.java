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
import java.util.List;

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

    public boolean start(String startingNodeName, String startingNodeAddress) {
        this.name = startingNodeName;
        this.address = startingNodeAddress;
        try {
            // socket splicing so that it doesn't get angry over string/proxy conflicts:
            String[] segments = startingNodeAddress.split(":");
            String ip = segments[0];
            int port = Integer.parseInt(segments[1]);

            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

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
            writer.write("GET? 1\n");
            writer.write(key + "\n");
            writer.flush();

            String response = reader.readLine();

            System.out.println(response);
            if (response.startsWith("VALUE")) {
                int numLines = Integer.parseInt(response.split(" ")[1]);
                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 0; i < numLines; i++) {
                    valueBuilder.append(reader.readLine()).append("\n");
                }
                return valueBuilder.toString().trim();
            } else if (response.equals("NOPE")) {
                System.out.println("nothing corresponds to " + key);
                List<FullNode.NodeData> nodes = getNearestNodes(reader,writer);
                String hexString = stringToHex(key);
                for (FullNode.NodeData ignored : nodes) {
                    invokeNearest(hexString);
                }
                return null;
            } else {
                System.out.println("invalid: " + response);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        writer.flush();

        loopreader.readLine();

        loopwriter.write("GET 1 " + "\n");
        writer.flush();

        String loopedResponse = reader.readLine();

        if (loopedResponse.startsWith("VALUE")){
            String[] valueSplitter = hexString.split(":");
            return String.valueOf(valueSplitter[1]);
        } else {
            // go to next until its finished
        }
        return ip;
    }

    public List<FullNode.NodeData> getNearestNodes(BufferedReader lr, BufferedWriter lw) throws IOException {
        List<FullNode.NodeData> searchedNodes = new ArrayList<>();
        lw.write("NEAREST?");
        lw.flush();

        String nodesText = lr.readLine();

        if (nodesText.startsWith("NODES")) {
            int numNodes = Integer.parseInt(nodesText.split(" ")[1]);

            for (int i = 0; i < numNodes; i++) {
                String nodeName = lr.readLine();
                String nodeAddress = lr.readLine();

                FullNode.NodeData nodeData = new FullNode.NodeData(nodeName,nodeAddress);
                searchedNodes.add(nodeData);
            }
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
            writer.write("END " + reason);
            writer.flush();
            socket.close();
            reader.close();
            writer.close();

            System.out.println("Connection terminated: " + reason);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
