package cn.edu.thu.timestamp;

import cn.edu.thu.timestamp.alg.LPTransform;
import cn.edu.thu.timestamp.alg.StreamRepair;
import cn.edu.thu.timestamp.common.Constraint;
import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;
import cn.edu.thu.timestamp.util.DataUtil;

import java.util.List;
import java.util.Map;

public class StreamExpMain {
  public static void expMain(Map<String, Node> truth, Map<String, Node> fault, List<String> order,
                             List<Constraint> constraintList, MinimumNetwork minimumNetwork,
                             int windowSize, int stepSize, int chainMaxLength,
                             double[] accuracyAcc, double[] timeAcc) {
    long startTime;
    long endTime;
    Map<String, Node> result;

    // Exact
    startTime = System.currentTimeMillis();
    StreamRepair exact = new StreamRepair();
    result = exact.repairStreaming(fault, order, false, constraintList, minimumNetwork, windowSize, stepSize, chainMaxLength);
    endTime = System.currentTimeMillis();
    accuracyAcc[0] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[0] += (endTime - startTime);


    // Heuristic
    startTime = System.currentTimeMillis();
    StreamRepair heuristic = new StreamRepair();
    result = heuristic.repairStreaming(fault, order, true, constraintList, minimumNetwork, windowSize, stepSize, chainMaxLength);
    endTime = System.currentTimeMillis();
    accuracyAcc[1] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[1] += (endTime - startTime);


    // LP+transform
    startTime = System.currentTimeMillis();
    LPTransform lpTransform = new LPTransform();
    result = lpTransform.repairStreaming(fault, order, minimumNetwork, windowSize, stepSize);
    endTime = System.currentTimeMillis();
    accuracyAcc[2] += DataUtil.calAccuracy(truth, fault, result);
    timeAcc[2] += (endTime - startTime);

  }
}
