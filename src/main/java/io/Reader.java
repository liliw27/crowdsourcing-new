package io;

import model.Instance;
import model.InstanceJob;
import model.InstanceSimple;
import model.Parcel;
import model.Station;
import model.Worker;
import util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 10:07 AM
 */
public class Reader {

    public static Instance readInstance(File file) throws FileNotFoundException {
        List<Worker> workers = new ArrayList<>();
        List<Parcel> parcels = new ArrayList<>();
        List<Station> stations = new ArrayList<>();
        int[][] travelTimeMatrix;

        int stationCapacity;
        int workerCapacity;


        Instance instance = new Instance();
        Scanner scanner = new Scanner(file);
        String string = scanner.nextLine();
        String[] split = string.split(":");
        int timeHorizon = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        int stationNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        int workerNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        int parcelNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        stationCapacity = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(":");
        workerCapacity = Integer.parseInt(split[1]);

        int index = 0;

        scanner.nextLine();

        for (int i = 0; i < stationNum; i++) {
            Station station = new Station();
            string = scanner.nextLine();
            split = string.split(" ");
            station.setCurrentCapRemained(stationCapacity);
            station.setLat(Integer.parseInt(split[1]));
            station.setLng(Integer.parseInt(split[2]));
            station.setNodeIndex(index);
            station.setIndex(i);
            stations.add(station);
            index++;
        }

        scanner.nextLine();

        for (int i = 0; i < workerNum; i++) {
            Worker worker = new Worker();
            string = scanner.nextLine();
            split = string.split(" ");
            if(i>=40){
                continue;
            }
            int latO=Integer.parseInt(split[1]);
            int lngO=Integer.parseInt(split[2]);
            int latD=Integer.parseInt(split[3]);
            int lngD=Integer.parseInt(split[4]);
            worker.setLatO(latO);
            worker.setLngO(lngO);
            worker.setLatD(latD);
            worker.setLngD(lngD);
            worker.setEarliestDeparture(Integer.parseInt(split[5]));
            worker.setLatestArrival(Integer.parseInt(split[6]));
            worker.setDrivingTimeMax(Integer.parseInt(split[7]));
            worker.setIndexO(index);
            index++;
            worker.setIndexD(index);
            index++;
            int travelTOD=Util.calTravelTime(latO,lngO,latD,lngD);
            worker.setTravelTOD(travelTOD);
            worker.setIndex(i);
            workers.add(worker);
        }

        scanner.nextLine();

        for (int i = 0; i < parcelNum; i++) {
            Parcel parcel = new Parcel();
            string = scanner.nextLine();
            split = string.split(" ");
            if(i>=20){
                continue;
            }
            parcel.setLat(Integer.parseInt(split[1]));
            parcel.setLng(Integer.parseInt(split[2]));
            parcel.setDeadline(Integer.parseInt(split[3]));
            parcel.setNodeIndex(index);
            parcel.setIndex(i);
            parcels.add(parcel);
            index++;
        }
        travelTimeMatrix=calTravelTimeMatrix(workers,parcels,stations);
        instance.setWorkerCapacity(workerCapacity);
        instance.setStationCapacity(stationCapacity);
        instance.setWorkers(workers);
        instance.setStations(stations);
        instance.setParcels(parcels);
        instance.setName(file.getName());
        instance.setTimeHorizon(timeHorizon);
        instance.setTravelTimeMatrix(travelTimeMatrix);
        setNearestStation(parcels,stations);
        return instance;
    }

    private static void setNearestStation(List<Parcel> parcels, List<Station> stations){
        for(Parcel parcel:parcels){
            int min=100000;
            Station stationMin=null;
            for(Station station:stations){
                int travelTime=Util.calTravelTime(parcel.getLat(),parcel.getLng(),station.getLat(),station.getLng());
                if(min>travelTime){
                    min=travelTime;
                    stationMin=station;
                }
            }
            parcel.setNearestStation(stationMin);
        }
    }

