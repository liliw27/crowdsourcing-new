package instanceGen;

import model.Instance;
import model.Parcel;
import model.Station;
import model.Worker;
import util.Constants;
import util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:58 AM
 * @date 2021/9/12 20:32
 */
public class InstanceGenerator {
    static int[] lng;
    static int[] lat;
    static int totalNodeNum;
    static int timeHorizon;


    public static void read(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        String string = scanner.nextLine();
        String[] split = string.split("\t");
        totalNodeNum = Integer.parseInt(split[1]);
        lng = new int[totalNodeNum];
        lat = new int[totalNodeNum];
        scanner.nextLine();
        scanner.nextLine();
        scanner.nextLine();
        int index = 0;
        while (index < totalNodeNum) {
            string = scanner.nextLine();
            split = string.split("\t");
            lat[index] = Integer.parseInt(split[1]);
            lng[index] = Integer.parseInt(split[2]);
            index++;
        }
    }

    public static Instance generateInstance() {
        List<Worker> workers = new ArrayList<>();
        List<Parcel> parcels = new ArrayList<>();
        List<Station> stations = new ArrayList<>();
        Random random = new Random(0);
        Scanner sc = new Scanner(System.in);
        int stationNum;
        do {
            System.out.println(" Please Enter The Number of Station(<=4):");
            stationNum = sc.nextInt();
        } while (stationNum >4);
        System.out.println(" Please Enter The Station Capacity(recommended 500):");
        int stationCapacity = sc.nextInt();
        System.out.println(" Please Enter The Worker Capacity(recommended 3):");
        int workerCapacity = sc.nextInt();
        System.out.println(" Please Enter The Time Horizon(recommended 780):");
        int timeHorizon = sc.nextInt();

        //读取整型输入
        int[][] coordinate = getStationCor(stationNum);
        for (int i = 0; i < stationNum; i++) {
            Station station = new Station();
            station.setLat(coordinate[i][0]);
            station.setLng(coordinate[i][1]);
            station.setCurrentCapRemained(stationCapacity);
            stations.add(station);
        }
        System.out.println(" Please Enter The average parcels per driver:");
        double avgParcel = sc.nextDouble();    //读取输入
        int workNum = (int) (totalNodeNum / (avgParcel + 2));
        Set<Integer> indexset = new HashSet<>();
        while (workers.size() < workNum) {
            int indexO = random.nextInt(totalNodeNum);
            if (indexset.contains(indexO)) {
                continue;
            }
            int indexD = random.nextInt(totalNodeNum);
            if(indexD==indexO){
                continue;
            }
            if (indexset.contains(indexD)) {
                continue;
            }
            double travelTime= Util.calTravelTime(lat[indexO],lng[indexO],lat[indexD],lng[indexD]);
//            double distance = Math.sqrt((lat[indexO]-lat[indexD]) * (lat[indexO]-lat[indexD]) + (lng[indexO]-lng[indexD]) * (lng[indexO]-lng[indexD]));
//            double travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);//min on 20km*20km area
            if (travelTime < 6) {
                continue;
            }
//            System.out.println("travel time: " + travelTime+"\tdistance: " + distance);

            Worker worker = new Worker();
            worker.setLatO(lat[indexO]);
            worker.setLngO(lng[indexO]);
            worker.setLatD(lat[indexD]);
            worker.setLngD(lng[indexD]);
            int latestArrival = 60 + random.nextInt(720);

            int earliestDeparture = latestArrival - Math.max((int)travelTime + 30, (int) (2 * travelTime));

            int drivingTimeMax = (int) (2 * travelTime);
            worker.setDrivingTimeMax(drivingTimeMax);
            worker.setEarliestDeparture(earliestDeparture);
            worker.setLatestArrival(latestArrival);
            workers.add(worker);
            indexset.add(indexO);
            indexset.add(indexD);
        }
        for (int i = 0; i < totalNodeNum; i++) {
            if (indexset.contains(i)) {
                continue;
            }
            int deadline = 60 + random.nextInt(720);
            Parcel parcel = new Parcel();
            parcel.setDeadline(deadline);
            parcel.setLat(lat[i]);
            parcel.setLng(lng[i]);
            parcels.add(parcel);
        }
        Instance instance = new Instance();
        instance.setName("S" + stationNum + "_W" + workNum + "_P" + parcels.size());
        instance.setParcels(parcels);
        instance.setStations(stations);
        instance.setWorkers(workers);
        instance.setStationCapacity(stationCapacity);
        instance.setWorkerCapacity(workerCapacity);
        instance.setTimeHorizon(timeHorizon);
        return instance;
    }

    private static int[][] getStationCor(int stationNum) {
        int[][] coordinate = new int[stationNum][2];
        switch (stationNum) {
            case 2:
                coordinate[0] = new int[]{200, 400};
                coordinate[1] = new int[]{600, 800};
                return coordinate;
            case 3:
                coordinate[0] = new int[]{300, 300};
                coordinate[1] = new int[]{650, 300};
                coordinate[2] = new int[]{450, 650};
                return coordinate;
            case 4:
                coordinate[0] = new int[]{250, 250};
                coordinate[1] = new int[]{750, 750};
                coordinate[2] = new int[]{250, 750};
                coordinate[3] = new int[]{750, 250};
                return coordinate;
            default:
                coordinate[0] = new int[]{500, 500};
                return coordinate;
        }

    }

    public static void writeInstance(Instance instance) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("./dataset/instance/" + instance.getName() + ".txt"));
            writer.write(instance.toString());
            writer.flush();
        } catch (IOException e) {
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        File file=new File("dataset/X-n670-k130.vrp");
        InstanceGenerator.read(file);
        Instance instance=InstanceGenerator.generateInstance();
        InstanceGenerator.writeInstance(instance);
    }

}
