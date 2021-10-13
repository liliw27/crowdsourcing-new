package xgoostTraining.sample;

import MIP.mipJob.JobMip;
import ilog.concert.IloException;
import io.Reader;
import model.Instance;
import model.InstanceSample;
import model.Job;
import model.Parcel;
import model.Station;
import model.Worker;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import util.Constants;
import util.Util;
import xgoostTraining.sample.Mip.MipS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:19 AM
 */
public class SampleGeneration {

   public static double[][] varValues;
   public static double[] timeVarValues;
   public static boolean isFeasible;
    public static void sampleGen(Instance instance, int sampleNum) throws IloException, IOException {
//        Scanner sc = new Scanner(System.in);
//        System.out.println(" Please Enter how many samples do you want:");
//        int sampleNum = sc.nextInt();
        double[] ratio = getRatio(instance);// double[instance.getWorkerCapacity() ];the percentage of job with 2,3,...instance.getWorkerCapacity() parcels
        Random random = new Random(57);

        for (int i = 0; i < sampleNum; i++) {
            Job job = getRandomJob(instance, random, ratio);
            InstanceSample instanceSample = job2InstanceSample(job,instance);

            double obj = solveModel(instanceSample, i);

            /*summarize the features*/
            String features = getFeature(instanceSample);
            String s = obj + "\t" + features;
            BufferedWriter bf = new BufferedWriter(new FileWriter("dataset/test.svm.txt", true));
            bf.write(s);
            bf.close();

        }


    }

