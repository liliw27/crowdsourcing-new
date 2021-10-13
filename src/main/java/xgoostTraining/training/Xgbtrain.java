package xgoostTraining.training;

import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 2021/9/16 20:47
 */
public class Xgbtrain {


    public static void trainAndPredict() throws XGBoostError {
        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("eta", 0.1);
                put("max_depth", 6);
//            put("silent", 1);
                put("objective", "reg:squarederror");
                put("eval_metric","mae");
            }
        };
        DMatrix trainMat = new DMatrix("dataset/train.svm.txt");
        DMatrix testMat = new DMatrix("dataset/test.svm.txt");
        // Specify a watch list to see model accuracy on data sets
        Map<String, DMatrix> watches = new HashMap<String, DMatrix>() {
            {
                put("train", trainMat);
                put("test", testMat);
            }
        };
        int nround = 500;
        Booster booster = XGBoost.train(trainMat, params, nround, watches, null, null);
        booster.saveModel("model.bin");

        DMatrix dtest = new DMatrix("dataset/test.svm.txt");
// predict
        float[][] predicts = booster.predict(dtest);
// predict leaf
        float[][] leafPredicts = booster.predictLeaf(dtest, 0);
    }

    public static void main(String[] args) throws XGBoostError {
        Xgbtrain.trainAndPredict();
//        Booster booster = XGBoost.loadModel("model.bin");
//        DMatrix dtest = new DMatrix("dataset/test.svm.txt");
//// predict
//        float[][] predicts = booster.predict(dtest);
//        System.out.println(predicts[100][0]);
//
//// predict leaf
//        float[][] leafPredicts = booster.predictLeaf(dtest, 0);
    }

}
