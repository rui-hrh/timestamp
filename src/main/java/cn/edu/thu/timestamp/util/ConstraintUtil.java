package cn.edu.thu.timestamp.util;

import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;

import java.util.*;

public class ConstraintUtil {
  public static boolean checkConformance(Map<String, Node> nodeMap, List<Constraint> constraintList) {
    for (Constraint constraint : constraintList) {
      if (nodeMap.containsKey(constraint.startId) && nodeMap.containsKey(constraint.endId)) {
        Node start = nodeMap.get(constraint.startId);
        Node end = nodeMap.get(constraint.endId);
        if (!checkConformance(start, end, constraint)) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean checkConformance(Node start, Node end, Constraint constraint) {
    return checkConformance(start.timestamp, end.timestamp, constraint);
  }

  public static boolean checkConformance(long start, long end, Constraint constraint) {
    long difference = end - start;
    int len = constraint.constraints.size();
    for (int i = 0; i < len; i++) {
      long[] tmp = constraint.constraints.get(i);
      if (difference >= tmp[0] && difference <= tmp[1]) {
        return true;
      }
    }
    return false;
  }

  public static boolean checkConformance(Map<String, Node> nodeMap, MinimumNetwork minimumNetwork) {
    for (String i : nodeMap.keySet()) {
      if (minimumNetwork.miniNet.containsKey(i)) {
        for (String j : nodeMap.keySet()) {
          if (j.equals(i) || !minimumNetwork.miniNet.get(i).containsKey(j)) {
            continue;
          }
          if (nodeMap.get(j).timestamp - nodeMap.get(i).timestamp > minimumNetwork.miniNet.get(i).get(j).get(0)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public static Map<String, Map<String, List<Constraint>>> buildIndex(List<Constraint> constraintList) {
    Map<String, Map<String, List<Constraint>>> constraintMap = new HashMap<>();
    for (Constraint constraint : constraintList) {
      String start = constraint.startId;
      String end = constraint.endId;
      Map<String, List<Constraint>> startConstraintMap;
      if (constraintMap.containsKey(start)) {
        startConstraintMap = constraintMap.get(start);
      } else {
        startConstraintMap = new HashMap<>();
        constraintMap.put(start, startConstraintMap);
      }
      List<Constraint> startEndConstraintList;
      if (startConstraintMap.containsKey(end)) {
        startEndConstraintList = startConstraintMap.get(end);
      } else {
        startEndConstraintList = new ArrayList<>();
        startConstraintMap.put(end, startEndConstraintList);
      }
      startEndConstraintList.add(constraint);
    }
    return constraintMap;
  }


  public static Set<String> findContain(List<Set<String>> cliqueList, String key) {
    for (Set<String> existSet : cliqueList) {
      if (existSet.contains(key)) {
        return existSet;
      }
    }
    return null;
  }
}
