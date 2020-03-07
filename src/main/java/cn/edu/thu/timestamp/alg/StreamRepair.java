package cn.edu.thu.timestamp.alg;

import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;
import cn.edu.thu.timestamp.util.ConstraintUtil;
import cn.edu.thu.timestamp.util.DataUtil;

import java.util.*;

public class StreamRepair {
  private long minCost = Long.MAX_VALUE;
  private Set<String> visited = new HashSet<>();
  private Map<String, Node> minRepair = null;

  public Map<String, Node> repairStreaming(Map<String, Node> fault, List<String> order, boolean heuristic,
                                           List<Constraint> constraintList, MinimumNetwork minimumNetwork,
                                           int windowSize, int stepSize, int chainMaxLength) {
    if (stepSize > windowSize) {
      stepSize = windowSize;
    }
    Map<String, Node> result = new HashMap<>();
    Map<String, Node> curWindow = new HashMap<>();
    Map<String, Node> curRepair = new HashMap<>();

    String[] keyArray = new String[order.size()];
    order.toArray(keyArray);
    Map<String, Set<Long>> candidateMap = new HashMap<>();
    for (int i = 0; i < order.size(); i += stepSize) {
      int curEnd = i + stepSize < order.size() ? i + stepSize : order.size();
      for (int j = i; j < curEnd; j++) {
        String curKey = order.get(j);
        Node curNode = fault.get(curKey);
        curWindow.put(curKey, curNode);
        if (curWindow.size() > windowSize) {
          String removeKey = order.get(j - windowSize);
          curWindow.remove(removeKey);
          curRepair.remove(removeKey);
        }
      }
      candidateMap = updateCandidate(minimumNetwork, curWindow, curRepair, chainMaxLength, candidateMap);

      iterRepairStreaming(i, curEnd, curRepair, curWindow, candidateMap, minimumNetwork, keyArray, heuristic);

      if (minRepair == null) {
        for (int j = i; j < curEnd; j++) {
          String curKey = order.get(j);
          Node curNode = new Node(curKey, fault.get(curKey).timestamp);
          curRepair.put(curKey, curNode);
        }
      } else {
        curRepair = minRepair;
      }
      for (int j = i; j < curEnd; j++) {
        String curKey = order.get(j);
        Node curNode = curRepair.get(curKey);
        result.put(curKey, curNode);
      }
    }
    return result;
  }

  private Map<String, Node> iterRepairStreaming(int curBegin, int curEnd, Map<String, Node> curRepair,
                                                Map<String, Node> curWindow,
                                                Map<String, Set<Long>> candidateMap, MinimumNetwork minimumNetwork,
                                                String[] keyArray, boolean heuristic) {
    Map<String, Set<Long>> repairCandidateMap = new HashMap<>();
    for (String key : curWindow.keySet()) {
      if (curRepair.containsKey(key)) {
        Set<Long> newCanSet = new HashSet<>();
        newCanSet.add(curRepair.get(key).timestamp);
        repairCandidateMap.put(key, newCanSet);
      } else {
        Set<Long> newCanSet = new HashSet<>(candidateMap.get(key));
        repairCandidateMap.put(key, newCanSet);
      }
    }
    return repair(minimumNetwork, repairCandidateMap, curWindow, curRepair, Long.MAX_VALUE, keyArray, curBegin, curEnd, heuristic);
  }


  private Map<String, Set<Long>> updateCandidate(MinimumNetwork minimumNetwork, Map<String, Node> curWindow,
                                                 Map<String, Node> curRepair, int chainMaxLength,
                                                 Map<String, Set<Long>> lastCandidate) {
    Map<String, Set<Long>> newCandidate = new HashMap<>();
    Set<String> oldNode = new HashSet<>();
    Map<String, Node> lastTupe = new HashMap<>();
    Map<String, Node> newTupe = new HashMap<>();
    for (String key : curWindow.keySet()) {
      if (lastCandidate.containsKey(key)) {
        newCandidate.put(key, lastCandidate.get(key));
        lastTupe.put(key, curRepair.get(key));
        oldNode.add(key);
      } else {
        newTupe.put(key, curWindow.get(key));
      }
    }
    minCost = Long.MAX_VALUE;
    minRepair = null;
    visited.clear();
    newCandidate.putAll(generateCandidate(minimumNetwork, newTupe, Long.MAX_VALUE, chainMaxLength));
    minCost = Long.MAX_VALUE;
    minRepair = null;
    visited.clear();
    for (String curNode : oldNode) {
      for (long curCandidate : newCandidate.get(curNode)) {
        Map<String, Node> t = DataUtil.deepCopy(lastTupe);
        t.get(curNode).timestamp = curCandidate;
        generate(curWindow, curNode, t, minimumNetwork, true, newCandidate, minCost, t.size() + chainMaxLength);
        generate(curWindow, curNode, t, minimumNetwork, false, newCandidate, minCost, t.size() + chainMaxLength);
      }
    }
    return newCandidate;
  }


