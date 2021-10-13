package rollingHorizon;

import MIP.mipJob.JobMip;
import cg.CrowdsourcingCGSolver;
import cg.model.Crowdsourcing;
import cg.model.JobReduced;
import ilog.concert.IloException;
import io.Reader;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import model.Instance;
import model.InstanceJob;
import model.InstanceSample;
import model.Job;
import model.Parcel;
import model.Solution;
import model.Station;
import model.Worker;
import org.apache.commons.lang3.tuple.Pair;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import util.Constants;
import util.TopK;
import util.Util;
import xgoostTraining.sample.SampleGeneration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static xgoostTraining.sample.SampleGeneration.job2InstanceSample;


/**
 * @author Wang Li
 * @description
 * @date 2021/9/17 15:26
 */
public class RollingTimeHorizon {

    public void mainIteration(Instance instance) throws IOException, XGBoostError, TimeLimitExceededException, IloException {
        Set<Parcel> parcelTotal = new HashSet<>(instance.getParcels());
        Set<Worker> workerTotal = new HashSet<>(instance.getWorkers());
        int timePeriodsNum = instance.getTimeHorizon() / Constants.deltaT;
        int startT = 60;
        boolean isTotalJob = true;
        long runtime = System.currentTimeMillis();

        for (int it = 0; it < timePeriodsNum - 1; it++) {
            int periodStart = startT + it * Constants.deltaT;
            int periodEnd = periodStart + 2 * Constants.deltaT;
            System.out.println("----------iteration:" + it + ":" + periodStart + "~" + periodEnd + "---------");
            List<Parcel> parcelList = getParcelList(parcelTotal, periodEnd);
            System.out.println("parcel set size:" + parcelList.size());
            Set<Worker> workerSet = getWorkerSet(workerTotal, periodEnd);
            System.out.println("worker set size:" + workerSet.size());

            long runTime = System.currentTimeMillis();

            Set<Pair<Station, Worker>> pairSWSet = getAvailableSWPair(instance.getStations(), workerSet, instance.getTravelTimeMatrix());
            Map<Pair<Station, Worker>, Set<Parcel>> unavailableSWPMap = getUnavailablePairSWPMap(pairSWSet, parcelList, instance.getTravelTimeMatrix());
            runTime = System.currentTimeMillis() - runTime;
            System.out.println(" run availble pair:" + runTime);
            runTime = System.currentTimeMillis();
            Set<Job> jobSet = generateMipJob(parcelList, pairSWSet, unavailableSWPMap, instance);
            runTime = System.currentTimeMillis() - runTime;
            System.out.println(" run generateJob:" + runTime);
            runTime = System.currentTimeMillis();
            Map<Parcel, Set<Job>> parcelToJob = getParcelToJob(parcelList, jobSet);
            runTime = System.currentTimeMillis() - runTime;
            System.out.println(" run getParcelToJob:" + runTime);
            runTime = System.currentTimeMillis();
            System.out.println("***********jobNum:" + jobSet.size() + "***************");
            InstanceJob instanceJob = genInstanceJob(parcelToJob, jobSet, workerSet, parcelList, instance.getStations(), it, instance.getWorkerCapacity());
//            CrowdsourcingCGSolver cgSolver = cgen(instanceJob);
            runTime = System.currentTimeMillis() - runTime;
            System.out.println(" run cgen:" + runTime);
            runTime = System.currentTimeMillis();
            System.out.println("&&&&&&&&&&&&&&&column number cg:" + instanceJob.getJobMipList().size() + "&&&&&&&&&&&&&&&&&&&&&");
            Solution solution = mip(instanceJob, isTotalJob);
//            Solution solution = mipIter(instanceJob, cgSolver.getDualCostsMap(), cgSolver.getObjectiveValue());
            runTime = System.currentTimeMillis() - runTime;
            System.out.println(" run mipIter:" + runTime);
            double objDiff = validateObjDiff(solution, instance, it);
            System.out.println("@@@@@@@@@@@@solution diff:" + objDiff + "@@@@@@@@@@@@");
            reduceParcelWorker(parcelTotal, workerTotal, solution, periodEnd - Constants.deltaT);

        }
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        System.out.println("现在时间：" + sdf.format(date)); // 输出已经格式化的现在时间（24小时制）
        System.out.println("total time: " + (System.currentTimeMillis() - runtime));
    }

