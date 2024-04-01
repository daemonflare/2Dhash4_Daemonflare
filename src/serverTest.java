public class serverTest {
    public static void main(String[] args) {
        String nodeNameBase = "arber.isufi@city.ac.uk:test-node"; // customize this to whatever you want
        int startingPort = 2512; // this too

        for (int i = 1; i <= 8; i++) { // change second parameter to however many fullnodes you want to create and run
            String nodeName = nodeNameBase + i;
            int port = startingPort + i - 1;
            FullNode node = new FullNode();
            node.setStartingNodeName(nodeName);
            startServerThread(node, "127.0.0.1", port);
        }
    }

    private static void startServerThread(FullNode node, String ipAddress, int port) {
        Thread serverThread = new Thread(() -> {
            if (node.listen(ipAddress, port)) {
                node.handleIncomingConnections(node.getStartingNodeName(), ipAddress + ":" + port);
            }
        });
        serverThread.start();
    }
}
