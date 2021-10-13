package model;

import lombok.Data;
import util.Constants;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:29 AM
 */
@Data
public class Instance {
    private int timeHorizon;
    private List<Worker> workers;
    private List<Parcel> parcels;
    private List<Station> stations;
    private int[][] travelTimeMatrix;
    private int stationCapacity = 500;
    private int workerCapacity = 3;
    private String name;


    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TimeHorizon:" + timeHorizon + "\n");
        stringBuilder.append("StationNum:" + stations.size() + "\n");
        stringBuilder.append("WorkerNum:" + workers.size() + "\n");
        stringBuilder.append("ParcelNum:" + parcels.size() + "\n");
        stringBuilder.append("stationCapacity:" + stationCapacity + "\n");
        stringBuilder.append("workerCapacity:" + workerCapacity + "\n");
        stringBuilder.append("station lat lng\n");
        for (int i = 0; i < stations.size(); i++) {
            Station station = stations.get(i);
            stringBuilder.append(i + 1 + " " + station.getLat() + " " + station.getLng() + "\n");
        }
        stringBuilder.append("worker latO lngO latD lngD earliestD lastA drivingTMax\n");
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);
            stringBuilder.append(i + 1 + " " + worker.getLatO() + " " + worker.getLngO()+ " " + worker.getLatD() + " " + worker.getLngD() + " " + worker.getEarliestDeparture() + " " + worker.getLatestArrival() + " " + worker.getDrivingTimeMax() + "\n");
        }
        stringBuilder.append("parcel lat lng deadline\n");
        for (int i = 0; i < parcels.size(); i++) {
            Parcel parcel = parcels.get(i);
            stringBuilder.append(i + 1 + " " + parcel.getLat() + " " + parcel.getLng() + " " + parcel.getDeadline() + "\n");
        }
        return stringBuilder.toString();
    }
}