    private static int[][] calTravelTimeMatrix(List<Worker> workers, List<Parcel> parcels, List<Station> stations) {
        int totalNodes=workers.size()*2+parcels.size()+stations.size();
        int[][] travelTimeMatrix=new int[totalNodes][totalNodes];
        for(Worker worker:workers){
            int o=worker.getIndexO();

            int d=worker.getIndexD();
//            travelTimeMatrix[o][d]=Util.calTravelTime(worker.getLatO(),worker.getLngO(),worker.getLatD(),worker.getLngD());
            travelTimeMatrix[o][d]=0;
            travelTimeMatrix[d][o]=travelTimeMatrix[o][d];
            for(Station station:stations){
                int i=worker.getIndexO();
                int j=station.getNodeIndex();
                int k=worker.getIndexD();
                travelTimeMatrix[i][j]= Util.calTravelTime(worker.getLatO(),worker.getLngO(),station.getLat(),station.getLng());
                travelTimeMatrix[j][k]= Util.calTravelTime(station.getLat(),station.getLng(),worker.getLatD(),worker.getLngD());
            }
            for(Parcel parcel:parcels){
                int i=parcel.getNodeIndex();
                int j=worker.getIndexD();
                travelTimeMatrix[i][j]=Util.calTravelTime(parcel.getLat(),parcel.getLng(),worker.getLatD(),worker.getLngD());
            }
        }
        for(Station station:stations){
            for(Parcel parcel:parcels){
                int i=station.getNodeIndex();
                int j=parcel.getNodeIndex();
                travelTimeMatrix[i][j]=Util.calTravelTime(station.getLat(),station.getLng(),parcel.getLat(),parcel.getLng());
            }
        }
        for(Parcel parcel:parcels){
            for(Parcel parcel1:parcels){
                int i=parcel.getNodeIndex();
                int j=parcel1.getNodeIndex();
                if(i!=j){
                    travelTimeMatrix[i][j]=Util.calTravelTime(parcel.getLat(),parcel.getLng(),parcel1.getLat(),parcel1.getLng());
                }
            }

        }
        return travelTimeMatrix;
    }

