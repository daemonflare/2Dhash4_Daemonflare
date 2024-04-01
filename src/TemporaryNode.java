// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Arber Isufi
// 210016038
// arber.isufi@city.ac.uk

import java.io.*;
import java.net.Socket;

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

            return true;
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean store(String key, String value) {
        try {
            int numKeys = 1; // dirty fix to limiting it to 1
            int numValues = 1;

            // Send the PUT request with the correct number of keys and values
            writer.write("PUT? " + numKeys + " " + numValues + "\n");
            writer.write(key + "\n" + value + "\n");
            writer.flush();

            // Log the PUT request sent
            System.out.println("PUT request sent: PUT? " + numKeys + " " + numValues);
            System.out.println("Key: " + key);
            System.out.println("Value: " + value);

            return true;
        } catch (Exception e) {
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

            if (response.startsWith("VALUE")) {
                int numLines = Integer.parseInt(response.split(" ")[1]);
                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 0; i < numLines; i++) {
                    valueBuilder.append(reader.readLine()).append("\n");
                }
                return valueBuilder.toString().trim();
            } else if (response.equals("NOPE")) {
                System.out.println("nothing corresponds to " + key);
                return null;
            } else {
                System.out.println("invalid: " + response);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void echo() {
        try {
            writer.write("ECHO?\n");
            writer.flush();

            // Read the response from the server
            String response = reader.readLine();

            if (response != null && response.equals("OHCE")) {
                System.out.println("Connection is active: OHCE");
            } else {
                System.out.println("Unexpected response: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
