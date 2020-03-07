package cn.edu.thu.timestamp.alg;

import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;
import cn.edu.thu.timestamp.util.ConstraintUtil;
import cn.edu.thu.timestamp.util.DataUtil;

import java.util.*;

public class Repair {
  private long minCost = Long.MAX_VALUE;
  private Set<String> visited = new HashSet<>();
  private Map<String, Node> minRepair = null;
  private Map<Map<String, Node>, Map<Map<String, Node>, Map<String, Set<Long>>>> index = new HashMap<>();
  private KDTree kdTree;


  public Map<String, Node> repair(Map<String, Node> fault, boolean heuristic,
                                  List<Constraint> constraintList, MinimumNetwork minimumNetwork,
                                  int chainMaxLength, boolean pruneFlag, boolean indexFlag, int indexBudget) {
    List<Set<String>> cliqueList = DataUtil.getCliqueList(fault, constraintList);
    Map<String, Node> result = new HashMap<>();
    for (Set<String> cliqueSet : cliqueList) {
      Map<String, Node> tmp = new HashMap<>();
      for (String key : cliqueSet) {
        tmp.put(key, fault.get(key));
      }
      Repair alg = new Repair();
      Map<String, Node> tmpResult = alg.repairProcess(tmp, heuristic, constraintList, minimumNetwork, chainMaxLength, pruneFlag, indexFlag, indexBudget);
      result.putAll(tmpResult);
    }
    for (String key : fault.keySet()) {
      if (!result.containsKey(key)) {
        result.put(key, fault.get(key));
      }
    }
    return result;
  }

  private Map<String, Node> repairProcess(Map<String, Node> fault, boolean heuristic,
                                          List<Constraint> constraintList, MinimumNetwork minimumNetwork,
                                          int chainMaxLength, boolean pruneFlag, boolean indexFlag, int indexBudget) {
    Map<String, Node> init = fault;
    init = transform(minimumNetwork, fault, init);
    long curMinCost = DataUtil.calCost(fault, init);
    minCost = curMinCost;
    minRepair = init;
    Map<String, Set<Long>> candidateMap = generateCandidate(minimumNetwork, fault, curMinCost, constraintList, chainMaxLength);
    curMinCost = curMinCost < minCost ? curMinCost : minCost;
    Map<String, Map<String, List<Constraint>>> constraintIndex = ConstraintUtil.buildIndex(constraintList);
    String[] keyArray = new String[fault.size()];
    int i = 0;
    for (String key : fault.keySet()) {
      keyArray[i] = key;
      i++;
    }
    kdTree = new KDTree(keyArray, indexBudget);
    iterRepair(constraintList, candidateMap, fault, new HashMap<>(), curMinCost, keyArray, 0, fault.size(), constraintIndex, heuristic, indexFlag, pruneFlag);
    return minRepair;
  }


