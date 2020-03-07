package cn.edu.thu.timestamp.common;

public class Node {
  public String id;
  public long timestamp;
  public String value;

  public Node(String id, long timestamp) {
    this.id = id;
    this.timestamp = timestamp;
  }

  public Node(String id, long timestamp, String value) {
    this.id = id;
    this.timestamp = timestamp;
    this.value = value;
  }
}
