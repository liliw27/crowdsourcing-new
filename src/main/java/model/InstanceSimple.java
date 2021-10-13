package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 2021/9/20 10:10
 */
@Data
@Deprecated
public class InstanceSimple {
    private double[][][] cost;
    private int stationNum ;
    private int workerNum ;
    private int parcelNum;
    private int stationCapacity=500;
    private int workerCapacity=5;
    private String name;
}
