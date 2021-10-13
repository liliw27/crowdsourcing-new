package instanceGen;

import io.Writer;
import util.Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 8/14/21 2:30 PM
 */
public class JobInstanceGenerator {
    private double[][] cost;

    private int stationNum;
    private int workerNum;
    private int parcelNum;
    private int jobNum;
    private int workerCapacity;
    private int stationCapacity;


    private Random random = new Random(0);

    public void gen(int No) throws IOException {
        stationNum = 3;
        workerNum = 50;
        parcelNum = 100;
        workerCapacity = 3;
        stationCapacity = 50;
        for (int i = 1; i < workerCapacity + 1; i++) {
            jobNum += Util.C(parcelNum, i);
        }
        cost = new double[workerNum][jobNum];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stationNum " + stationNum + "\n");
        stringBuilder.append("workerNum " + workerNum + "\n");
        stringBuilder.append("parcelNum " + parcelNum + "\n");
        stringBuilder.append("jobNum " + jobNum * workerNum * stationNum + "\n");
        stringBuilder.append("workerCapacity " + workerCapacity + "\n");
        stringBuilder.append("stationCapacity " + stationCapacity + "\n");
        stringBuilder.append("parcelIndexSet    workerIndex stationIndex    distanceCost    otPunish    costTotal\n");
        BufferedWriter bf = new BufferedWriter(new FileWriter("./dataset/" + No + ".txt"));
        List<List<Integer>> res = new ArrayList<>();
        Map<Integer, List<Integer>> parcelToJob = new HashMap<>();
        for (int i = 0; i < parcelNum; i++) {
            parcelToJob.put(i, new LinkedList<>());
        }


        for (int i = 0; i <= workerCapacity; i++) {
            Util.combine(parcelNum, i,res);
        }
//        for (int j = 0; j < workerNum; j++) {
//            stringBuilder = new StringBuilder();
//            for (int k = 0; k < jobNum; k++) {
//
//                travelTime[j][k] = random.nextDouble() * 100;
//                stringBuilder.append(travelTime[j][k] + "\t");
//            }
//
//            bf.write(stringBuilder.toString());
//
//            bf.newLine();
//
//            bf.flush();
//        }
        int index = 0;

            for (int i = 0; i < stationNum; i++) {
                for (int j = 0; j < workerNum; j++) {
                    for (int k = 0; k < jobNum; k++) {
                    List<Integer> ParcelList = res.get(k);
                    stringBuilder.append(index + "\t");
                    for (int ind : ParcelList) {
                        stringBuilder.append(ind - 1 + ",");
                        parcelToJob.get(ind - 1).add(index);
                    }
                    stringBuilder.append("\t" + j + "\t");
                    stringBuilder.append(i + "\t");
                    double distance = random.nextDouble() * 100;
                    stringBuilder.append(distance + "\t");
                    double punish = 0;
                    stringBuilder.append(punish + "\t");
                    double costTotal = distance + punish;
                    stringBuilder.append(costTotal + "\n");
                        System.out.println(index);
                    index++;
                }
            }
        }
//        for (Integer integer : parcelToJob.keySet()) {
//            stringBuilder.append(integer + " ");
//            for (int i : parcelToJob.get(integer)) {
//                stringBuilder.append(i + ",");
//            }
//            stringBuilder.append("\n");
//        }
        Writer.writeInstance(stringBuilder.toString(), "jobInstance" + No);
    }


    public static void main(String[] args) throws IOException {
        JobInstanceGenerator instanceGenerator = new JobInstanceGenerator();
        instanceGenerator.gen(8);
    }

//    public static void main(String[] args) {
//        int m = A(50, 10);
//        int n = C(200, 3) + C(200, 2) + 200;
//
//        List<List<Integer>> res = new ArrayList<>();
//        Map<Integer, List<Integer>> parcelToJob = new HashMap<>();
//        for (int i = 1; i <= 200; i++) {
//            parcelToJob.put(i, new LinkedList<>());
//        }
//        for (int i = 1; i < 4; i++) {
//            combine(200, i, res, parcelToJob);
//        }
//        System.out.println(res.toString());
//        int a = 0;
//    }




//    public static void main(String[] args) {
//        int[] num=new int[100];
//        for(int i=0;i<num.length;i++){
//            num[i]=i+1;
//        }
//        String str="";
//        //求3个数的组合个数
////        count(0,str,num,3);
////        求1-n个数的组合个数
//        count1(0,str,num);
//    }
//
//    private static void count1(int i, String str, int[] num) {
//        if(i==num.length){
//            System.out.println(str);
//            return;
//        }
//        count1(i+1,str,num);
//        count1(i+1,str+num[i]+",",num);
//    }

//    public static void main(String[] args) {
//        InstanceSimpleGenerator instanceGenerator = new InstanceSimpleGenerator();
//        instanceGenerator.gen(7);
//    }
}
