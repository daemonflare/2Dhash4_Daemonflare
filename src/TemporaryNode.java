// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Arber Isufi
// 210016038
// arber.isufi@city.ac.uk

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    Socket trioSocket;
    BufferedReader trioReader;
    BufferedWriter trioWriter;
    List<String> visitedNodes = new ArrayList<>();

    public boolean start(String startingNodeName, String startingNodeAddress) {
        this.name = startingNodeName;
        this.address = startingNodeAddress;
        try {
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
            int storeKeyNo = key.split("\n").length;
            int storeValueNo = value.split("\n").length;
            writer.write("PUT? " + storeKeyNo + " " + storeValueNo + "\n");
            writer.write(key);
            writer.write(value);
            writer.flush();

            System.out.println("Sent key " + key);
            System.out.println("Sent value " + value);

            String response = reader.readLine();
            System.out.println(response);

            if (response.startsWith("SUCCESS")) {
                System.out.println("Store successful!");
            } else if (response.startsWith("FAILED")) {
                String hexID = stringToHex(key);
                writer.write("NEAREST? " + hexID + "\n");
                writer.flush();
                response = reader.readLine();
                if (response.startsWith("NODES")) {
                    int numIter = Integer.parseInt(response.split(" ")[1]);
                    String[] names = new String[numIter];
                    String[] addrs = new String[numIter];
                    for (int i = 0; i < numIter; i++) {
                        response = reader.readLine();
                        System.out.println(response);
                        names[i] = response;

                        response = reader.readLine();
                        System.out.println(response);
                        addrs[i] = response;

                        boolean success = startAsTrios(names[i], addrs[i], key, value);
                        if (success) {
                            System.out.println("Started node in trios: " + names[i]);
                        } else {
                            System.out.println("Failed to start node in trio: " + names[i]);
                        }
                    }
                } else {
                    System.out.println("NODES condition not satisfied!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    public boolean startAsTrios(String trioName, String trioAddress, String key, String value) { // used to send start for the 3 nearest after initial store
        this.name = trioName;
        this.address = trioAddress;
        try {
            String[] segments = trioAddress.split(":");
            String ip = segments[0];
            int port = Integer.parseInt(segments[1]);

            trioSocket = new Socket(ip, port);
            trioReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            trioWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


            System.out.println("Passed key is " + key);
            System.out.println("Passed value is " + value);
            trioWriter.write("START 1 " + name + "\n");
            trioWriter.flush();

            System.out.println(trioReader.readLine());

            int trioKey = key.split("\n").length;
            int trioVal = value.split("\n").length;

            writer.write("PUT? " + trioKey + " " + trioVal + "\n");
            writer.write(key);
            writer.write(value);
            writer.flush();

            System.out.println("Sent key " + key);
            System.out.println("Sent value " + value);

            String response = reader.readLine();
            System.out.println(response);

            return true;
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String get(String key) {
        try {
            if (!key.endsWith("\n")) {
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

                for (int i = 0; i < valueNo; i++) {
                    value.append(reader.readLine());
                    if (i < valueNo - 1) {
                        value.append("\n");
                    }
                }

                String result = value.toString();

                System.out.println("Received: VALUE " + valueNo + "\n");
                return result;
            } else if (response.equals("NOPE")) {
                String hexID = stringToHex(key);
                System.out.println("Hex ID to check is " + hexID);
                writer.write("NEAREST? " + hexID + "\n");
                writer.flush();

                response = reader.readLine();
                if (response.startsWith("NODES")) {
                    int numIter = Integer.parseInt(response.split(" ")[1]);
                    String[] names = new String[numIter];
                    String[] addrs = new String[numIter];
                    for (int i = 0; i < numIter; i++) {
                        response = reader.readLine();
                        System.out.println(response);
                        names[i] = response;

                        response = reader.readLine();
                        System.out.println(response);
                        addrs[i] = response;
                    }

                    for (int i = 0; i < numIter; i++) {
                        if (visitedNodes.contains(names[i])) {
                            return null;
                        } else {
                            visitedNodes.add(names[i]);
                        }

                        if (this.start(names[i], addrs[i])) {
                            String value = get(key);
                            if (value != null) {
                                return value;
                            }
                        }
                    }
                }
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return key;
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

    public void terminateConnection(String reason) {
        try {
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