  private Map<String, Set<Long>> generateCandidate(MinimumNetwork minimumNetwork, Map<String, Node> origin,
                                                   long curMinCost, int chainMaxLength) {
    Map<String, Set<Long>> candidateMap = new HashMap<>();
    for (String key : origin.keySet()) {
      Set<Long> candidateSet = new HashSet<>();
      candidateSet.add(origin.get(key).timestamp);
      candidateMap.put(key, candidateSet);
    }

    for (String key : origin.keySet()) {
      Map<String, Node> t = new HashMap<>();
      t.put(key, new Node(key, origin.get(key).timestamp));
      generate(origin, key, t, minimumNetwork, true, candidateMap, curMinCost, chainMaxLength);
      generate(origin, key, t, minimumNetwork, false, candidateMap, curMinCost, chainMaxLength);
    }
    return candidateMap;
  }

  private void generate(Map<String, Node> origin, String curNode, Map<String, Node> t, MinimumNetwork minimumNetwork,
                        boolean direction, Map<String, Set<Long>> candidateMap, long curMinCost, int chainMaxLength) {
    long cost = DataUtil.calCost(origin, t);
    curMinCost = curMinCost < minCost ? curMinCost : minCost;
    if (cost > curMinCost || t.size() > chainMaxLength || !ConstraintUtil.checkConformance(t, minimumNetwork)
        || checkVisited(origin, t, visited, curNode, direction)) {
      return;
    }
    if (t.size() == origin.size()) {
      minCost = cost;
      minRepair = t;
      return;
    }
    Set<String> remainder = new HashSet<String>(origin.keySet());
    remainder.removeAll(t.keySet());
    for (String nextNode : remainder) {
      if (direction) {
        if (minimumNetwork.miniNet.containsKey(curNode) && minimumNetwork.miniNet.get(curNode).containsKey(nextNode)) {
          for (long distance : minimumNetwork.miniNet.get(curNode).get(nextNode)) {
            Map<String, Node> newT = DataUtil.deepCopy(t);
            long timeCandidate = t.get(curNode).timestamp + distance;
            newT.put(nextNode, new Node(nextNode, timeCandidate));
            candidateMap.get(nextNode).add(timeCandidate);
            generate(origin, nextNode, newT, minimumNetwork, false, candidateMap, curMinCost, chainMaxLength);
          }
        }
      } else {
        if (minimumNetwork.miniNet.containsKey(nextNode) && minimumNetwork.miniNet.get(nextNode).containsKey(curNode)) {
          for (long distance : minimumNetwork.miniNet.get(nextNode).get(curNode)) {
            Map<String, Node> newT = DataUtil.deepCopy(t);
            long timeCandidate = t.get(curNode).timestamp - distance;
            newT.put(nextNode, new Node(nextNode, timeCandidate));
            candidateMap.get(nextNode).add(timeCandidate);
            generate(origin, nextNode, newT, minimumNetwork, true, candidateMap, curMinCost, chainMaxLength);
          }
        }
      }
    }
  }