    public static String getFeature(InstanceSample instanceSample) {
        int avgTravelTimeS = 0;
        int avgTravelTimeW = 0;
        int maxTravelTimeS = 0;
        int maxTravelTimeW = 0;
        int minTravelTimeS = Integer.MAX_VALUE;
        int minTravelTimeW = Integer.MAX_VALUE;
        int areaP;
        int areaTotal;
        int maxLatDiffP;
        int maxLngDiffP;
        int maxLatDiffTotal;
        int maxLngDiffTotal;
        int avgLatDiffP;
        int avgLngDiffP;
        int avgLatDiffTotal;
        int avgLngDiffTotal;


        int destinationIndex = instanceSample.getTravelTimeMatrix().length - 1;
        int maxLatP = 0;
        int minLatP = Integer.MAX_VALUE;
        int maxLngP = 0;
        int minLngP = Integer.MAX_VALUE;
        int maxLatTotal;
        int minLatTotal;
        int maxLngTotal;
        int minLngTotal;
        int latDiffPsum = 0;
        int lngDiffPsum = 0;
        int countP = 0;
        int latDiffTotalsum = 0;
        int lngDiffTotalsum = 0;
        int countT = 0;
        for (int j = 0; j < instanceSample.getTravelTimeMatrix().length; j++) {
            if (j != 0 && j != destinationIndex) {
                avgTravelTimeS += instanceSample.getTravelTimeMatrix()[0][j];
                avgTravelTimeW += instanceSample.getTravelTimeMatrix()[j][destinationIndex];
                if (maxTravelTimeS < instanceSample.getTravelTimeMatrix()[0][j]) {
                    maxTravelTimeS = instanceSample.getTravelTimeMatrix()[0][j];
                }
                if (minTravelTimeS > instanceSample.getTravelTimeMatrix()[0][j]) {
                    minTravelTimeS = instanceSample.getTravelTimeMatrix()[0][j];
                }
                if (maxTravelTimeW < instanceSample.getTravelTimeMatrix()[j][destinationIndex]) {
                    maxTravelTimeW = instanceSample.getTravelTimeMatrix()[j][destinationIndex];
                }
                if (minTravelTimeW > instanceSample.getTravelTimeMatrix()[j][destinationIndex]) {
                    minTravelTimeW = instanceSample.getTravelTimeMatrix()[j][destinationIndex];
                }
                if (maxLatP < instanceSample.getCoordinate()[j][0]) {
                    maxLatP = instanceSample.getCoordinate()[j][0];
                }
                if (minLatP > instanceSample.getCoordinate()[j][0]) {
                    minLatP = instanceSample.getCoordinate()[j][0];
                }
                if (maxLngP < instanceSample.getCoordinate()[j][1]) {
                    maxLngP = instanceSample.getCoordinate()[j][1];
                }
                if (minLngP > instanceSample.getCoordinate()[j][1]) {
                    minLngP = instanceSample.getCoordinate()[j][1];
                }
            }
            for (int k = j + 1; k < instanceSample.getTravelTimeMatrix().length; k++) {
                if (j != 0 && j != destinationIndex && k != 0 && k != destinationIndex) {
                    latDiffPsum += Math.abs(instanceSample.getCoordinate()[j][0] - instanceSample.getCoordinate()[k][0]);
                    lngDiffPsum += Math.abs(instanceSample.getCoordinate()[j][1] - instanceSample.getCoordinate()[k][1]);
                    countP++;
                }
                latDiffTotalsum += Math.abs(instanceSample.getCoordinate()[j][0] - instanceSample.getCoordinate()[k][0]);
                lngDiffTotalsum += Math.abs(instanceSample.getCoordinate()[j][1] - instanceSample.getCoordinate()[k][1]);
                countT++;
            }


        }
        avgTravelTimeW = avgTravelTimeW / instanceSample.getParcelNum();
        avgTravelTimeS = avgTravelTimeS / instanceSample.getParcelNum();

        maxLatTotal = Math.max(maxLatP, instanceSample.getCoordinate()[0][0]);
        maxLatTotal = Math.max(maxLatTotal, instanceSample.getCoordinate()[destinationIndex][0]);
        maxLngTotal = Math.max(maxLngP, instanceSample.getCoordinate()[0][1]);
        maxLngTotal = Math.max(maxLngTotal, instanceSample.getCoordinate()[destinationIndex][1]);
        minLatTotal = Math.min(minLatP, instanceSample.getCoordinate()[0][0]);
        minLatTotal = Math.min(minLatTotal, instanceSample.getCoordinate()[destinationIndex][0]);
        minLngTotal = Math.min(minLngP, instanceSample.getCoordinate()[0][1]);
        minLngTotal = Math.min(minLngTotal, instanceSample.getCoordinate()[destinationIndex][1]);

        maxLatDiffP = maxLatP - minLatP;
        maxLngDiffP = maxLngP - minLngP;
        maxLatDiffTotal = maxLatTotal - minLatTotal;
        maxLngDiffTotal = maxLngTotal - minLngTotal;
        areaP = maxLatDiffP * maxLngDiffP;
        areaTotal = maxLatDiffTotal * maxLngDiffTotal;
        avgLatDiffP = latDiffPsum / countP;
        avgLngDiffP = lngDiffPsum / countP;
        avgLatDiffTotal = latDiffTotalsum / countT;
        avgLngDiffTotal = lngDiffTotalsum / countT;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("1:" + avgTravelTimeS + "\t");
        stringBuilder.append("2:" + avgTravelTimeW + "\t");
        stringBuilder.append("3:" + maxTravelTimeS + "\t");
        stringBuilder.append("4:" + maxTravelTimeW + "\t");
        stringBuilder.append("5:" + minTravelTimeS + "\t");
        stringBuilder.append("6:" + minTravelTimeW + "\t");
        stringBuilder.append("7:" + areaP + "\t");
        stringBuilder.append("8:" + areaTotal + "\t");
        stringBuilder.append("9:" + maxLatDiffP + "\t");
        stringBuilder.append("10:" + maxLngDiffP + "\t");
        stringBuilder.append("11:" + maxLatDiffTotal + "\t");
        stringBuilder.append("12:" + maxLngDiffTotal + "\t");
        stringBuilder.append("13:" + avgLatDiffP + "\t");
        stringBuilder.append("14:" + avgLngDiffP + "\t");
        stringBuilder.append("15:" + avgLatDiffTotal + "\t");
        stringBuilder.append("16:" + avgLngDiffTotal + "\n");

        return stringBuilder.toString();
    }

