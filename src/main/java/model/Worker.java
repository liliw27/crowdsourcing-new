package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:42 AM
 */
@Data
public class Worker {
    private int index;
    private int indexO;
    private int indexD;
    private int latO;
    private int lngO;
    private int latD;
    private int lngD;
    private int earliestDeparture;
    private int latestArrival;
    private int drivingTimeMax;
    private int travelTOD;
}
