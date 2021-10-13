package instanceGen;

import io.Writer;

import java.util.Random;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:59 AM
 */
public class InstanceSimpleGenerator {
    private double[][][] cost;

    private int stationNum = 3;
    private int workerNum = 10000;
    private int parcelNum = 1000;

    private Random random = new Random(0);

    public void gen(int No) {
        cost = new double[stationNum][workerNum][parcelNum];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stationNum "+stationNum+"\n");
        stringBuilder.append("workerNum "+workerNum+"\n");
        stringBuilder.append("parcelNum "+parcelNum+"\n");

        for (int i = 0; i < stationNum; i++) {
            for (int j = 0; j < workerNum; j++) {
                for (int k = 0; k < parcelNum; k++) {
                    cost[i][j][k] = random.nextDouble() * 100;
                   stringBuilder.append(String.format("%.2f", cost[i][j][k]) + " ");
                }
//                stringBuilder.trimToSize();
                stringBuilder.append("\n");
            }
        }
        Writer.writeInstance(stringBuilder.toString(), "instance_"+No);
    }

    public static void main(String[] args) {
        InstanceSimpleGenerator instanceSimpleGenerator = new InstanceSimpleGenerator();
        instanceSimpleGenerator.gen(7);
    }
}
