package cn.edu.thu.timestamp.common;

import java.util.ArrayList;
import java.util.List;

public class Constraint {
  public String startId;
  public String endId;
  public List<long[]> constraints;

  public Constraint(String startId, String endId) {
    this.startId = startId;
    this.endId = endId;
    constraints = new ArrayList<long[]>();
  }

  public void addConstraint(long a, long b) {
    long[] tmp = new long[2];
    tmp[0] = a;
    tmp[1] = b;
    constraints.add(tmp);
  }
}
