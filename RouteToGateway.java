import java.util.*;
import java.util.stream.Collectors;

public class RouteToGateway {
    static final int INF = Integer.MAX_VALUE / 2; // Define infinity as a large number

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        int n = Integer.parseInt(sc.nextLine()); // Number of routers/nodes
        int[][] graph = new int[n][n]; // Adjacency matrix to store link costs

        // Read the graph from input
        for (int i = 0; i < n; i++) {
            String[] parts = sc.nextLine().split(" "); // Split line into weights
            for (int j = 0; j < n; j++) {
                int val = Integer.parseInt(parts[j]);
                graph[i][j] = (val == -1) ? INF : val; // Use INF for no direct link
            }
        }

        // Read gateways
        String[] gatewayParts = sc.nextLine().trim().split(" ");
        ArrayList<Integer> gateways = new ArrayList<>();
        boolean[] isGateway = new boolean[n]; // For fast lookup of gateways
        for (String g : gatewayParts) {
            int idx = Integer.parseInt(g) - 1; // Convert to 0-based index
            gateways.add(idx);
            isGateway[idx] = true; // Mark this node as a gateway
        }

        int SA = Integer.parseInt(sc.nextLine()) - 1; // Source node (SA), 0-indexed

        // Create a reversed graph to compute paths to SA
        int[][] reverse = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                reverse[j][i] = graph[i][j]; // Reverse direction of all edges

        // Gateways cannot be used as intermediate nodes to SA
        for (int g : gateways)
            Arrays.fill(reverse[g], INF); // Remove outgoing edges from gateways

        // Run Dijkstra algorithm
        Result distToSA = dijkstra(reverse, SA); // Shortest distances to SA
        Result distFromSA = dijkstra(graph, SA); // Shortest distances from SA

        // Build forwarding tables for each router (except gateways)
        for (int i = 0; i < n; i++) {
            if (isGateway[i])
                continue; // Skip gateways

            System.out.println("Forwarding Table for " + (i + 1));
            System.out.println("To\tCost\tNext Hop");

            for (int g : gateways) {
                int costToSA = (i == SA) ? 0 : distToSA.dist[i]; // Cost from i to SA
                int costFromSA = distFromSA.dist[g]; // Cost from SA to gateway g
                int totalCost = (costToSA >= INF || costFromSA >= INF) ? INF : costToSA + costFromSA;

                // If there is no valid path
                if (totalCost >= INF) {
                    System.out.println((g + 1) + "\t-1\t-1");
                    continue;
                }

                // Special case if current router is SA itself
                if (i == SA) {
                    int hop = getNextHopFromSA(SA, g, distFromSA.prev); // Find first hop from SA
                    System.out.println((g + 1) + "\t" + totalCost + "\t" + (hop == -1 ? -1 : hop + 1));
                    continue;
                }

                // For normal routers: check all neighbors to find next hops
                ArrayList<Integer> nextHops = new ArrayList<>();
                for (int v = 0; v < n; v++) {
                    if (v == i || graph[i][v] >= INF)
                        continue; // Skip self or no link
                    // If going through this neighbor is part of shortest path
                    if (!isGateway[v] && graph[i][v] + distToSA.dist[v] == distToSA.dist[i]) {
                        nextHops.add(v + 1); // +1 to match 1-indexed output
                    }
                }

                // Print result for this gateway
                if (nextHops.isEmpty())
                    System.out.println((g + 1) + "\t" + totalCost + "\t-1"); // No next hop
                else {
                    Collections.sort(nextHops); // Sort hops for consistent output
                    String hopStr = nextHops.stream().map(String::valueOf).collect(Collectors.joining(","));
                    System.out.println((g + 1) + "\t" + totalCost + "\t" + hopStr);
                }
            }
            System.out.println(); // Empty line between tables
        }

        sc.close(); // Close scanner
    }

    // Class to store Dijkstra results
    static class Result {
        int[] dist; // Distance array
        int[] prev; // Previous node array for path reconstruction

        Result(int[] d, int[] p) {
            dist = d;
            prev = p;
        }
    }

    // Dijkstra algorithm using arrays
    public static Result dijkstra(int[][] graph, int start) {
        int n = graph.length;
        int[] dist = new int[n]; // Shortest distance from start to each node
        int[] prev = new int[n]; // Previous node to reconstruct path
        boolean[] visited = new boolean[n]; // Track visited nodes

        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);
        dist[start] = 0; // Distance to self is 0

        // Repeat for each node
        for (int count = 0; count < n; count++) {
            int u = -1;
            // Find unvisited node with smallest distance
            for (int i = 0; i < n; i++)
                if (!visited[i] && (u == -1 || dist[i] < dist[u]))
                    u = i;
            if (u == -1 || dist[u] >= INF)
                break; // All reachable nodes processed
            visited[u] = true;

            // Update distances of neighbors
            for (int v = 0; v < n; v++) {
                if (!visited[v] && graph[u][v] < INF) {
                    int newDist = dist[u] + graph[u][v];
                    if (newDist < dist[v]) {
                        dist[v] = newDist;
                        prev[v] = u; // Record path
                    }
                }
            }
        }

        return new Result(dist, prev);
    }

    // Find the first hop from SA to reach gateway g
    public static int getNextHopFromSA(int SA, int g, int[] prevFromSA) {
        int cur = g;
        while (prevFromSA[cur] != -1 && prevFromSA[cur] != SA)
            cur = prevFromSA[cur]; // Go backward until reaching SA
        return (prevFromSA[cur] == SA) ? cur : -1; // Return first hop or -1 if none
    }
}
