package model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 2021/9/19 14:09
 */
@Data
public class InstanceJob {
    private List<Station> stationList;
    private List<Worker> workerList;
    private List<Parcel> parcelList;
    private Map<Worker,Integer> workerIdxMap;
    private Map<Parcel,Integer> parcelIdxMap;
    private List<Job> jobList;
    private List<Job> jobMipList=new ArrayList<>();
    private Map<Parcel, Set<Job>> parcelToJob;
    private int workerCapacity;
    private String name;

    public void setWorkerList( List<Worker> workerList){
        this.workerList=workerList;
        workerIdxMap=new HashMap<>();
        for(int i=0;i<workerList.size();i++){
            Worker worker=workerList.get(i);
            workerIdxMap.put(worker,i);
        }
    }
    public void setParcelList(List<Parcel>parcelList){
        this.parcelList=parcelList;
        parcelIdxMap=new HashMap<>();
        for(int i=0;i<parcelList.size();i++){
            Parcel parcel=parcelList.get(i);
            parcelIdxMap.put(parcel,i);
        }
    }

}