  private Map<String, Node> iterRepair(List<Constraint> constraintList, Map<String, Set<Long>> candidateMap,
                                       Map<String, Node> origin, Map<String, Node> tuple, long curMinCost,
                                       String[] keyArray, int k, int n,
                                       Map<String, Map<String, List<Constraint>>> constraintIndex,
                                       boolean heuristic, boolean indexFlag, boolean pruneFlag) {
    if (pruneFlag) {
      candidateMap = prune(candidateMap, origin, constraintIndex, curMinCost);
    }
    Map<String, Node> result = null;

    if (k >= n) {
      return tuple;
    }
    curMinCost = curMinCost < minCost ? curMinCost : minCost;


    if (indexFlag) {
      List<Map<String, Node>> cache = kdTree.rangeQuery(candidateMap);
      Map<String, Node> newXMin = null;
      for (Map<String, Node> optimal : cache) {
        for (Map<String, Node> x_tilde : index.get(optimal).keySet()) {
          newXMin = calRepairViaIndex(optimal, origin, candidateMap, x_tilde, index.get(optimal).get(x_tilde), constraintIndex);
          if (newXMin == null) {
            continue;
          }
          long cost = DataUtil.calCost(newXMin, origin);
          if (cost < curMinCost) {
            minCost = cost;
            minRepair = newXMin;
          }
          break;
        }
      }
      if (newXMin != null) {
        return newXMin;
      }
    }

    String key = keyArray[k];
    for (long candidate : candidateMap.get(key)) {
      Map<String, Node> tupleNew = DataUtil.deepCopy(tuple);
      tupleNew.put(key, new Node(key, candidate));
      long cost = DataUtil.calCost(origin, tupleNew);
      if (cost > curMinCost || !ConstraintUtil.checkConformance(tupleNew, constraintList)) {
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
      result = iterRepair(constraintList, newCandidateMap, origin, tupleNew, curMinCost, keyArray, k + 1, n, constraintIndex, heuristic, indexFlag, pruneFlag);
      if (result == null) {
        continue;
      }
      cost = DataUtil.calCost(origin, result);
      if (cost < minCost) {
        minCost = cost;
        minRepair = result;
      }
      if (heuristic && ConstraintUtil.checkConformance(result, constraintList)) {
        break;
      }
    }
    if (indexFlag) {
      if (result != null) {
        Map<Map<String, Node>, Map<String, Set<Long>>> tmp;
        Map<String, Node> cacheKey = kdTree.checkCache(result);
        if (cacheKey == null) {
          kdTree.insert(result);
          tmp = new HashMap<>();
          index.put(result, tmp);
        } else {
          tmp = index.get(result);
        }
        tmp.put(origin, candidateMap);
      }
    }

    return result;
  }

  public Map<String, Node> transform(MinimumNetwork minimumNetwork, Map<String, Node> origin, Map<String, Node> init) {
    Map<String, Node> result = DataUtil.deepCopy(init);

    Set<String> vSet = new HashSet<String>(result.keySet());
    Set<String> mSet = new HashSet<String>();
    Set<String> uSet = new HashSet<String>();
    while (vSet.size() > 0) {
      String current = vSet.iterator().next();
      vSet.remove(current);
      mSet.add(current);

      long alpha = Long.MIN_VALUE;
      long beta = Long.MIN_VALUE;
      for (String k : uSet) {
        Node iNode = result.get(current);
        Node kNode = result.get(k);
        if (minimumNetwork.miniNet.containsKey(current) && minimumNetwork.miniNet.get(current).containsKey(k)) {
          long diff = (kNode.timestamp - iNode.timestamp) - minimumNetwork.miniNet.get(current).get(k).get(0);
          if (diff > alpha) {
            alpha = diff;
          }
        }
        if (minimumNetwork.miniNet.containsKey(k) && minimumNetwork.miniNet.get(k).containsKey(current)) {
          long diff = (iNode.timestamp - kNode.timestamp) - minimumNetwork.miniNet.get(k).get(current).get(0);
          if (diff > beta) {
            beta = diff;
          }
        }
      }
      if (alpha > 0) {
        result.get(current).timestamp += alpha;
      } else if (beta > 0) {
        result.get(current).timestamp -= beta;
      }

      while (mSet.size() > 0) {
        Set<String> mSetTmp = new HashSet<String>(mSet);
        for (String j : mSetTmp) {
          Node jNode = result.get(j);
          for (String i : vSet) {
            Node iNode = result.get(i);
            if (minimumNetwork.miniNet.containsKey(i) && minimumNetwork.miniNet.get(i).containsKey(j)) {
              if (jNode.timestamp - iNode.timestamp == minimumNetwork.miniNet.get(i).get(j).get(0)) {
                if (checkMSetAdd(result, minimumNetwork, mSet, uSet, i)) {
                  mSet.add(i);
                  continue;
                }
              }
            }
            if (minimumNetwork.miniNet.containsKey(j) && minimumNetwork.miniNet.get(j).containsKey(i)) {
              if (iNode.timestamp - jNode.timestamp == minimumNetwork.miniNet.get(j).get(i).get(0)) {
                if (checkMSetAdd(result, minimumNetwork, mSet, uSet, i)) {
                  mSet.add(i);
                  continue;
                }
              }
            }
          }
        }
        vSet.removeAll(mSet);

        for (String j : mSetTmp) {
          boolean flag = false;
          Node jNode = result.get(j);
          for (String k : uSet) {
            Node kNode = result.get(k);
            if (minimumNetwork.miniNet.containsKey(k) && minimumNetwork.miniNet.get(k).containsKey(j)) {
              if (jNode.timestamp - kNode.timestamp == minimumNetwork.miniNet.get(k).get(j).get(0)) {
                flag = true;
                break;
              }
            }
            if (minimumNetwork.miniNet.containsKey(j) && minimumNetwork.miniNet.get(j).containsKey(k)) {
              if (kNode.timestamp - jNode.timestamp == minimumNetwork.miniNet.get(j).get(k).get(0)) {
                flag = true;
                break;
              }
            }
          }
          if (flag) {
            uSet.addAll(mSet);
            mSet.clear();
            break;
          }
        }

        for (String j : mSet) {
          Node originJNode = origin.get(j);
          Node resultJNode = result.get(j);
          if (originJNode.timestamp - resultJNode.timestamp == 0) {
            uSet.addAll(mSet);
            mSet.clear();
            break;
          }
        }
        if (mSet.size() == 0) {
          break;
        }

        Set<String> pSet = new HashSet<String>();
        Set<String> qSet = new HashSet<String>();
        for (String j : mSet) {
          Node originJNode = origin.get(j);
          Node resultJNode = result.get(j);
          if (originJNode.timestamp < resultJNode.timestamp) {
            pSet.add(j);
          } else {
            qSet.add(j);
          }
        }

        long eta = Long.MAX_VALUE;
        long theta = Long.MAX_VALUE;
        if (pSet.size() > qSet.size()) {
          for (String j : mSet) {
            if (!minimumNetwork.miniNet.containsKey(j)) {
              continue;
            }
            Set<String> unionSet = new HashSet<String>(vSet);
            unionSet.addAll(uSet);
            for (String k : unionSet) {
              if (!minimumNetwork.miniNet.get(j).containsKey(k)) {
                continue;
              }
              Node jNode = result.get(j);
              Node kNode = result.get(k);
              long diff = minimumNetwork.miniNet.get(j).get(k).get(0) - (kNode.timestamp - jNode.timestamp);
              if (diff > 0 && diff < eta) {
                eta = diff;
              }
            }
          }

          for (String j : pSet) {
            Node originJNode = origin.get(j);
            Node resultJNode = result.get(j);
            long diff = resultJNode.timestamp - originJNode.timestamp;
            if (diff < theta) {
              theta = diff;
            }
          }

          long minus = (eta < theta) ? eta : theta;
          for (String j : mSet) {
            result.get(j).timestamp -= minus;
          }
        } else {
          for (String j : mSet) {
            Set<String> unionSet = new HashSet<String>(vSet);
            unionSet.addAll(uSet);
            for (String k : unionSet) {
              if (!(minimumNetwork.miniNet.containsKey(k) && minimumNetwork.miniNet.get(k).containsKey(j))) {
                continue;
              }
              Node jNode = result.get(j);
              Node kNode = result.get(k);
              long diff = minimumNetwork.miniNet.get(k).get(j).get(0) - (jNode.timestamp - kNode.timestamp);
              if (diff > 0 && diff < eta) {
                eta = diff;
              }
            }
          }

          for (String j : qSet) {
            Node originJNode = origin.get(j);
            Node resultJNode = result.get(j);
            long diff = originJNode.timestamp - resultJNode.timestamp;
            if (diff < theta) {
              theta = diff;
            }
          }
          long add = (eta < theta) ? eta : theta;
          for (String j : mSet) {
            result.get(j).timestamp += add;
          }
        }

      }
    }
    return result;
  }

  private boolean checkMSetAdd(Map<String, Node> result, MinimumNetwork minimumNetwork, Set<String> mSet,
                               Set<String> uSet, String i) {
    Node iNode = result.get(i);
    Set<String> unionSet = new HashSet<>(mSet);
    unionSet.addAll(uSet);
    for (String m : unionSet) {
      Node mNode = result.get(m);
      if (minimumNetwork.miniNet.containsKey(i) && minimumNetwork.miniNet.get(i).containsKey(m)) {
        if ((mNode.timestamp - iNode.timestamp) > minimumNetwork.miniNet.get(i).get(m).get(0)) {
          return false;
        }
      }
      if (minimumNetwork.miniNet.containsKey(m) && minimumNetwork.miniNet.get(m).containsKey(i)) {
        if ((iNode.timestamp - mNode.timestamp) > minimumNetwork.miniNet.get(m).get(i).get(0)) {
          return false;
        }
      }
    }
    return true;
  }


  private Map<String, Set<Long>> generateCandidate(MinimumNetwork minimumNetwork, Map<String, Node> origin,
                                                   long curMinCost, List<Constraint> constraintList,
                                                   int chainMaxLength) {
    Map<String, Set<Long>> candidateMap = new HashMap<String, Set<Long>>();
    for (String key : origin.keySet()) {
      Set<Long> candidateSet = new HashSet<Long>();
      candidateSet.add(origin.get(key).timestamp);
      candidateMap.put(key, candidateSet);
    }

    for (String key : origin.keySet()) {
      Map<String, Node> t = new HashMap<String, Node>();
      t.put(key, new Node(key, origin.get(key).timestamp));
      generate(origin, key, t, minimumNetwork, true, candidateMap, curMinCost, chainMaxLength, constraintList);
      generate(origin, key, t, minimumNetwork, false, candidateMap, curMinCost, chainMaxLength, constraintList);
    }
    return candidateMap;
  }

  private void generate(Map<String, Node> origin, String curNode, Map<String, Node> t, MinimumNetwork minimumNetwork,
                        boolean direction, Map<String, Set<Long>> candidateMap, long curMinCost, int chainMaxLength,
                        List<Constraint> constraintList) {
    long cost = DataUtil.calCost(origin, t);
    curMinCost = curMinCost < minCost ? curMinCost : minCost;
    if (cost > curMinCost || t.size() > chainMaxLength || !ConstraintUtil.checkConformance(t, constraintList)
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
            generate(origin, nextNode, newT, minimumNetwork, false, candidateMap, curMinCost, chainMaxLength, constraintList);
          }
        }
      } else {
        if (minimumNetwork.miniNet.containsKey(nextNode) && minimumNetwork.miniNet.get(nextNode).containsKey(curNode)) {
          for (long distance : minimumNetwork.miniNet.get(nextNode).get(curNode)) {
            Map<String, Node> newT = DataUtil.deepCopy(t);
            long timeCandidate = t.get(curNode).timestamp - distance;
            newT.put(nextNode, new Node(nextNode, timeCandidate));
            candidateMap.get(nextNode).add(timeCandidate);
            generate(origin, nextNode, newT, minimumNetwork, true, candidateMap, curMinCost, chainMaxLength, constraintList);
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


  private Map<String, Set<Long>> prune(Map<String, Set<Long>> candidateMap, Map<String, Node> origin,
                                       Map<String, Map<String, List<Constraint>>> constraintIndex,
                                       long curMinCost) {
    Set<String> allKey = candidateMap.keySet();
    Map<String, Set<Long>> newCandidateMap = new HashMap<>();
    for (String iKey : allKey) {
      Set<Long> candidateSet = candidateMap.get(iKey);
      Set<Long> newSet = new HashSet<>(candidateSet);
      if (!constraintIndex.containsKey(iKey)) {
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
          List<Constraint> iKey_jKey = null;
          List<Constraint> jKey_iKey = null;
          if (constraintIndex.get(iKey).containsKey(jKey)) {
            iKey_jKey = constraintIndex.get(iKey).get(jKey);
          }
          if (constraintIndex.containsKey(jKey) && constraintIndex.get(jKey).containsKey(iKey)) {
            jKey_iKey = constraintIndex.get(jKey).get(iKey);
          }
          if (iKey_jKey == null && jKey_iKey == null) {
            continue;
          }
          for (long tj : candidateMap.get(jKey)) {
            if (iKey_jKey != null) {
              for (Constraint constraint : iKey_jKey) {
                if (!ConstraintUtil.checkConformance(ti, tj, constraint)) {
                  allSatisfyFlag = false;
                  break;
                }
              }
            }
            if (!allSatisfyFlag) {
              break;
            }
            if (jKey_iKey != null) {
              for (Constraint constraint : jKey_iKey) {
                if (!ConstraintUtil.checkConformance(tj, ti, constraint)) {
                  allSatisfyFlag = false;
                  break;
                }
              }
            }
            if (!allSatisfyFlag) {
              break;
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
      if (!constraintIndex.containsKey(iKey)) {
        continue;
      }
      if (newCandidateMap.get(iKey).size() == 1) {
        long ti = newCandidateMap.get(iKey).iterator().next();
        for (String jKey : allKey) {
          if (iKey.equals(jKey) || newCandidateMap.get(jKey).size() == 1) {
            continue;
          }
          Set<Long> newSet = new HashSet<>(newCandidateMap.get(jKey));
          if (constraintIndex.get(iKey).containsKey(jKey)) {
            for (long tj : newCandidateMap.get(jKey)) {
              for (Constraint constraint : constraintIndex.get(iKey).get(jKey)) {
                if (!ConstraintUtil.checkConformance(ti, tj, constraint)) {
                  newSet.remove(tj);
                }
              }
            }
            newCandidateMap.put(jKey, newSet);
          }
          newSet = new HashSet<>(newCandidateMap.get(jKey));
          if (constraintIndex.containsKey(jKey) && constraintIndex.get(jKey).containsKey(iKey)) {
            for (long tj : newCandidateMap.get(jKey)) {
              for (Constraint constraint : constraintIndex.get(jKey).get(iKey)) {
                if (!ConstraintUtil.checkConformance(tj, ti, constraint)) {
                  newSet.remove(tj);
                }
              }
            }
            newCandidateMap.put(jKey, newSet);
          }
        }
      }
    }
    return newCandidateMap;
  }


  private Map<String, Node> calRepairViaIndex(Map<String, Node> optimal, Map<String, Node> x, Map<String, Set<Long>> T,
                                              Map<String, Node> x_tilde, Map<String, Set<Long>> T_tilde,
                                              Map<String, Map<String, List<Constraint>>> constraintIndex) {
    int state = isSafeSubProblem(x, T, x_tilde, T_tilde, constraintIndex);
    if (state == 1) {
      for (String key : T_tilde.keySet()) {
        if (!T.get(key).contains(x_tilde.get(key).timestamp)) {
          return null;
        }
      }
      return optimal;
    } else if (state == 0) {
      Map<String, Node> newX = new HashMap<>();
      for (String key : T_tilde.keySet()) {
        if (isSubCandidateSet(x.get(key).timestamp, T.get(key), x_tilde.get(key).timestamp, T_tilde.get(key))) {
          if (!T.get(key).contains(x_tilde.get(key).timestamp)) {
            return null;
          }
          newX.put(key, new Node(key, optimal.get(key).timestamp));
        } else {
          long minValue = 0;
          long minDistance = Long.MAX_VALUE;
          for (long candidate : T.get(key)) {
            long distance = Math.abs(candidate - x.get(key).timestamp);
            if (distance < minDistance) {
              minDistance = distance;
              minValue = candidate;
            }
          }
          newX.put(key, new Node(key, minValue));
        }
      }
      return newX;
    }
    return null;
  }

  private int isSafeSubProblem(Map<String, Node> x, Map<String, Set<Long>> T,
                               Map<String, Node> x_tilde, Map<String, Set<Long>> T_tilde,
                               Map<String, Map<String, List<Constraint>>> constraintIndex) {
    int result = 1;
    for (String key : T.keySet()) {
      if (!isSubCandidateSet(x.get(key).timestamp, T.get(key), x_tilde.get(key).timestamp, T_tilde.get(key))) {
        result = 0;
        if (!isSafeCandidateSet(key, T, constraintIndex) || !isSafeCandidateSet(key, T_tilde, constraintIndex)) {
          return -1;
        }
      }
    }
    return result;
  }

  private boolean isSafeCandidateSet(String target, Map<String, Set<Long>> T,
                                     Map<String, Map<String, List<Constraint>>> constraintIndex) {
    for (long ti : T.get(target)) {
      for (String other : T.keySet()) {
        if (other.equals(target)) {
          continue;
        }
        if (constraintIndex.containsKey(target) && constraintIndex.get(target).containsKey(other)) {
          for (Constraint constraint : constraintIndex.get(target).get(other)) {
            for (long tj : T.get(other)) {
              if (!ConstraintUtil.checkConformance(ti, tj, constraint)) {
                return false;
              }
            }
          }
        }
        if (constraintIndex.containsKey(other) && constraintIndex.get(other).containsKey(target)) {
          for (Constraint constraint : constraintIndex.get(other).get(target)) {
            for (long tj : T.get(other)) {
              if (!ConstraintUtil.checkConformance(tj, ti, constraint)) {
                return false;
              }
            }
          }
        }
      }
    }
    return true;
  }

  private boolean isSubCandidateSet(long xi, Set<Long> Ti,
                                    long xi_tilde, Set<Long> Ti_tilde) {
    if (!isSubset(Ti, Ti_tilde)) {
      return false;
    }
    for (Long ti : Ti) {
      if (Math.abs(ti - xi) != Math.abs(ti - xi_tilde)) {
        return false;
      }
    }
    return true;
  }

  private boolean isSubset(Set<Long> a, Set<Long> b) {
    for (long aValue : a) {
      if (!b.contains(aValue)) {
        return false;
      }
    }
    return true;
  }

  private class KDTree {
    TreeNode root;
    int budget;
    int size;
    int curI;
    String[] keyArray;
    List<Map<String, Node>> allLeaf;

    KDTree(String[] keyArray, int budget) {
      root = null;
      size = 0;
      curI = 0;
      this.keyArray = keyArray;
      this.budget = budget;
      allLeaf = new ArrayList<>();
    }

    void insert(Map<String, Node> x_tilde) {
      if (allLeaf.size() > budget) {
        deleteNode(allLeaf.get(curI));
        allLeaf.set(curI, x_tilde);
        curI = (curI + 1) % budget;
      } else {
        allLeaf.add(x_tilde);
      }
      if (root == null) {
        root = new TreeNode(0);
      } else {
        TreeNode curNode = root;
        TreeNode preNode = null;
        int h = 0;
        while (curNode != null && curNode.x_tilde_index == -1) {
          long nodeValue = curNode.value;
          long curValue = x_tilde.get(curNode.key).timestamp;
          preNode = curNode;
          if (curValue > nodeValue) {
            curNode = curNode.right;
          } else {
            curNode = curNode.left;
          }
          h++;
        }

        if (curNode == null) {
          if (preNode.left == null) {
            preNode.left = new TreeNode(allLeaf.size() - 1);
          } else {
            preNode.right = new TreeNode(allLeaf.size() - 1);
          }
          return;
        }
        String key = keyArray[h % keyArray.length];
        long curValue = x_tilde.get(key).timestamp;
        long leafValue = allLeaf.get(curNode.x_tilde_index).get(key).timestamp;
        int cur_x_tilde_index = curNode.x_tilde_index;
        while (leafValue == curValue) {
          curNode.left = new TreeNode(key, leafValue);
          curNode.right = null;
          curNode = curNode.left;
          h++;
          key = keyArray[h % keyArray.length];
          curValue = x_tilde.get(key).timestamp;
          leafValue = allLeaf.get(cur_x_tilde_index).get(key).timestamp;
        }
        if (leafValue < curValue) {
          curNode.left = new TreeNode(cur_x_tilde_index);
          curNode.right = new TreeNode(allLeaf.size() - 1);
          curNode.key = key;
          curNode.value = leafValue;
          curNode.x_tilde_index = -1;
        } else {
          curNode.right = new TreeNode(cur_x_tilde_index);
          curNode.left = new TreeNode(allLeaf.size() - 1);
          curNode.key = key;
          curNode.value = curValue;
          curNode.x_tilde_index = -1;
        }
      }
    }

    List<Map<String, Node>> rangeQuery(Map<String, Set<Long>> candidateMap) {
      Map<String, long[]> rangeMap = new HashMap<>();
      long min = Long.MAX_VALUE;
      long max = Long.MIN_VALUE;
      for (String key : candidateMap.keySet()) {
        for (long value : candidateMap.get(key)) {
          if (value > max) {
            max = value;
          }
          if (value < min) {
            min = value;
          }
        }
        long[] rangeArray = new long[2];
        rangeArray[0] = min;
        rangeArray[1] = max;
        rangeMap.put(key, rangeArray);
      }
      return iterRangeQuery(root, rangeMap);
    }

    private List<Map<String, Node>> iterRangeQuery(TreeNode curNode, Map<String, long[]> rangeMap) {
      List<Map<String, Node>> result = new ArrayList<>();
      if (curNode == null) {
        return result;
      }
      if (curNode.x_tilde_index != -1) {
        if (checkInRange(rangeMap, allLeaf.get(curNode.x_tilde_index))) {
          result.add(allLeaf.get(curNode.x_tilde_index));
        }
        return result;
      }
      long[] rangeArray = rangeMap.get(curNode.key);
      if (rangeArray[0] > curNode.value) {
        return iterRangeQuery(curNode.right, rangeMap);
      } else if (rangeArray[1] <= curNode.value) {
        return iterRangeQuery(curNode.left, rangeMap);
      } else {
        result.addAll(iterRangeQuery(curNode.left, rangeMap));
        result.addAll(iterRangeQuery(curNode.right, rangeMap));
        return result;
      }
    }

    private boolean checkInRange(Map<String, long[]> rangeMap, Map<String, Node> x) {
      for (String key : x.keySet()) {
        long timestamp = x.get(key).timestamp;
        if (timestamp < rangeMap.get(key)[0] || timestamp > rangeMap.get(key)[1]) {
          return false;
        }
      }
      return true;
    }

    Map<String, Node> checkCache(Map<String, Node> x_tilde) {
      for (Map<String, Node> x : allLeaf) {
        boolean flag = true;
        for (String key : x.keySet()) {
          if (x.get(key).timestamp != x_tilde.get(key).timestamp) {
            flag = false;
            break;
          }
        }
        if (flag) {
          return x;
        }
      }
      return null;
    }

    private void deleteNode(Map<String, Node> x_tilde) {
      TreeNode curNode = root;
      TreeNode preNode = null;
      while (curNode.x_tilde_index == -1) {
        preNode = curNode;
        if (x_tilde.get(curNode.key).timestamp > curNode.value) {
          curNode = curNode.right;
        } else {
          curNode = curNode.left;
        }
      }
      if (preNode.right == curNode) {
        preNode.right = null;
      } else {
        preNode.left = null;
      }
    }
  }

  private class TreeNode {
    String key;
    long value;
    int x_tilde_index;
    TreeNode left;
    TreeNode right;

    TreeNode(int x_tilde_index) {
      this.key = null;
      this.x_tilde_index = x_tilde_index;
      left = null;
      right = null;
    }

    TreeNode(String key, long value) {
      this.key = key;
      this.value = value;
      x_tilde_index = -1;
      left = null;
      right = null;
    }
  }
}