  private boolean checkVisited(Map<String, Node> origin, Map<String, Node> t, Set<String> visited,
                               String tail, boolean direction) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(tail);
    if (direction) {
      stringBuilder.append("out");
    } else {
      stringBuilder.append("in");
    }
    stringBuilder.append(",");
    for (String key : origin.keySet()) {
      stringBuilder.append(key);
      stringBuilder.append("=");
      if (t.containsKey(key)) {
        stringBuilder.append(t.get(key).timestamp);
      }
      stringBuilder.append(",");
    }
    String cur = stringBuilder.toString();
    if (visited.contains(cur)) {
      return true;
    } else {
      visited.add(cur);
      return false;
    }
  }


  private Map<String, Node> repair(MinimumNetwork minimumNetwork,
                                   Map<String, Set<Long>> candidateMap,
                                   Map<String, Node> origin, Map<String, Node> tuple, long curMinCost,
                                   String[] keyArray, int k, int n,
                                   boolean heuristic) {
    candidateMap = prune(minimumNetwork, candidateMap, origin, curMinCost);
    Map<String, Node> result = null;

    if (k >= n) {
      return tuple;
    }
    curMinCost = curMinCost < minCost ? curMinCost : minCost;

    String key = keyArray[k];
    for (long candidate : candidateMap.get(key)) {
      Map<String, Node> tupleNew = DataUtil.deepCopy(tuple);
      tupleNew.put(key, new Node(key, candidate));
      long cost = DataUtil.calCost(origin, tupleNew);
      if (cost > curMinCost || !ConstraintUtil.checkConformance(tupleNew, minimumNetwork)) {
        continue;
      }
      Map<String, Set<Long>> newCandidateMap = new HashMap<>();
      for (String newCanKey : candidateMap.keySet()) {
        Set<Long> newCanSet;
        if (!newCanKey.equals(key)) {
          newCanSet = new HashSet<>(candidateMap.get(newCanKey));
        } else {
          newCanSet = new HashSet<>();
          newCanSet.add(candidate);
        }
        newCandidateMap.put(newCanKey, newCanSet);
      }
      result = repair(minimumNetwork, newCandidateMap, origin, tupleNew, curMinCost, keyArray, k + 1, n, heuristic);
      if (result == null) {
        continue;
      }
      cost = DataUtil.calCost(origin, result);
      if (cost < minCost) {
        minCost = cost;
        minRepair = result;
      }
      if (heuristic && ConstraintUtil.checkConformance(result, minimumNetwork)) {
        break;
      }
    }
    return result;
  }

  private Map<String, Set<Long>> prune(MinimumNetwork minimumNetwork, Map<String, Set<Long>> candidateMap,
                                       Map<String, Node> origin,
                                       long curMinCost) {
    Set<String> allKey = candidateMap.keySet();
    Map<String, Set<Long>> newCandidateMap = new HashMap<>();
    for (String iKey : allKey) {
      Set<Long> candidateSet = candidateMap.get(iKey);
      Set<Long> newSet = new HashSet<>(candidateSet);
      if (!minimumNetwork.miniNet.containsKey(iKey)) {
        newCandidateMap.put(iKey, newSet);
        continue;
      }
      if (candidateSet.size() == 1) {
        newCandidateMap.put(iKey, newSet);
        continue;
      }
      long wMin = Long.MAX_VALUE;
      long tMin = Long.MAX_VALUE;
      for (long ti : candidateSet) {
        if (Math.abs(ti - origin.get(iKey).timestamp) > curMinCost) {
          newSet.remove(ti);
          continue;
        }

        boolean allSatisfyFlag = true;
        for (String jKey : allKey) {
          if (iKey.equals(jKey)) {
            continue;
          }
          Long iKey_jKey = null;
          Long jKey_iKey = null;
          if (minimumNetwork.miniNet.get(iKey).containsKey(jKey)) {
            iKey_jKey = minimumNetwork.miniNet.get(iKey).get(jKey).get(0);
          }
          if (minimumNetwork.miniNet.containsKey(jKey) && minimumNetwork.miniNet.get(jKey).containsKey(iKey)) {
            jKey_iKey = minimumNetwork.miniNet.get(jKey).get(iKey).get(0);
          }
          if (iKey_jKey == null && jKey_iKey == null) {
            continue;
          }
          for (long tj : candidateMap.get(jKey)) {
            if (iKey_jKey != null) {
              if (tj - ti > iKey_jKey) {
                allSatisfyFlag = false;
                break;
              }
            }
            if (jKey_iKey != null) {
              if (ti - tj > jKey_iKey) {
                allSatisfyFlag = false;
                break;
              }
            }
          }
          if (!allSatisfyFlag) {
            break;
          }
        }
        if (allSatisfyFlag) {
          newSet.remove(ti);
          long w = Math.abs(ti - origin.get(iKey).timestamp);
          if (w < wMin) {
            wMin = w;
            tMin = ti;
          }
        }
      }
      if (tMin != Long.MAX_VALUE) {
        newSet.add(tMin);
      }
      newCandidateMap.put(iKey, newSet);
    }

    for (String iKey : allKey) {
      if (!minimumNetwork.miniNet.containsKey(iKey)) {
        continue;
      }
      if (newCandidateMap.get(iKey).size() == 1) {
        long ti = newCandidateMap.get(iKey).iterator().next();
        for (String jKey : allKey) {
          if (iKey.equals(jKey) || newCandidateMap.get(jKey).size() == 1) {
            continue;
          }
          Set<Long> newSet = new HashSet<>(newCandidateMap.get(jKey));
          if (minimumNetwork.miniNet.get(iKey).containsKey(jKey)) {
            for (long tj : newCandidateMap.get(jKey)) {
              if (tj - ti > minimumNetwork.miniNet.get(iKey).get(jKey).get(0)) {
                newSet.remove(tj);
              }
            }
            newCandidateMap.put(jKey, newSet);
          }

          newSet = new HashSet<>(newCandidateMap.get(jKey));
          if (minimumNetwork.miniNet.containsKey(jKey) && minimumNetwork.miniNet.get(jKey).containsKey(iKey)) {
            for (long tj : newCandidateMap.get(jKey)) {
              if (ti - tj > minimumNetwork.miniNet.get(jKey).get(iKey).get(0)) {
                newSet.remove(tj);
              }
            }
            newCandidateMap.put(jKey, newSet);
          }
        }
      }
    }
    return newCandidateMap;
  }
}