    private double validateObjDiff(Solution solution, Instance instance, int it) throws IloException {
        double objTrain = 0;
        double objMip = 0;
        for (Job job : solution.getSolutionJobs()) {

            InstanceSample instanceSample = job2InstanceSample(job, instance);
            double obj1 = job.getCostTotal();
            objTrain += obj1;
            double obj2 = SampleGeneration.solveModel(instanceSample, it) + instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getStation().getNodeIndex()];
            objMip += obj2;
            System.out.println("objTrain:" + obj1 + ", objMip:" + obj2);
            System.out.println("job: " + job);
//            if (Math.abs(obj1 - obj2) > 5) {
//                String feature = SampleGeneration.getFeature(instanceSample);
//                System.out.println(feature);
//                System.out.println("a large difference");
//
//            }
        }
        System.out.println("objTrain:" + objTrain + ", objMip:" + objMip);
        double diff = (objTrain - objMip) / objMip;
        return diff;
    }

    private Solution mip(InstanceJob instanceJob, boolean isTotalJob) throws IloException {
        JobMip mip = new JobMip(instanceJob, isTotalJob);
        long runTime = System.currentTimeMillis();
        System.out.println("Starting branch and bound for " + instanceJob.getName());
        mip.solve();
        runTime = System.currentTimeMillis() - runTime;

        if (mip.isFeasible()) {

            System.out.println("Objective: " + mip.getObjectiveValue());
            System.out.println("Runtime: " + runTime);
            System.out.println("Is optimal: " + mip.isOptimal());
            System.out.println("Bound: " + mip.getLowerBound());
            System.out.println("Nodes: " + mip.getNrOfNodes());

        } else {
            System.out.println("MIP infeasible!");
        }
        Solution solution = mip.getSolution();
        return solution;
    }

    private Solution mipIter(InstanceJob instanceJob, Map<String, double[]> dualCostsMap, double cgObj) throws IloException {

        Set<JobReduced> jobReducedSet = getReducedJob(instanceJob, dualCostsMap);
        Set<JobReduced> jobReducedSetAdded = new HashSet<>();
        TopK<JobReduced> topK = new TopK<>();


        PriorityQueue<JobReduced> jobReducedPriorityQueue = topK.bottomK(Constants.mipIterColumnNum, new ArrayList<>(jobReducedSet));
        for (JobReduced jobReduced : jobReducedPriorityQueue) {
            instanceJob.getJobMipList().add(jobReduced.getJob());
        }
        jobReducedSet.removeAll(jobReducedPriorityQueue);
        jobReducedSetAdded.addAll(jobReducedPriorityQueue);

        JobMip mip = new JobMip(instanceJob, false);
        System.out.println("column number:" + instanceJob.getJobMipList().size());
        mip.solve();
        double upperbound = mip.getObjectiveValue();
        double lowerbound = cgObj;
        double gap = upperbound - lowerbound;

        for (JobReduced jobReduced : jobReducedSet) {
            if (jobReduced.getReducedCost() < gap) {
                instanceJob.getJobMipList().add(jobReduced.getJob());
                jobReducedSetAdded.add(jobReduced);
            }
        }
        mip = new JobMip(instanceJob, false);
        System.out.println("column number:" + instanceJob.getJobMipList().size());
        mip.solve();
        Solution solution = mip.getSolution();

        if (verifyOptimality(mip.getObjectiveValue(), cgObj, jobReducedSetAdded)) {
            System.out.println("YEAH! OPTIMAL SOLUTION IS FOUND!");
        }


//        for(int iter=0;iter<Constants.mipIter;iter++){
//           PriorityQueue<JobReduced> jobReducedPriorityQueue= topK.bottomK(Constants.mipIterColumnNum,new ArrayList<>(jobReducedSet));
//           for (JobReduced jobReduced:jobReducedPriorityQueue){
//               instanceJob.getJobMipList().add(jobReduced.getJob());
//           }
//           jobReducedSet.removeAll(jobReducedPriorityQueue);
//           jobReducedSetAdded.addAll(jobReducedPriorityQueue);
//           JobMip mip = new JobMip(instanceJob, false);
//           long runTime = System.currentTimeMillis();
//           System.out.println("column number:"+instanceJob.getJobMipList().size());
//           mip.solve();
////           runTime = System.currentTimeMillis() - runTime;
////
////           if (mip.isFeasible()) {
////
////               System.out.println("Objective: " + mip.getObjectiveValue());
////               System.out.println("Runtime: " + runTime);
////               System.out.println("Is optimal: " + mip.isOptimal());
////               System.out.println("Bound: " + mip.getLowerBound());
////               System.out.println("Nodes: " + mip.getNrOfNodes());
////
////           } else {
////               System.out.println("MIP infeasible!");
////           }
//           solution = mip.getSolution();
//           if(verifyOptimality(mip.getObjectiveValue(),cgObj,jobReducedSetAdded)){
//               System.out.println("YEAH! OPTIMAL SOLUTION IS FOUND!");
//               break;
//           }
//
//       }
        return solution;
    }

    private boolean verifyOptimality(double currentObj, double cgObj, Set<JobReduced> jobReducedSetAdded) {
        double maxReducedCost = Double.MIN_VALUE;
        for (JobReduced jobReduced : jobReducedSetAdded) {
            if (maxReducedCost < jobReduced.getReducedCost()) {
                maxReducedCost = jobReduced.getReducedCost();
            }
        }
        double newB = cgObj + maxReducedCost;
        if (currentObj > newB) {
            return false;
        }
        return true;
    }

    private Set<JobReduced> getReducedJob(InstanceJob instanceJob, Map<String, double[]> dualCostsMap) {
        Set<JobReduced> jobReducedList = new HashSet<>();
        for (Job job : instanceJob.getJobList()) {
            JobReduced jobReduced = new JobReduced(job);
            jobReducedList.add(jobReduced);
            double reducedCost = job.getCostTotal();
            reducedCost -= dualCostsMap.get("stationCapacity")[job.getStation().getIndex()];
            reducedCost -= dualCostsMap.get("workerOneJobAtMost")[instanceJob.getWorkerIdxMap().get(job.getWorker())];

            for (Parcel parcel : job.getParcelList()) {
                int index = instanceJob.getParcelIdxMap().get(parcel);
                reducedCost -= dualCostsMap.get("parcelvisitedConstraints")[index];
            }
            jobReduced.setReducedCost(reducedCost);
        }
        return jobReducedList;
    }

    private CrowdsourcingCGSolver cgen(InstanceJob instanceJob) throws TimeLimitExceededException {
        Crowdsourcing crowdsourcing = new Crowdsourcing(instanceJob);

        CrowdsourcingCGSolver cgSolver = new CrowdsourcingCGSolver(crowdsourcing);
        return cgSolver;
    }

    private void reduceParcelWorker(Set<Parcel> parcelTotal, Set<Worker> workerTotal, Solution solution, int periodCutoff) {
//        label:
//        for (Job job : solution.getSolutionJobs()) {
//            for (Parcel parcel : job.getParcelList()) {
//                if (parcel.getDeadline() <= periodCutoff) {
//                    int curCap = job.getStation().getCurrentCapRemained();
//                    job.getStation().setCurrentCapRemained(curCap - 1);
//                    parcelTotal.removeAll(job.getParcelList());
//                    workerTotal.remove(job.getWorker());
//                    continue label;
//                }
//            }
//
//
//        }
        label:
        for (Job job : solution.getSolutionJobs()) {
            for (Parcel parcel : job.getParcelList()) {
                if (parcel.getDeadline() > periodCutoff) {

                    continue label;
                }
            }
            int curCap = job.getStation().getCurrentCapRemained();
            job.getStation().setCurrentCapRemained(curCap - 1);
            parcelTotal.removeAll(job.getParcelList());
            workerTotal.remove(job.getWorker());

        }
    }

    private InstanceJob genInstanceJob(Map<Parcel, Set<Job>> parcelToJob, Set<Job> jobSet, Set<Worker> workerSet, List<Parcel> parcelList, List<Station> stationList, int it, int workerCapacity) {
        InstanceJob instanceJob = new InstanceJob();
        instanceJob.setJobList(new ArrayList<>(jobSet));
        instanceJob.setWorkerList(new ArrayList<>(workerSet));
        instanceJob.setParcelList(parcelList);
        instanceJob.setStationList(stationList);
        instanceJob.setName("iteration_" + it);
        instanceJob.setWorkerCapacity(workerCapacity);
        instanceJob.setParcelToJob(parcelToJob);
        return instanceJob;
    }

    private Map<Parcel, Set<Job>> getParcelToJob(List<Parcel> parcels, Set<Job> jobSet) {
        Map<Parcel, Set<Job>> parcelToJob = new HashMap<>(parcels.size());
        for (Parcel parcel : parcels) {
            parcelToJob.put(parcel, new HashSet<>());
        }
        for (Job job : jobSet) {
            for (Parcel parcel : job.getParcelList()) {
                parcelToJob.get(parcel).add(job);
            }
        }
        return parcelToJob;
    }

    private Set<Job> generateJob(List<Parcel> parcels, Set<Pair<Station, Worker>> pairSWSet, Map<Pair<Station, Worker>, Set<Parcel>> unavailableSWPMap, Instance instance) throws IOException, XGBoostError {
        Set<Job> jobSet = new LinkedHashSet<>();
        List<List<Integer>> combines = new ArrayList<>();
        for (int i = 2; i <= instance.getWorkerCapacity(); i++) {
            Util.combine(parcels.size(), i, combines);
        }
        int count = 0;
        long runTime = System.currentTimeMillis();
        BufferedWriter bf = new BufferedWriter(new FileWriter("dataset/predict.svm.txt"));

        for (Pair<Station, Worker> pair : pairSWSet) {
            StringBuilder stringBuilder = new StringBuilder();
            lable:
            for (List<Integer> combine : combines) {
                List<Parcel> parcelList = new ArrayList<>();
                for (int index : combine) {
                    Parcel parcel = parcels.get(index - 1);
                    if (unavailableSWPMap.get(pair).contains(parcel)) {
                        continue lable;
                    }
                    parcelList.add(parcels.get(index - 1));
                }

                Job job = genANewJob(pair, parcelList, count);
                jobSet.add(job);
                count++;
//                InstanceSample instanceSample = job2InstanceSample(job);
//                /*summarize the features*/
//                String features = SampleGeneration.getFeature(instanceSample);
                String features = Util.getFeature(pair.getLeft(), pair.getRight(), parcelList, instance.getTravelTimeMatrix());
                stringBuilder.append(21.0 + "\t" + features);
                if (count % 100000 == 0) {
                    bf.write(stringBuilder.toString());
                    bf.flush();
//                    stringBuilder.setLength(0);
                    stringBuilder = new StringBuilder();
                }
            }
            bf.write(stringBuilder.toString());
            bf.flush();
        }
        bf.close();
        System.out.println("runtime of feature calculation and record:" + (System.currentTimeMillis() - runTime));
        runTime = System.currentTimeMillis();
        System.out.println("count:" + count);
        Booster booster = XGBoost.loadModel("model.bin");
        DMatrix dtest = new DMatrix("dataset/predict.svm.txt#dtest.cache");
// predict
        float[][] predicts = booster.predict(dtest);
        System.out.println("run time of xgb prediction" + (System.currentTimeMillis() - runTime));
        System.out.println("predict length:" + predicts.length);
        int negCount = 0;
        float min = 0;
        float max = 0;
        Set<Job> removeJob = new HashSet<>();
        label:
        for (Job job : jobSet) {
            float travelTime = predicts[job.getJobIndex()][0];
            travelTime += instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getStation().getNodeIndex()];
            if (job.getWorker().getEarliestDeparture() + travelTime > job.getWorker().getLatestArrival()) {
                removeJob.add(job);
                continue;
            }
            for (Parcel parcel : job.getParcelList()) {
                if (job.getWorker().getEarliestDeparture() + travelTime > parcel.getDeadline() + instance.getTravelTimeMatrix()[parcel.getNodeIndex()][job.getWorker().getIndexD()]) {
                    removeJob.add(job);
                    continue label;
                }
            }
            job.setTravelTime(travelTime);
            job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);


