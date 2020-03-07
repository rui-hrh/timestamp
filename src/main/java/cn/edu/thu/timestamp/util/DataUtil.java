package cn.edu.thu.timestamp.util;

import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.Node;

import java.util.*;

public class DataUtil {
  public static Map<String, Node> deepCopy(Map<String, Node> nodeMap) {
    Map<String, Node> result = new HashMap<>();
    for (String key : nodeMap.keySet()) {
      Node value = nodeMap.get(key);
      Node newNode = new Node(value.id, value.timestamp, value.value);
      result.put(key, newNode);
    }
    return result;
  }


  public static long calCost(Map<String, Node> origin, Map<String, Node> repair) {
    long delta = 0;
    for (String key : repair.keySet()) {
      delta += Math.abs(origin.get(key).timestamp - repair.get(key).timestamp);
    }
    return delta;
  }


  public static double calAccuracy(Map<String, Node> truth, Map<String, Node> fault, Map<String, Node> repair) {
    double error = 0;
    double cost = 0;
    double inject = 0;
    for (String key : truth.keySet()) {
      error += Math.abs(truth.get(key).timestamp - repair.get(key).timestamp);
      cost += Math.abs(fault.get(key).timestamp - repair.get(key).timestamp);
      inject += Math.abs(truth.get(key).timestamp - fault.get(key).timestamp);
    }
    if (error == 0) {
      return 1;
    }
    return (1 - (error / (cost + inject)));
  }


  public static List<Set<String>> getCliqueList(Map<String, Node> fault, List<Constraint> constraintList) {
    List<Set<String>> cliqueList = new ArrayList<>();
    for (Constraint constraint : constraintList) {
      String start = constraint.startId;
      String end = constraint.endId;
      if (!fault.keySet().contains(start) || !fault.keySet().contains(end)) {
        continue;
      }
      if (cliqueList.size() == 0) {
        Set<String> cliqueSet = new HashSet<>();
        cliqueSet.add(start);
        cliqueSet.add(end);
        cliqueList.add(cliqueSet);
      } else {
        Set<String> startSet = ConstraintUtil.findContain(cliqueList, start);
        Set<String> endSet = ConstraintUtil.findContain(cliqueList, end);
        if (startSet == null && endSet == null) {
          Set<String> cliqueSet = new HashSet<>();
          cliqueSet.add(start);
          cliqueSet.add(end);
          cliqueList.add(cliqueSet);
        } else if (startSet == null) {
          endSet.add(start);
        } else if (endSet == null) {
          startSet.add(end);
        } else {
          if (startSet != endSet) {
            startSet.addAll(endSet);
            cliqueList.remove(endSet);
          }
        }
      }
    }
    return cliqueList;
  }
}