    public static double solveModel(InstanceSample instanceSample, int i) throws IloException {
        MipS mip = new MipS(instanceSample,false);
        long runTime = System.currentTimeMillis();
//        System.out.println("Starting branch and bound for sample" + i);
        mip.solve();
        runTime = System.currentTimeMillis() - runTime;

        if (mip.isFeasible()) {
//            System.out.println("Objective: " + mip.getObjectiveValue());
//            System.out.println("Runtime: " + runTime);
//            System.out.println("Is optimal: " + mip.isOptimal());
//            System.out.println("Bound: " + mip.getLowerBound());
//            System.out.println("Nodes: " + mip.getNrOfNodes());

        } else {
            System.out.println("MIP infeasible!");
        }

        varValues = mip.getVarValues();
        return mip.getObjectiveValue();

    }

    public static double solveModel2(InstanceSample instanceSample, int i) throws IloException {
        MipS mip = new MipS(instanceSample,true);
        long runTime = System.currentTimeMillis();
        mip.solve();

        if (mip.isFeasible()) {
            isFeasible=true;
            varValues = mip.getVarValues();
            timeVarValues=mip.getTimeVarValues();
        } else {
            isFeasible=false;
        }

        return mip.getObjectiveValue();

    }

    public static InstanceSample job2InstanceSample(Job job,Instance instance) {
        InstanceSample instanceSample = new InstanceSample();
        int parcelNum = job.getParcelList().size();
        int[][] travelTimeMatrix = new int[parcelNum + 2][parcelNum + 2];
        int[][] coordinate = new int[parcelNum + 2][2];
        for (int i = 0; i < travelTimeMatrix.length; i++) {
            travelTimeMatrix[i][i] = 0;
        }
        Station station = job.getStation();
        Worker worker = job.getWorker();
        coordinate[0][0] = station.getLat();
        coordinate[0][1] = station.getLng();
        coordinate[parcelNum + 1][0] = worker.getLatD();
        coordinate[parcelNum + 1][1] = worker.getLngD();
        List<Parcel> parcels = job.getParcelList();
        int travelTime;

        for (int i = 0; i < parcels.size(); i++) {
            Parcel parcel = parcels.get(i);
            coordinate[i + 1][0] = parcel.getLat();
            coordinate[i + 1][1] = parcel.getLng();
//            travelTime = Util.calTravelTime(parcel.getLat(), parcel.getLng(), station.getLat(), station.getLng());
//            distance = Math.sqrt((parcel.getLat() - station.getLat()) * (parcel.getLat() - station.getLat()) + (parcel.getLng() - station.getLng()) * (parcel.getLng() - station.getLng()));
//            travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
            travelTime=instance.getTravelTimeMatrix()[station.getNodeIndex()][parcel.getNodeIndex()];
            travelTimeMatrix[0][i + 1] = travelTime;
            travelTimeMatrix[i + 1][0] = travelTime;
//            travelTime = Util.calTravelTime(parcel.getLat(), parcel.getLng(), worker.getLatD(), worker.getLngD());

//            distance = Math.sqrt((parcel.getLat() - worker.getLatD()) * (parcel.getLat() - worker.getLatD()) + (parcel.getLng() - worker.getLngD()) * (parcel.getLng() - worker.getLngD()));
//            travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
            travelTime=instance.getTravelTimeMatrix()[parcel.getNodeIndex()][worker.getIndexD()];
            travelTimeMatrix[parcelNum + 1][i + 1] = travelTime;
            travelTimeMatrix[i + 1][parcelNum + 1] = travelTime;

            for (int i0 = i + 1; i0 < parcels.size(); i0++) {
                Parcel parcel0 = parcels.get(i0);
                travelTime=instance.getTravelTimeMatrix()[parcel.getNodeIndex()][parcel0.getNodeIndex()];
//                travelTime = Util.calTravelTime(parcel.getLat(), parcel.getLng(), parcel0.getLat(), parcel0.getLng());
//                distance = Math.sqrt((parcel.getLat() - parcel0.getLat()) * (parcel.getLat() - parcel0.getLat()) + (parcel.getLng() - parcel0.getLng()) * (parcel.getLng() - parcel0.getLng()));
//                travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
                travelTimeMatrix[i + 1][i0 + 1] = travelTime;
                travelTimeMatrix[i0 + 1][i + 1] = travelTime;
            }

        }
        instanceSample.setParcelNum(parcelNum);
        instanceSample.setTravelTimeMatrix(travelTimeMatrix);
        instanceSample.setCoordinate(coordinate);

        DirectedGraph<Integer, DefaultWeightedEdge> routingGraph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                Graphs.addEdgeWithVertices(routingGraph, i, j, travelTimeMatrix[i][j]);
            }
        }
        instanceSample.setRoutingGraph(routingGraph);

        int[] timeLimit=new int[parcelNum+2];
        timeLimit[0]=worker.getEarliestDeparture()+instance.getTravelTimeMatrix()[worker.getIndexO()][station.getNodeIndex()];
        for(int i=1;i<parcelNum+1;i++){
            timeLimit[i]=parcels.get(i-1).getDeadline();
        }
        timeLimit[parcelNum+1]=worker.getLatestArrival();
        instanceSample.setTimeLimit(timeLimit);
        return instanceSample;
    }

    private static double[] getRatio(Instance instance) {
        double[] ratio = new double[instance.getWorkerCapacity()];

        int[] jobNumCombine = new int[instance.getWorkerCapacity()];
        for (int i = 0; i < jobNumCombine.length; i++) {
            if (i == 0) {
                jobNumCombine[i] = 0;
            } else {
                jobNumCombine[i] = jobNumCombine[i - 1] + Util.C(instance.getParcels().size(), i + 1);

            }

        }
        for (int i = 0; i < ratio.length; i++) {
            ratio[i] = (jobNumCombine[i] * 1.0) / jobNumCombine[jobNumCombine.length - 1];
        }
        return ratio;
    }

    private static Job getRandomJob(Instance instance, Random random, double[] ratio) {
        List<Parcel> parcelSetIndex = new ArrayList<>();
        int stationIdx = random.nextInt(instance.getStations().size());
        Station station = instance.getStations().get(stationIdx);
        int workerIdx = random.nextInt(instance.getWorkers().size());
        Worker worker = instance.getWorkers().get(workerIdx);
        int parcelNumInJob = 0;
        double r = random.nextDouble();
        for (int j = 1; j < ratio.length; j++) {
            if (r >= ratio[j - 1] && r < ratio[j]) {
                parcelNumInJob = j + 1;
                break;
            }
        }
        System.out.println("=======parcelNumInJob=======:" + parcelNumInJob);
        while (parcelSetIndex.size() < parcelNumInJob) {
            int index = random.nextInt(instance.getParcels().size());
            parcelSetIndex.add(instance.getParcels().get(index));
        }
        Job job = new Job();
        job.setParcelList(parcelSetIndex);
        job.setWorker(worker);
        job.setStation(station);
        return job;
    }

    public static void main(String[] args) throws IOException, IloException {
        File file = new File("dataset/instance/S3_W191_P288.txt");
        Instance instance = Reader.readInstance(file);
        long runTime = System.currentTimeMillis();
        SampleGeneration.sampleGen(instance, 500000);

        runTime = System.currentTimeMillis() - runTime;
        System.out.println("*******runtime*******:" + runTime);
    }
}
