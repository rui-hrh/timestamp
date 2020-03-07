package cn.edu.thu.timestamp.common;

import java.util.*;

public class MinimumNetwork {
  public Map<String, Map<String, List<Long>>> miniNet;

  public MinimumNetwork(List<Constraint> constraintList) {
    Set<String> allNodeSet = new HashSet<String>();
    for (Constraint constraint : constraintList) {
      allNodeSet.add(constraint.startId);
      allNodeSet.add(constraint.endId);
    }
    Map<String, Integer> assignMap = new HashMap<String, Integer>();
    int index = 0;
    for (String nodeKey : allNodeSet) {
      assignMap.put(nodeKey, index);
      index++;
    }
    int n = allNodeSet.size();
    Long[][] distance = new Long[n][n];
    initDistance(distance, constraintList, assignMap);
    calAllPairShortestPath(n, distance);
    miniNet = new HashMap<>();
    transMinimumNetwork(distance, assignMap, allNodeSet);
  }

  private void transMinimumNetwork(Long[][] distance, Map<String, Integer> assignMap, Set<String> allNodeSet) {
    for (String nodeKey1 : allNodeSet) {
      int startIndex = assignMap.get(nodeKey1);
      Map<String, List<Long>> tmpMap = new HashMap<String, List<Long>>();
      miniNet.put(nodeKey1, tmpMap);
      for (String nodeKey2 : allNodeSet) {
        int endIndex = assignMap.get(nodeKey2);
        if (distance[startIndex][endIndex] != null) {
          List<Long> distanceList = new ArrayList<Long>();
          distanceList.add(distance[startIndex][endIndex]);
          tmpMap.put(nodeKey2, distanceList);
        }
      }
    }
  }

  private void initDistance(Long[][] distance, List<Constraint> constraintList, Map<String, Integer> assignMap) {
    for (Constraint constraint : constraintList) {
      int startIndex = assignMap.get(constraint.startId);
      int endIndex = assignMap.get(constraint.endId);
      distance[startIndex][endIndex] = constraint.constraints.get(0)[1];
      if (constraint.constraints.get(0)[0] != Long.MIN_VALUE) {
        distance[endIndex][startIndex] = -constraint.constraints.get(0)[0];
      }
    }
  }

  private void calAllPairShortestPath(int n, Long[][] distance) {
    for (int k = 0; k < n; k++) {
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
          if (i != j && distance[i][k] != null && distance[k][j] != null) {
            if (distance[i][j] == null || distance[i][j] > distance[i][k] + distance[k][j]) {
              distance[i][j] = distance[i][k] + distance[k][j];
            }
          }
        }
      }
    }
  }
}