    public static InstanceSimple readInstanceSimple(File file) throws FileNotFoundException {
        double[][][] cost;

        int stationNum;
        int workerNum;
        int parcelNum;

        Scanner scanner = new Scanner(file);
        String string = scanner.nextLine();
        String[] split = string.split(" ");
        stationNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(" ");
        workerNum = Integer.parseInt(split[1]);
        string = scanner.nextLine();
        split = string.split(" ");
        parcelNum = Integer.parseInt(split[1]);

        cost = new double[stationNum][workerNum][parcelNum];
        for (int i = 0; i < stationNum; i++) {
            for (int j = 0; j < workerNum; j++) {
                string = scanner.nextLine();
                split = string.split(" ");
                for (int k = 0; k < parcelNum; k++) {
                    cost[i][j][k] = Double.parseDouble(split[k]);
                }
            }
        }
        InstanceSimple instanceSimple = new InstanceSimple();
        instanceSimple.setCost(cost);
        instanceSimple.setParcelNum(parcelNum);
        instanceSimple.setStationNum(stationNum);
        instanceSimple.setWorkerNum(workerNum);
        instanceSimple.setName(file.getName().substring(0, file.getName().length() - 4));
        return instanceSimple;
    }

//    public static InstanceJob readCGInput(File file) throws FileNotFoundException {
//        int stationNum;
//        int workerNum;
//        int parcelNum;
//        int jobNum;
//        JobSimple[] jobSimpleArray;
//        Map<Integer, Set<Integer>> parcelToJob = new HashMap<>(10);
//        int stationCapacity;
//        int workerCapacity;
//        String name;
//
//
//        Scanner scanner = new Scanner(file);
//        String string = scanner.nextLine();
//        String[] split = string.split(" ");
//        stationNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        workerNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        parcelNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        jobNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        workerCapacity = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        stationCapacity = Integer.parseInt(split[1]);
//        scanner.nextLine();
//        int count = 0;
//        jobSimpleArray = new JobSimple[jobNum];
//
//        for (int i = 0; i < parcelNum; i++) {
//            parcelToJob.put(i, new HashSet<>());
//        }
//        while (count < jobNum) {
//            Set<Integer> parcelSet = new HashSet<>();
//            string = scanner.nextLine();
//            split = string.split("\t");
////            String nodeIndex=split[0];
//            String parcleString = split[1];
//            String[] parcleSplit = parcleString.split(",");
//            for (int i = 0; i < parcleSplit.length; i++) {
//                int ind = Integer.parseInt(parcleSplit[i]);
//                parcelSet.add(ind);
//            }
//            JobSimple jobSimple = new JobSimple();
//            jobSimpleArray[count] = jobSimple;
////            jobSimple.setJobIndex(Integer.parseInt(nodeIndex));
//            jobSimple.setParcelIndexSet(parcelSet);
//            jobSimple.setJobIndex(count);
//            jobSimple.setWorkerIndex(Integer.parseInt(split[2]));
//            jobSimple.setStationIndex(Integer.parseInt(split[3]));
//            jobSimple.setDistance(Double.parseDouble(split[4]));
//            jobSimple.setOvertimePunish(Double.parseDouble(split[5]));
//            jobSimple.setCostTotal(Double.parseDouble(split[6]));
//            count++;
//        }
//        scanner.close();
//        int size = 1;
//        for (int i = 1; i <= workerCapacity - 1; i++) {
//            size += C(parcelNum - 1, i);
//        }
//        size *= stationNum * workerNum;
//        for (int i = 0; i < parcelNum; i++) {
//            Set<Integer> parcleIndexSet = new HashSet<>(size);
//            for (JobSimple jobSimple : jobSimpleArray) {
//                if (jobSimple.getParcelIndexSet().contains(i)) {
//                    parcleIndexSet.add(jobSimple.getJobIndex());
//                }
//            }
//            parcelToJob.put(i, parcleIndexSet);
//        }
//
//
////        String[] split0;
////        while (scanner.hasNext()){
////            string = scanner.nextLine();
////            split = string.split(" ");
////            int parcelIndex=Integer.parseInt(split[0]);
////            Set<Integer> jobSet=new HashSet<>();
////            split0=split[1].split(",");
////            for(String s:split0){
////                int i=Integer.parseInt(s);
////                jobSet.add(i);
////            }
////            parcelToJob.put(parcelIndex,jobSet);
////        }
//        InstanceJob instance = new InstanceJob();
//
//        instance.setJobNum(jobNum);
//        instance.setParcelNum(parcelNum);
//        instance.setStationNum(stationNum);
//        instance.setWorkerNum(workerNum);
//        instance.setStationCapacity(stationCapacity);
//        instance.setWorkerCapacity(workerCapacity);
//        instance.setJobSimpleArray(jobSimpleArray);
//        instance.setParcelToJob(parcelToJob);
//
//        instance.setName(file.getName().substring(0, file.getName().length() - 4));
//        return instance;
//    }

