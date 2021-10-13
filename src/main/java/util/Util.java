package util;

import model.InstanceSample;
import model.Parcel;
import model.Station;
import model.Worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 9/15/21 10:44 AM
 */
public class Util {
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

    public static List<List<Integer>> combine(int n, int k,List<List<Integer>> res) {
        if (n <= 0 || k <= 0 || k > n) {
            return res;
        }
        List<Integer> c = new ArrayList<>();
        generateCombinations(n, k, 1, c, res);
        return res;

    }

    /**
     * 回溯求所有组合结果
     *
     * @param n
     * @param k
     * @param start 开始搜索新元素的位置
     * @param c     当前已经找到的组合
     */
    private static void generateCombinations(int n, int k, int start, List<Integer> c, List<List<Integer>> res) {
        if (c.size() == k) {
            //这里需要注意java的值传递
            //此处必须使用重新创建对象的形式，否则 res 列表中存放的都是同一个引用
            res.add(new ArrayList<>(c));
//            for (Integer i : c) {
//                List list = parcelToJob.get(i);
//                list.add(res.size() - 1);
//            }
            return;
        }

        //通过终止条件，进行剪枝优化，避免无效的递归
        //c中还剩 k - c.size()个空位，所以[ i ... n]中至少要有k-c.size()个元素
        //所以i最多为 n - (k - c.size()) + 1
        for (int i = start; i <= n - (k - c.size()) + 1; i++) {
            c.add(i);
            generateCombinations(n, k, i + 1, c, res);
            //记得回溯状态啊
            c.remove(c.size() - 1);
        }
    }

    public static int calTravelTime(int lat1, int lng1, int lat2, int lng2) {

        double distance = Math.sqrt((lat1 - lat2) * (lat1 - lat2) + (lng1 - lng2) * (lng1 - lng2));
        int travelTime = (int) (distance * 20 / (Constants.speed * 1000) * 60);
        if(travelTime==0){
            travelTime=1;
        }

        return travelTime;
    }