//            if (predicts[job.getJobIndex()][0] < 0) {
//                negCount++;
//            }
//            if (predicts[job.getJobIndex()][0] < min) {
//                min = predicts[job.getJobIndex()][0];
//            }
//            if (predicts[job.getJobIndex()][0] > max) {
//                max = predicts[job.getJobIndex()][0];
//            }
        }
//        System.out.println("negCount:" + negCount + "; min:" + min + "; max:" + max);
        jobSet.removeAll(removeJob);
        int removedCnt = 0;
        System.out.println("pairSWSet size:" + pairSWSet.size());
        for (Parcel parcel : parcels) {
            int unAvailable = 0;
            for (Pair<Station, Worker> pair : pairSWSet) {
                if (unavailableSWPMap.get(pair).contains(parcel)) {
                    unAvailable++;
                }
            }

            System.out.println("unAvailable parcel:" + unAvailable);

        }
        for (Pair<Station, Worker> pair : pairSWSet) {
            for (Parcel parcel : parcels) {
                if (unavailableSWPMap.get(pair).contains(parcel)) {
                    removedCnt++;
                    continue;
                }
                double travelTime = Util.calTravelTime(pair.getRight(), pair.getLeft(), parcel, instance.getTravelTimeMatrix());
                Job job = genANewJob(pair, Collections.singletonList(parcel), count);
                job.setTravelTime(travelTime);
                job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);
                jobSet.add(job);
                count++;
            }
        }
        System.out.println("removed count: " + removedCnt);
        return jobSet;
    }

    private Set<Job> generateMipJob(List<Parcel> parcels, Set<Pair<Station, Worker>> pairSWSet, Map<Pair<Station, Worker>, Set<Parcel>> unavailableSWPMap, Instance instance) throws IOException, XGBoostError, IloException {
        Set<Job> jobSet = new LinkedHashSet<>();
        List<List<Integer>> combines = new ArrayList<>();
        for (int i = 2; i <= instance.getWorkerCapacity(); i++) {
            Util.combine(parcels.size(), i, combines);
        }
        int count = 0;
        Set<Job> removeJob = new HashSet<>();

        for (Pair<Station, Worker> pair : pairSWSet) {
            lable:
            for (List<Integer> combine : combines) {
                List<Parcel> parcelList = new ArrayList<>();
                for (int index : combine) {
                    Parcel parcel = parcels.get(index - 1);
                    if (unavailableSWPMap.get(pair).contains(parcel)) {
                        continue lable;
                    }
                    parcelList.add(parcels.get(index - 1));
                }

                Job job = genANewJob(pair, parcelList, count);
                InstanceSample instanceSample = job2InstanceSample(job, instance);

                double travelTime = SampleGeneration.solveModel(instanceSample, job.getJobIndex());

                jobSet.add(job);
                count++;
                travelTime += instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getStation().getNodeIndex()];
                if (job.getWorker().getEarliestDeparture() + travelTime > job.getWorker().getLatestArrival()) {
                    removeJob.add(job);
                    if (isJobSelected(job)) {
                        System.out.println("selected job removed1:" + job.getJobIndex() + "-ed:" + job.getWorker().getEarliestDeparture() + "-la:" + job.getWorker().getLatestArrival() + "-travelTime:" + travelTime);
                    }

                    continue;
                }
                for (Parcel parcel : job.getParcelList()) {
                    if (job.getWorker().getEarliestDeparture() + travelTime > parcel.getDeadline() + instance.getTravelTimeMatrix()[parcel.getNodeIndex()][job.getWorker().getIndexD()]) {
                        removeJob.add(job);
                        if (isJobSelected(job)) {
                            System.out.println("selected job removed2:" + job.getJobIndex() + "-ed:" + job.getWorker().getEarliestDeparture() + "-la:" + job.getWorker().getLatestArrival() + "-travelTime:" + travelTime);
                        }

                        continue lable;
                    }
                }
                job.setTravelTime(travelTime);
                job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);
            }
        }


        jobSet.removeAll(removeJob);
        for (Job job : removeJob) {
            if (isJobSelected(job)) {
                System.out.println("selected job removed:" + job.getJobIndex() + "-ed:" + job.getWorker().getEarliestDeparture() + "-la:" + job.getWorker().getLatestArrival() + "-travelTime:" + job.getTravelTime());
                System.out.println(job);
            }
        }
        System.out.println("count:" + count);
        for (Pair<Station, Worker> pair : pairSWSet) {
            for (Parcel parcel : parcels) {
                if (unavailableSWPMap.get(pair).contains(parcel)) {
                    continue;
                }
                double travelTime = Util.calTravelTime(pair.getRight(), pair.getLeft(), parcel, instance.getTravelTimeMatrix());
                Job job = genANewJob(pair, Collections.singletonList(parcel), count);
                job.setTravelTime(travelTime);
                job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);
                jobSet.add(job);
                count++;
            }
        }

        return jobSet;
    }

    private boolean isJobSelected(Job job) {


//        j_4438, w_22, s_1, p_0, p_3, p_14,
//                selected job removed2:6304
//        j_6304, w_5, s_0, p_8, p_11,
//                selected job removed2:8073
//        j_8073, w_35, s_2, p_2, p_5, p_7,
//                selected job removed2:10568
//        j_10568, w_15, s_1, p_4, p_10, p_15,

        List<Integer> parcelIndex = new ArrayList<>();
        parcelIndex.add(13);
        parcelIndex.add(16);
        parcelIndex.add(19);
        if (isSelected(job, 1, 1, parcelIndex)) {
            return true;
        }
        parcelIndex.clear();
        parcelIndex.add(1);
        parcelIndex.add(6);
        parcelIndex.add(17);
        if (isSelected(job, 4, 2, parcelIndex)) {
            return true;
        }
        parcelIndex.clear();
        parcelIndex.add(9);
        parcelIndex.add(12);
        parcelIndex.add(18);
        if (isSelected(job, 39, 2, parcelIndex)) {
            return true;
        }
        parcelIndex.clear();
        parcelIndex.add(0);
        parcelIndex.add(3);
        parcelIndex.add(14);
        if (isSelected(job, 22, 1, parcelIndex)) {
            return true;
        }

        parcelIndex.clear();
        parcelIndex.add(8);
        parcelIndex.add(11);
        if (isSelected(job, 5, 0, parcelIndex)) {
            return true;
        }
        parcelIndex.clear();
        parcelIndex.add(2);
        parcelIndex.add(5);
        parcelIndex.add(7);
        if (isSelected(job, 35, 2, parcelIndex)) {
            return true;
        }
        parcelIndex.clear();
        parcelIndex.add(4);
        parcelIndex.add(10);
        parcelIndex.add(15);
        if (isSelected(job, 15, 1, parcelIndex)) {
            return true;
        }

        return false;

    }

    private boolean isSelected(Job job, int w, int s, List<Integer> parcelIndex) {
        if (job.getWorker().getIndex() != w) {
            return false;
        }
        if (job.getStation().getIndex() != s) {
            return false;
        }
        for (int i = 0; i < job.getParcelList().size(); i++) {
            Parcel parcel = job.getParcelList().get(i);
            if (parcel.getIndex() != parcelIndex.get(i)) {
                return false;
            }
        }
        return true;
    }

    private Set<Job> generateMipJob2(List<Parcel> parcels, Set<Pair<Station, Worker>> pairSWSet, Map<Pair<Station, Worker>, Set<Parcel>> unavailableSWPMap, Instance instance) throws IOException, XGBoostError, IloException {
        Set<Job> jobSet = new LinkedHashSet<>();
        List<List<Integer>> combines = new ArrayList<>();
        for (int i = 2; i <= instance.getWorkerCapacity(); i++) {
            Util.combine(parcels.size(), i, combines);
        }
        int count = 0;
        Set<Integer> selectedJob = new HashSet<>();
        selectedJob.add(616);
        selectedJob.add(2701);
        selectedJob.add(3632);
        selectedJob.add(4438);
        selectedJob.add(6304);
        selectedJob.add(8073);
        selectedJob.add(10568);
        for (Pair<Station, Worker> pair : pairSWSet) {
            lable:
            for (List<Integer> combine : combines) {
                List<Parcel> parcelList = new ArrayList<>();
                for (int index : combine) {
                    Parcel parcel = parcels.get(index - 1);
                    if (unavailableSWPMap.get(pair).contains(parcel)) {
                        continue lable;
                    }
                    parcelList.add(parcels.get(index - 1));
                }

                Job job = genANewJob(pair, parcelList, count);
                InstanceSample instanceSample = job2InstanceSample(job, instance);

                double travelTime = SampleGeneration.solveModel2(instanceSample, job.getJobIndex());
                travelTime += instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getStation().getNodeIndex()];
                if (SampleGeneration.isFeasible) {
                    if (selectedJob.contains(job.getJobIndex())) {
                        System.out.println("selected job removed2:" + job.getJobIndex());
                        System.out.println(job);
                    }
                    jobSet.add(job);
                    job.setTravelTime(travelTime);
                    job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);
                    count++;
//                    System.out.println(count);
                }

            }
        }
        for (Pair<Station, Worker> pair : pairSWSet) {
            for (Parcel parcel : parcels) {
                if (unavailableSWPMap.get(pair).contains(parcel)) {
                    continue;
                }
                double travelTime = Util.calTravelTime(pair.getRight(), pair.getLeft(), parcel, instance.getTravelTimeMatrix());
                Job job = genANewJob(pair, Collections.singletonList(parcel), count);
                job.setTravelTime(travelTime);
                job.setCostTotal(travelTime - instance.getTravelTimeMatrix()[job.getWorker().getIndexO()][job.getWorker().getIndexD()]);
                jobSet.add(job);
                count++;
            }
        }
        System.out.println("count:" + count);
        return jobSet;
    }

    private Job genANewJob(Pair<Station, Worker> pair, List<Parcel> parcelList, int count) {
        Job job = new Job();
        job.setStation(pair.getLeft());
        job.setWorker(pair.getRight());
        job.setParcelList(parcelList);
        job.setJobIndex(count);
        return job;
    }

    private Set<Pair<Station, Worker>> getAvailableSWPair(List<Station> stations, Set<Worker> workerSet, int[][] travelTMatrix) {
        Set<Pair<Station, Worker>> pairSWSet = new HashSet<>();
        for (Station station : stations) {
            for (Worker worker : workerSet) {
                int travelT = travelTMatrix[worker.getIndexO()][station.getIndex()] + travelTMatrix[station.getIndex()][worker.getIndexD()];
                if (worker.getEarliestDeparture() + travelT > worker.getLatestArrival()) {
                    continue;
                }
                pairSWSet.add(Pair.of(station, worker));
            }
        }
        return pairSWSet;
    }

    private Map<Pair<Station, Worker>, Set<Parcel>> getUnavailablePairSWPMap(Set<Pair<Station, Worker>> pairSWSet, List<Parcel> parcels, int[][] travelTMatrix) {
        Map<Pair<Station, Worker>, Set<Parcel>> unavailableSWPMap = new HashMap<>();
        for (Pair<Station, Worker> pair : pairSWSet) {
            Worker worker = pair.getRight();
            Station station = pair.getLeft();
            if (worker.getIndex() == 7 && station.getIndex() == 0) {
                int a = 0;
            }
            Set<Parcel> unavailableParcelSet = new HashSet<>();
            for (Parcel parcel : parcels) {
                if (parcel.getIndex() == 9) {
                    int a = 0;
                }
                int indexO = worker.getIndexO();
                int indexS = station.getNodeIndex();
                int indexP = parcel.getNodeIndex();
                int indexD = worker.getIndexD();
                int travelTOS = travelTMatrix[indexO][indexS];
                int travelTSP = travelTMatrix[indexS][indexP];
                int travelTPD = travelTMatrix[indexP][indexD];
                int travelT = travelTOS + travelTSP + travelTPD;
//                System.out.println(travelT+" "+(travelTOS+travelTSP)+" "+worker.getEarliestDeparture()+" "+parcel.getDeadline());
                if (parcel.getNearestStation().getIndex() != station.getIndex()) {
//                    unavailableParcelSet.add(parcel);
//                    continue;
                }
                if (worker.getEarliestDeparture() + travelTOS + travelTSP > parcel.getDeadline()) {
                    unavailableParcelSet.add(parcel);
                    continue;
                }
                if (worker.getEarliestDeparture() + travelT > worker.getLatestArrival()) {
                    unavailableParcelSet.add(parcel);
                    continue;
                }


            }
            unavailableSWPMap.put(pair, unavailableParcelSet);
//            System.out.println("station:"+station.getIndex()+" worker:"+worker.getIndex()+" unavailable parcel size:"+unavailableParcelSet.size());
        }
        return unavailableSWPMap;
    }

    private Set<Worker> getWorkerSet(Set<Worker> workerTotal, int periodEnd) {
        Set<Worker> workerSet = new HashSet<>();
        for (Worker worker : workerTotal) {
            int earliestDeparture = worker.getEarliestDeparture();
            int travelTOD = worker.getTravelTOD();
            if (earliestDeparture > periodEnd - travelTOD) {
                continue;
            }

            workerSet.add(worker);
        }
        return workerSet;
    }

    private List<Parcel> getParcelList(Set<Parcel> parcelTotal, int periodEnd) {
        List<Parcel> parcelList = new ArrayList<>();
        for (Parcel parcel : parcelTotal) {
            if (parcel.getDeadline() > periodEnd) {
                continue;
            }

            parcelList.add(parcel);
        }
        return parcelList;
    }

    public static void main(String[] args) throws IOException, XGBoostError, TimeLimitExceededException, IloException {
        File file = new File("dataset/instance/S3_W191_P288.txt");
        Instance instance = Reader.readInstance(file);


//        Job job=new Job();
//        job.setStation(instance.getStations().get(1));
//        job.setWorker(instance.getWorkers().get(190));
//        List<Parcel> parcelList=new ArrayList<>();
//        parcelList.add(instance.getParcels().get(15));
//        parcelList.add(instance.getParcels().get(221));
//        parcelList.add(instance.getParcels().get(249));
//        job.setParcelList(parcelList);
//        float[]feature=Util.getFeature0(job.getStation(),job.getWorker(),job.getParcelList(),instance.getTravelTimeMatrix());
//        Booster booster = XGBoost.loadModel("model.bin");
////        DMatrix dtest = new DMatrix("dataset/predict.svm.txt");
//
////        float[] data = new float[] {1f,2f,3f,4f,5f,6f};
//        int nrow = 1;
//        int ncol = 16;
//        float missing = 0.0f;
//        DMatrix dmat = new DMatrix(feature, nrow, ncol, missing);
//// predict
//        float[][] predicts = booster.predict(dmat);
//
//        InstanceSample instanceSample = job2InstanceSample(job);
//        double obj2=SampleGeneration.solveModel(instanceSample, 0);


        RollingTimeHorizon rollingTimeHorizon = new RollingTimeHorizon();
        rollingTimeHorizon.mainIteration(instance);
    }
}
