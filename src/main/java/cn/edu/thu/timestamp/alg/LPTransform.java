package cn.edu.thu.timestamp.alg;

import cn.edu.thu.timestamp.common.MinimumNetwork;
import cn.edu.thu.timestamp.common.Node;
import gurobi.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LPTransform {
  public Map<String, Node> repair(Map<String, Node> fault, MinimumNetwork minimumNetwork) {
    try {
      GRBEnv env = new GRBEnv("lp.log");
      GRBModel model = new GRBModel(env);
      GRBLinExpr expr = new GRBLinExpr();
      Map<String, GRBVar> uVariableMap = new HashMap<>();
      Map<String, GRBVar> vVariableMap = new HashMap<>();
      for (String key : fault.keySet()) {
        GRBVar ui = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "u_" + key);
        GRBVar vi = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "v_" + key);
        expr.addTerm(1, ui);
        expr.addTerm(1, vi);
        uVariableMap.put(key, ui);
        vVariableMap.put(key, vi);
      }
      model.setObjective(expr, GRB.MINIMIZE);

      for (String i : fault.keySet()) {
        if (!minimumNetwork.miniNet.containsKey(i)) {
          continue;
        }
        for (String j : minimumNetwork.miniNet.get(i).keySet()) {
          if (!fault.keySet().contains(j)) {
            continue;
          }
          GRBVar ui = uVariableMap.get(i);
          GRBVar vi = vVariableMap.get(i);
          GRBVar uj = uVariableMap.get(j);
          GRBVar vj = vVariableMap.get(j);
          long xi = fault.get(i).timestamp;
          long xj = fault.get(j).timestamp;
          expr = new GRBLinExpr();
          expr.addTerm(1, uj);
          expr.addTerm(-1, vj);
          expr.addTerm(-1, ui);
          expr.addTerm(1, vi);
          model.addConstr(expr, GRB.LESS_EQUAL, xi - xj + minimumNetwork.miniNet.get(i).get(j).get(0), "c" + i + j);
        }
      }
      model.optimize();

      Map<String, Node> result = new HashMap<>();
      for (String key : fault.keySet()) {
        long xi = fault.get(key).timestamp;
        GRBVar ui = uVariableMap.get(key);
        GRBVar vi = vVariableMap.get(key);
        long timestamp = Math.round(xi + ui.get(GRB.DoubleAttr.X) - vi.get(GRB.DoubleAttr.X));
        result.put(key, new Node(key, timestamp));
      }
      System.out.println(model.get(GRB.DoubleAttr.ObjVal));
      model.dispose();
      env.dispose();

      Repair repair = new Repair();
      result = repair.transform(minimumNetwork, fault, result);

      return result;
    } catch (GRBException e) {
      e.printStackTrace();
    }
    return fault;
  }


  public Map<String, Node> repairStreaming(Map<String, Node> fault, List<String> order,
                                           MinimumNetwork minimumNetwork,
                                           int windowSize, int stepSize) {
    if (stepSize > windowSize) {
      stepSize = windowSize;
    }
    Map<String, Node> result = new HashMap<>();
    Map<String, Node> curWindow = new HashMap<>();
    Map<String, Node> curRepair = new HashMap<>();
    String[] keyArray = new String[order.size()];
    order.toArray(keyArray);
    curWindow.put(order.get(0), fault.get(order.get(0)));
    curRepair.put(order.get(0), fault.get(order.get(0)));
    result.put(order.get(0), fault.get(order.get(0)));
    for (int i = 1; i < order.size(); i += stepSize) {
      int curEnd = i + stepSize < order.size() ? i + stepSize : order.size();
      for (int j = i; j < curEnd; j++) {
        String curKey = order.get(j);
        Node curNode = fault.get(curKey);
        curWindow.put(curKey, curNode);
        curRepair.put(curKey, curNode);
        if (curWindow.size() > windowSize) {
          String removeKey = order.get(j - windowSize);
          curWindow.remove(removeKey);
          curRepair.remove(removeKey);
        }
      }
      curRepair.putAll(repair(curRepair, minimumNetwork));
      result.putAll(curRepair);
    }
    return result;
  }
}
