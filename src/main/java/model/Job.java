package model;

import lombok.Data;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 9/15/21 9:55 AM
 */
@Data
public class Job {
    private int jobIndex;
    private List<Parcel> parcelList;
    private Worker worker;
    private Station station;
    //    private Set<Integer> parcelIndexSet;
//    private int stationIndex;
//    private int workerIndex;
    private double travelTime;
    private double overtimePunish;
    private double costTotal;

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o)
            return true;
        if (!(o instanceof Job))
            return false;
        Job other = (Job) o;
        if (other.getWorker().getIndex() != this.getWorker().getIndex()) {
            return false;
        }
        if (other.getStation().getIndex() != this.getStation().getIndex()) {
            return false;
        }
        if (other.getParcelList().size() != this.getParcelList().size()) {
            return false;
        }
        for (Parcel parcel : this.getParcelList()) {
            if (!other.getParcelList().contains(parcel)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
//        int code;
//        code=station.getIndex();
//        code=code*31+worker.getIndex();
//        for(Parcel parcel : this.getParcelList()){
//            code=code*31+parcel.getIndex();
//        }
//        return code;
        return jobIndex;
    }
    public String toString(){
        String s="";
        s+="j_"+jobIndex+", ";
        s+="w_"+worker.getIndex()+", ";
        s+="s_"+station.getIndex()+", ";
        for(Parcel parcel:parcelList){
            s+="p_"+parcel.getIndex()+", ";
        }
        return s;
    }
}
