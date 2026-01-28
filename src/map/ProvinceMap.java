package map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ProvinceMap {
    private final List<Province> provinces;
    private final List<List<Integer>> adjacencies;
    private final List<Integer> cityProvinces = new ArrayList<>();

    public ProvinceMap(List<Province> provinces, List<List<Integer>> adjacencies) {
        this.provinces = provinces;
        this.adjacencies = adjacencies;
    }

    public List<Province> getProvinces() {
        return provinces;
    }

    public List<Integer> getAdjacentProvinces(int provinceIndex) {
        return adjacencies.get(provinceIndex);
    }

    // Clear map, replace terrain with open terrain type (green)
    public void fillWithEmpty() {
        for (Province p : provinces) {
            p.setTerrainType(Province.TerrainType.OPEN);
        }
    }

    // Add blobs of forest terrain (darker green)
    public void populateForests() {
        Random random = new Random();
        List<Integer> seeds = new ArrayList<>();

        for (int i = 0; i < 50 && i < provinces.size(); i++) {
            int randomIndex;
            do {
                randomIndex = random.nextInt(provinces.size());
            } while (seeds.contains(randomIndex));

            seeds.add(randomIndex);
            provinces.get(randomIndex).setTerrainType(Province.TerrainType.FOREST);
        }

        int n = seeds.size();
        for (int i = 0; i < n; i++) {
            List<Integer> adjacentIndices = getAdjacentProvinces(seeds.get(i));
            int randomIndex = random.nextInt(adjacentIndices.size());
            adjacentIndices = getAdjacentProvinces(adjacentIndices.get(randomIndex));
            randomIndex = random.nextInt(adjacentIndices.size());
            seeds.add(adjacentIndices.get(randomIndex));
        }

        for (int seedIdx : seeds) {
            List<Integer> adjacentIndices = getAdjacentProvinces(seedIdx);
            provinces.get(seedIdx).setTerrainType(Province.TerrainType.FOREST);
            for (int adjacentIndex : adjacentIndices) {
                if (Math.random() < 0.4f) {
                    provinces.get(adjacentIndex).setTerrainType(Province.TerrainType.FOREST);
                }
            }
        }
    }

    // Count how many river neighbours a province has
    private int countRiverNeighbours(int provinceIdx, Set<Integer> newRivers) {
        Set<Integer> unique = new HashSet<>(); // Neighbours are not necessarily unique in the adjlist
        for (int p : adjacencies.get(provinceIdx)) {
            if (provinces.get(p).getTerrainType() == Province.TerrainType.RIVER || newRivers.contains(p)) {
                unique.add(p);
            }
        }
        return unique.size();
    }

    // Check if river can fork here (any valid adjacent province to continue river)
    public boolean isForkableProvince(int provinceIdx,  Set<Integer> newRivers) {
        for(int n : adjacencies.get(provinceIdx)) {
            if(countRiverNeighbours(n, newRivers) <= 1) {
                return true;
            }
        }
        return false;
    }


    // Generates a random river path from a start to map edge, returns null if no valid path found after retries
    private List<Integer> generateRiverPath(int start, Set<Integer> existingRivers, int minLength, int maxRetries) {
        Random random = new Random();
        for (int retry = 0; retry < maxRetries; retry++) {
            Set<Integer> visited = new HashSet<>(existingRivers);  // Avoid existing rivers except start
            List<Integer> path = new ArrayList<>();
            path.add(start);
            visited.add(start);
            int current = start;
            while (path.size() < 1000) {  // Safety limit to prevent infinite loops
                List<Integer> neighbours = getAdjacentProvinces(current).stream()
                        .filter(n -> !visited.contains(n) // Avoid revisits
                                && countRiverNeighbours(n, visited) < 2)  // Avoid adjacency to other parts of river
                        .toList();
                if (neighbours.isEmpty()) {
                    break;
                }
                int next = neighbours.get(random.nextInt(neighbours.size()));
                path.add(next);
                visited.add(next);
                current = next;
                // Stop if conditions met: long enough, at edge, and not start
                if (path.size() >= minLength && provinces.get(current).isEdge() && current != start) {
                    return path;
                }
            }
            // Check if valid end state
            if (path.size() >= minLength && provinces.get((path.get(path.size() - 1))).isEdge() && path.get(path.size() - 1) != start) {
                return path;
            }
        }
        return null;  // Failed after retries
    }

    // Generate rivers with paths and forks
    private void generateRivers() {
        Random random = new Random();
        List<Integer> edgeProvinces = new ArrayList<>();
        for (int i = 0; i < provinces.size(); i++) {
            if (provinces.get(i).isEdge()) {
                edgeProvinces.add(i);
            }
        }
        if (edgeProvinces.size() < 2) {
            System.out.println("Not enough edge provinces to generate a river.");  // Debug log
            return;  // Skip if map is too small
        }

        // Select random start for main river
        int start = edgeProvinces.get(random.nextInt(edgeProvinces.size()));
        Set<Integer> allRivers = new HashSet<>();

        // Generate main river path (min 10 long)
        List<Integer> mainPath = generateRiverPath(start, allRivers, 10, 10);  // 10 retries
        if (mainPath == null) {
            System.out.println("Could not generate main river path after retries.");  // Debug log
            return;
        }
        for (int idx : mainPath) {
            provinces.get(idx).setTerrainType(Province.TerrainType.RIVER);
            allRivers.add(idx);
        }

        // Add 1-2 forks from main path
        int numForks = random.nextInt(2) + 1;  // 1 or 2 forks
        for (int i = 0; i < numForks; i++) {
            List<Integer> forkProvinces = new ArrayList<>();
            // Check for possible fork points, excluding first and last 3 provinces to give space
            for (int j = 3; j < mainPath.size() - 4; j++) {
                if (isForkableProvince(mainPath.get(j), allRivers)) {
                    forkProvinces.add(mainPath.get(j));
                }
            }
            if (forkProvinces.isEmpty()) {
                System.out.println("Not enough river fork locations found.");  // Debug log
                break;
            }

            // Try up to 10 times to build a path from a random fork position
            for (int retries = 0; retries < 10; retries++) {
                // Pick random fork point from filtered list
                int forkProvince = forkProvinces.get(random.nextInt(forkProvinces.size()));
                List<Integer> branch = generateRiverPath(forkProvince, allRivers, 10, 5);
                if (branch != null && branch.size() > 1) {  // branch[0] is fork province, so >1 for new provinces
                    for (int j = 1; j < branch.size(); j++) {
                        int idx = branch.get(j);
                        provinces.get(idx).setTerrainType(Province.TerrainType.RIVER);
                        allRivers.add(idx);
                    }
                    break; // Success
                }
            }
        }
    }

    // Computes the shortest path using Dijkstra's algorithm, considering terrain costs and roads
    public List<Province> findShortestPath(int startIndex, int endIndex) {
        if (startIndex < 0 || startIndex >= provinces.size() || endIndex < 0 || endIndex >= provinces.size()) {
            return Collections.emptyList();  // Invalid indices
        }

        int n = provinces.size();
        double[] distances = new double[n];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        distances[startIndex] = 0;

        int[] previous = new int[n];
        Arrays.fill(previous, -1);

        // Priority queue: {provinceIndex, distance}
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[1], b[1]));
        pq.add(new double[]{startIndex, 0});

        while (!pq.isEmpty()) {
            double[] current = pq.poll();
            int currIdx = (int) current[0];
            double currDist = current[1];

            if (currDist > distances[currIdx]) continue;

            for (int neighbourIdx : getAdjacentProvinces(currIdx)) {
                // Cost based on neighbour's terrain type and road status
                Province neighbour = provinces.get(neighbourIdx);
                double cost = neighbour.getTerrainType().getMovementCost();
                if (neighbour.hasRoad()) {
                    cost /= 2;  // Halve cost if neighbour has a road
                }
                double newDist = currDist + cost;
                if (newDist < distances[neighbourIdx]) {
                    distances[neighbourIdx] = newDist;
                    previous[neighbourIdx] = currIdx;
                    pq.add(new double[]{neighbourIdx, newDist});
                }
            }
        }

        // Reconstruct path
        List<Province> path = new ArrayList<>();
        int current = endIndex;
        while (current != -1) {
            path.add(provinces.get(current));
            current = previous[current];
        }
        if (path.size() == 1 && path.get(0) != provinces.get(startIndex)) {
            return Collections.emptyList();  // No path found
        }
        Collections.reverse(path);
        return path;
    }

    // Create a city at the specified province index
    public void createCity(int provinceIndex) {
        if (provinceIndex >= 0 && provinceIndex < provinces.size()) {
            Province province = provinces.get(provinceIndex);
            province.setTerrainType(Province.TerrainType.CITY);

            if (!cityProvinces.contains(provinceIndex)) {
                cityProvinces.add(provinceIndex);
            }
        }
    }

    public List<Integer> getCityProvinces() {
        return cityProvinces;
    }

    // Adds a city, computes path to another city, and places roads
    public void addConnectedCity() {
        List<Province> provinces = getProvinces();
        if (provinces.isEmpty()) return;

        Random random = new Random();

        // Select a random province (of those which have appropriate terrain) and convert to city
        int newCityIdx;
        do {
            newCityIdx = random.nextInt(provinces.size());
        } while (!provinces.get(newCityIdx).getTerrainType().canPlaceCityOn());
        createCity(newCityIdx);

        List<Integer> cities = getCityProvinces();
        // Don't make a road if this is the first city
        if (cities.size() > 1) {
            // Pick a random existing city (not the last one, which we just created)
            int targetIdx = cities.get(random.nextInt(cities.size() - 1));

            List<Province> path = findShortestPath(newCityIdx, targetIdx);

            // Place roads
            if (!path.isEmpty()) {
                for (Province p : path) {
                    p.setHasRoad(true);
                }
            }
        }

    }

    // Generate the base map
    public void populate() {
        fillWithEmpty();
        populateForests();
        generateRivers();  // Generate rivers after forests to avoid breaking river paths
    }

    // Calculate distance from each province to the nearest city (for mode 2)
    public int[] calculateDistancesToCities() {
        int[] distances = new int[provinces.size()];
        Arrays.fill(distances, Integer.MAX_VALUE);
        
        // Initialize distances for cities
        for (int cityIdx : cityProvinces) {
            distances[cityIdx] = 0;
        }
        
        // BFS to calculate distances
        List<Integer> queue = new ArrayList<>(cityProvinces);
        Set<Integer> visited = new HashSet<>(cityProvinces);
        
        while (!queue.isEmpty()) {
            int current = queue.remove(0);
            int currentDist = distances[current];
            
            for (int neighbor : getAdjacentProvinces(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    distances[neighbor] = currentDist + 1;
                    queue.add(neighbor);
                }
            }
        }
        
        return distances;
    }

    // Calculate rating for each province (for mode 3)
    public int[] calculateRatings() {
        int[] ratings = new int[provinces.size()];
        
        for (int i = 0; i < provinces.size(); i++) {
            int rating = 0;
            Province province = provinces.get(i);
            
            // Check adjacency to cities
            boolean adjacentToCity = false;
            boolean oneProvinceAwayFromCity = false;
            for (int neighbor : getAdjacentProvinces(i)) {
                if (provinces.get(neighbor).getTerrainType() == Province.TerrainType.CITY) {
                    adjacentToCity = true;
                    break;
                }
                // Check if neighbor is adjacent to a city (one province separated)
                for (int neighborOfNeighbor : getAdjacentProvinces(neighbor)) {
                    if (provinces.get(neighborOfNeighbor).getTerrainType() == Province.TerrainType.CITY) {
                        oneProvinceAwayFromCity = true;
                        break;
                    }
                }
                if (oneProvinceAwayFromCity) break;
            }
            
            if (adjacentToCity) {
                rating -= 10;
            } else if (oneProvinceAwayFromCity) {
                rating -= 5;
            }
            
            // Check adjacency to rivers
            for (int neighbor : getAdjacentProvinces(i)) {
                if (provinces.get(neighbor).getTerrainType() == Province.TerrainType.RIVER) {
                    rating += 3;
                    break;  // Only count once
                }
            }
            
            // Check if on road
            if (province.hasRoad()) {
                rating += 3;
            }
            
            // Check forest
            if (province.getTerrainType() == Province.TerrainType.FOREST) {
                rating -= 5;
            } else {
                // Check if adjacent to any forest
                for (int neighbor : getAdjacentProvinces(i)) {
                    if (provinces.get(neighbor).getTerrainType() == Province.TerrainType.FOREST) {
                        rating += 1;
                        break;  // Only count once
                    }
                }
            }
            
            ratings[i] = rating;
        }
        
        return ratings;
    }
}