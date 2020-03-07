package cn.edu.thu.timestamp;

import cn.edu.thu.timestamp.alg.LPTransform;
import cn.edu.thu.timestamp.alg.Repair;
import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;
import cn.edu.thu.timestamp.util.DataUtil;

import java.util.*;

public class ExpMain {
  public static void expMain(Map<String, Node> truth, Map<String, Node> fault,
                             List<Constraint> constraintList, MinimumNetwork minimumNetwork,
                             int chainMaxLength, boolean pruneFlag, boolean indexFlag, int indexBudget,
                             double[] accuracyAcc, double[] timeAcc) {
    long startTime;
    long endTime;
    Map<String, Node> result;

    // Exact
    startTime = System.currentTimeMillis();
    Repair exact = new Repair();
    result = exact.repair(fault, false, constraintList, minimumNetwork, chainMaxLength, pruneFlag, indexFlag, indexBudget);
    endTime = System.currentTimeMillis();
    accuracyAcc[0] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[0] += (endTime - startTime);


    // Heuristic
    startTime = System.currentTimeMillis();
    Repair heuristic = new Repair();
    result = heuristic.repair(fault, true, constraintList, minimumNetwork, chainMaxLength, pruneFlag, indexFlag, indexBudget);
    endTime = System.currentTimeMillis();
    accuracyAcc[1] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[1] += (endTime - startTime);


    // LP+transform
    startTime = System.currentTimeMillis();
    LPTransform lpTransform = new LPTransform();
    result = lpTransform.repair(fault, minimumNetwork);
    endTime = System.currentTimeMillis();
    accuracyAcc[2] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[2] += (endTime - startTime);

  }
}
