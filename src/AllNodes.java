/*
import java.util.*;

public class AllNodes {
    private static final List<Map<Integer, FullNode>> allNodes = new ArrayList<>();
    static int nodeCount = 0; // debug

    public static void addNode(FullNode node) {
        if (node.getStartingNodeName() == null) {
            // do nothing; this is a bug I don't have the time to fix
            return;
        }

        // first, check if node exists in a map at position 0 (i.e., it's already been added to the network)
        for (Map<Integer, FullNode> nodeMap : allNodes) {
            if (nodeMap.containsValue(node)) {
                System.out.println("Node with ID " + node.getHashID() + " already exists.");
                return; // if it does, exit the method
            }
        }

        // create a new hashmap for the new node
        Map<Integer, FullNode> newNodeMap = new HashMap<>();
        newNodeMap.put(0, node); // put the node with position 0
        System.out.println("Adding new node " + node.getStartingNodeName() + " with ID " + node.getHashID() + " to a new group");

        // Update distances and add existing nodes to the new node's hashmap
        for (Map<Integer, FullNode> existingNodeMap : allNodes) {
            if (!existingNodeMap.isEmpty()) {
                FullNode existingNode = existingNodeMap.get(0); // Get the first node in the existing node map
                int distance = calculateDistanceBetweenNodes(node.getHashID(), existingNode.getHashID());
                System.out.println("Distance between " + node.getHashID() + " and " + existingNode.getHashID() + " is " + distance);

                // Add existing node to the new node's hashmap
                newNodeMap.put(distance, existingNode);

                // Update existing node's hashmap
                existingNodeMap.put(distance, node);

                System.out.println("Updating existing node " + existingNode.getHashID() + " with distance " + distance);
                System.out.println("NodeMap after update: " + existingNodeMap);
            }
        }

        // Add the new node's hashmap to the list of all nodes
        allNodes.add(newNodeMap);

        // Ensure each hashmap has exactly four entries
        for (Map<Integer, FullNode> nodeMap : allNodes) {
            // Check if the size exceeds four
            while (nodeMap.size() > 4) {
                // Find and remove the entry with the furthest distance
                int maxDistance = Collections.max(nodeMap.keySet());
                nodeMap.remove(maxDistance);
                System.out.println("Removed entry with distance " + maxDistance + " from nodeMap: " + nodeMap);
            }
        }

        nodeCount++;
        System.out.println("node count is " + nodeCount);
        System.out.println("ALL NODES: " + allNodes);
    }


    public static List<FullNode> getAllNodes() {
        List<FullNode> nodes = new ArrayList<>();
        for (Map<Integer, FullNode> group : allNodes) {
            nodes.addAll(group.values());
        }
        return nodes;
    }
}

// shelving this for now; not sure if it is needed
*/