    public static InstanceJob readCGInputNew(File file) throws FileNotFoundException {
//        int stationNum;
//        int workerNum;
//        int parcelNum;
//        int jobNum;
//        JobSimple[] jobSimpleArray;
//        Map<Integer, Set<Integer>> parcelToJob = new HashMap<>(10);
//        int stationCapacity;
//        int workerCapacity;
//        String name;
//
//
//        Scanner scanner = new Scanner(file);
//        String string = scanner.nextLine();
//        String[] split = string.split(" ");
//        stationNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        workerNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        parcelNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        jobNum = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        workerCapacity = Integer.parseInt(split[1]);
//        string = scanner.nextLine();
//        split = string.split(" ");
//        stationCapacity = Integer.parseInt(split[1]);
//        scanner.nextLine();
//        int count = 0;
//        jobSimpleArray = new JobSimple[jobNum];
//
//        for (int i = 0; i < parcelNum; i++) {
//            parcelToJob.put(i, new HashSet<>());
//        }
//        while (count < jobNum) {
//            Set<Integer> parcelSet = new HashSet<>();
//            string = scanner.nextLine();
//            split = string.split("\t");
////            String nodeIndex=split[0];
//            String parcleString = split[1];
//            String[] parcleSplit = parcleString.split(",");
//            for (int i = 0; i < parcleSplit.length; i++) {
//                int ind = Integer.parseInt(parcleSplit[i]);
//                parcelSet.add(ind);
//            }
//            JobSimple jobSimple = new JobSimple();
//            jobSimpleArray[count] = jobSimple;
////            jobSimple.setJobIndex(Integer.parseInt(nodeIndex));
//            jobSimple.setParcelIndexSet(parcelSet);
//            jobSimple.setJobIndex(count);
//            jobSimple.setWorkerIndex(Integer.parseInt(split[2]));
//            jobSimple.setStationIndex(Integer.parseInt(split[3]));
//            jobSimple.setDistance(Double.parseDouble(split[4]));
//            jobSimple.setOvertimePunish(Double.parseDouble(split[5]));
//            jobSimple.setCostTotal(Double.parseDouble(split[6]));
//            count++;
//        }
//        scanner.close();
//        int size = 1;
//        for (int i = 1; i <= workerCapacity - 1; i++) {
//            size += C(parcelNum - 1, i);
//        }
//        size *= stationNum * workerNum;
//        for (int i = 0; i < parcelNum; i++) {
//            Set<Integer> parcleIndexSet = new HashSet<>(size);
//            for (JobSimple jobSimple : jobSimpleArray) {
//                if (jobSimple.getParcelIndexSet().contains(i)) {
//                    parcleIndexSet.add(jobSimple.getJobIndex());
//                }
//            }
//            parcelToJob.put(i, parcleIndexSet);
//        }
//
//
////        String[] split0;
////        while (scanner.hasNext()){
////            string = scanner.nextLine();
////            split = string.split(" ");
////            int parcelIndex=Integer.parseInt(split[0]);
////            Set<Integer> jobSet=new HashSet<>();
////            split0=split[1].split(",");
////            for(String s:split0){
////                int i=Integer.parseInt(s);
////                jobSet.add(i);
////            }
////            parcelToJob.put(parcelIndex,jobSet);
////        }
//        InstanceJob instance = new InstanceJob();
//
//        instance.setJobNum(jobNum);
//        instance.setParcelNum(parcelNum);
//        instance.setStationNum(stationNum);
//        instance.setWorkerNum(workerNum);
//        instance.setStationCapacity(stationCapacity);
//        instance.setWorkerCapacity(workerCapacity);
//        instance.setJobSimpleArray(jobSimpleArray);
//        instance.setParcelToJob(parcelToJob);
//
//        instance.setName(file.getName().substring(0, file.getName().length() - 4));
//        return instance;
        return new InstanceJob();
    }

    // 求排列数 A(n,m) n>m
    public static int A(int n, int m) {
        int result = 1;
        // 循环m次,如A(6,2)需要循环2次，6*5
        for (int i = m; i > 0; i--) {
            result *= n;
            n--;// 下一次减一
        }
        return result;
    }

    public static int C(int n, int m)// 应用组合数的互补率简化计算量
    {
        int helf = n / 2;
        if (m > helf) {
//            System.out.print(m + "---->");
            m = n - m;
//            System.out.print(m + "\n");
        }
        // 分子的排列数
        int numerator = A(n, m);
        // 分母的排列数
        int denominator = A(m, m);
        return numerator / denominator;
    }

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("dataset/jobInstance1.txt");
//        InstanceSimple instance=Reader.read(file);
//        System.out.println(instance.getName());
//        InstanceJob instance = Reader.readCGInput(file);
//        System.out.println(instance.getName());
    }
}