    public static String getFeature(Station station, Worker worker, List<Parcel> parcelList,int[][] travelTimeMatrix) {
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

        for(Parcel parcel:parcelList){
            avgTravelTimeS+=travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            avgTravelTimeW += travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            if (maxTravelTimeS < travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                maxTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (minTravelTimeS > travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                minTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (maxTravelTimeW < travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                maxTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (minTravelTimeW > travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                minTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (maxLatP < parcel.getLat()) {
                maxLatP = parcel.getLat();
            }
            if (minLatP > parcel.getLat()) {
                minLatP = parcel.getLat();
            }
            if (maxLngP < parcel.getLng()) {
                maxLngP = parcel.getLng();
            }
            if (minLngP > parcel.getLng()) {
                minLngP = parcel.getLng();
            }
            for(Parcel parcel1:parcelList){
                if(parcel.getIndex()>=parcel1.getIndex()){
                    continue;
                }
                latDiffPsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffPsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countP++;
                latDiffTotalsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffTotalsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countT++;

            }
            latDiffTotalsum += Math.abs(station.getLat() - parcel.getLat());
            lngDiffTotalsum += Math.abs(station.getLng() - parcel.getLng());
            countT++;
            latDiffTotalsum += Math.abs(parcel.getLat()-worker.getLatD());
            lngDiffTotalsum += Math.abs(parcel.getLng()-worker.getLngD());
            countT++;
        }
        latDiffTotalsum += Math.abs(station.getLat()-worker.getLatD());
        lngDiffTotalsum += Math.abs(station.getLng()-worker.getLngD());
        countT++;

        avgTravelTimeW = avgTravelTimeW / parcelList.size();
        avgTravelTimeS = avgTravelTimeS / parcelList.size();

        maxLatTotal = Math.max(maxLatP, station.getLat());
        maxLatTotal = Math.max(maxLatTotal, worker.getLatD());
        maxLngTotal = Math.max(maxLngP, station.getLng());
        maxLngTotal = Math.max(maxLngTotal, worker.getLngD());
        minLatTotal = Math.min(minLatP, station.getLat());
        minLatTotal = Math.min(minLatTotal, worker.getLatD());
        minLngTotal = Math.min(minLngP, station.getLng());
        minLngTotal = Math.min(minLngTotal, worker.getLngD());

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

        StringBuilder stringBuilder=new StringBuilder();

        stringBuilder.append("1:"+avgTravelTimeS+"\t");
        stringBuilder.append("2:"+avgTravelTimeW+"\t");
        stringBuilder.append("3:"+maxTravelTimeS+"\t");
        stringBuilder.append("4:"+maxTravelTimeW+"\t");
        stringBuilder.append("5:"+minTravelTimeS+"\t");
        stringBuilder.append("6:"+minTravelTimeW+"\t");
        stringBuilder.append("7:"+areaP+"\t");
        stringBuilder.append("8:"+areaTotal+"\t");
        stringBuilder.append("9:"+maxLatDiffP+"\t");
        stringBuilder.append("10:"+maxLngDiffP+"\t");
        stringBuilder.append("11:"+maxLatDiffTotal+"\t");
        stringBuilder.append("12:"+maxLngDiffTotal+"\t");
        stringBuilder.append("13:"+avgLatDiffP+"\t");
        stringBuilder.append("14:"+avgLngDiffP+"\t");
        stringBuilder.append("15:"+avgLatDiffTotal+"\t");
        stringBuilder.append("16:"+avgLngDiffTotal+"\n");

        return stringBuilder.toString();
    }

    public static float[] getFeature0(Station station, Worker worker, List<Parcel> parcelList,int[][] travelTimeMatrix) {
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

        for(Parcel parcel:parcelList){
            avgTravelTimeS+=travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            avgTravelTimeW += travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            if (maxTravelTimeS < travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                maxTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (minTravelTimeS > travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()]) {
                minTravelTimeS = travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
            }
            if (maxTravelTimeW < travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                maxTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (minTravelTimeW > travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()]) {
                minTravelTimeW = travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
            }
            if (maxLatP < parcel.getLat()) {
                maxLatP = parcel.getLat();
            }
            if (minLatP > parcel.getLat()) {
                minLatP = parcel.getLat();
            }
            if (maxLngP < parcel.getLng()) {
                maxLngP = parcel.getLng();
            }
            if (minLngP > parcel.getLng()) {
                minLngP = parcel.getLng();
            }
            for(Parcel parcel1:parcelList){
                if(parcel.getIndex()>=parcel1.getIndex()){
                    continue;
                }
                latDiffPsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffPsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countP++;
                latDiffTotalsum += Math.abs(parcel.getLat() - parcel1.getLat());
                lngDiffTotalsum += Math.abs(parcel.getLng() - parcel1.getLng());
                countT++;

            }
            latDiffTotalsum += Math.abs(station.getLat() - parcel.getLat());
            lngDiffTotalsum += Math.abs(station.getLng() - parcel.getLng());
            countT++;
            latDiffTotalsum += Math.abs(parcel.getLat()-worker.getLatD());
            lngDiffTotalsum += Math.abs(parcel.getLng()-worker.getLngD());
            countT++;
        }
        latDiffTotalsum += Math.abs(station.getLat()-worker.getLatD());
        lngDiffTotalsum += Math.abs(station.getLng()-worker.getLngD());
        countT++;

        avgTravelTimeW = avgTravelTimeW / parcelList.size();
        avgTravelTimeS = avgTravelTimeS / parcelList.size();

        maxLatTotal = Math.max(maxLatP, station.getLat());
        maxLatTotal = Math.max(maxLatTotal, worker.getLatD());
        maxLngTotal = Math.max(maxLngP, station.getLng());
        maxLngTotal = Math.max(maxLngTotal, worker.getLngD());
        minLatTotal = Math.min(minLatP, station.getLat());
        minLatTotal = Math.min(minLatTotal, worker.getLatD());
        minLngTotal = Math.min(minLngP, station.getLng());
        minLngTotal = Math.min(minLngTotal, worker.getLngD());

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

        float []feature=new float[]{avgTravelTimeS, avgTravelTimeW, maxTravelTimeS , maxTravelTimeW, minTravelTimeS, minTravelTimeW, areaP, areaTotal, maxLatDiffP, maxLngDiffP, maxLatDiffTotal, maxLngDiffTotal, avgLatDiffP, avgLngDiffP, avgLatDiffTotal, avgLngDiffTotal};

        return feature;
    }

    public static double calTravelTime(Worker worker,Station station,Parcel parcel,int[][] travelTimeMatrix){
        double travelTime=0;
        travelTime+=travelTimeMatrix[worker.getIndexO()][station.getNodeIndex()];
        travelTime+=travelTimeMatrix[station.getNodeIndex()][parcel.getNodeIndex()];
        travelTime+=travelTimeMatrix[parcel.getNodeIndex()][worker.getIndexD()];
        return travelTime;

    }
    public static boolean doubleToBoolean(double value) {
        if (Math.abs(1.0D - value) < Constants.EPSILON) {
            return true;
        } else if (Math.abs(value) < Constants.EPSILON) {
            return false;
        } else {
            throw new RuntimeException("Failed to convert to boolean, not near zero or one: " + value);
        }
    }
}
