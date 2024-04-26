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
    List<String> visitedNodes = new ArrayList<>();

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
            /*List<FullNode.NodeData> nodes = getNearestNodes(reader,writer,hexString);
            for (FullNode.NodeData ignored : nodes) {
                //invokeNearest(hexString);
            }*/
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
            System.out.println("Sent: GET? " + key);

            String response = reader.readLine();
            System.out.println(response);

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

                System.out.println("Received: VALUE " + valueNo + "\n");
                return result;
            } else if (response.equals("NOPE")) {
                System.out.println("RESPONSE IS " + response);
                System.out.println("KEY BEFORE CONVERSION IS " + key);
                String hexID = stringToHex(key);
                System.out.println("HEX ID IS " + hexID);
                System.out.println("HEX ID LENGTH IS " + hexID.length());
                writer.write("NEAREST? " + hexID + "\n");
                writer.flush();

                response = reader.readLine();
                System.out.println("RESPONSE AFTER READLINE IS " + response);
                if (response.startsWith("NODES")){
                    System.out.println("REACHES START OF NODES");
                    int numIter = Integer.parseInt(response.split(" ")[1]);
                    String[] names = new String[numIter];
                    String[] addrs = new String[numIter];
                    for (int i = 0; i < numIter; i++){
                        System.out.println("REACHES FOR LOOP");
                        // Reading all the node names and node addresses
                        response = reader.readLine();
                        System.out.println(response);
                        names[i] = response;

                        response = reader.readLine();
                        System.out.println(response);
                        addrs[i] = response;
                    }

                    //Check if the node has been visited
                    for (int i = 0; i < numIter; i++){
                        System.out.println("REACHES SECOND FOR LOOP");
                        if(visitedNodes.contains(names[i])){
                            return null;
                        }else{
                            // Otherwise, add to the list
                            visitedNodes.add(names[i]);
                        }

                        if(this.start(names[i], addrs[i])){
                            String value = get(key);
                            if (value != null){
                                return value;
                            }
                        }
                    }

                } // Indicate that the key was not found
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
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
