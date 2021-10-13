package cg;

import cg.column.JobColumn;
import cg.masterProblem.Master;
import cg.model.Crowdsourcing;
import cg.model.JobReduced;
import cg.model.JobReducedforHeur;
import cg.pricing.FkPricingSolver;
import cg.pricing.PricingProblem;
import io.Reader;
import model.InstanceJob;
import model.Job;
import model.Parcel;
import model.Station;
import model.Worker;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jorlib.frameworks.columnGeneration.colgenMain.ColGen;
import org.jorlib.frameworks.columnGeneration.io.SimpleCGLogger;
import org.jorlib.frameworks.columnGeneration.io.SimpleDebugger;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 8/31/21 8:00 PM
 */
public class CrowdsourcingCGSolver {

    private final Crowdsourcing dataModel;
    private double upperBound;
    private PricingProblem pricingProblem;
    private double objectiveValue;


    public CrowdsourcingCGSolver(Crowdsourcing dataModel) throws TimeLimitExceededException {
        this.dataModel = dataModel;


        //Create the pricing problems
        pricingProblem = new PricingProblem(dataModel, "crowdSourcingPricing");


        //Create the master problem
        Master master = new Master(dataModel, pricingProblem);

        //Define which solvers to use
        List<Class<? extends AbstractPricingProblemSolver<Crowdsourcing, JobColumn, PricingProblem>>> solvers = Collections.singletonList(FkPricingSolver.class);

        //Create a set of initial columns.
        long runTime=System.currentTimeMillis();
        List<JobColumn> initSolution = this.getInitialSolution(pricingProblem);
        System.out.println("run initSolution"+(System.currentTimeMillis()-runTime));
        runTime=System.currentTimeMillis();
        //Define an upper bound (stronger is better). In this case we get it from the initial solution.
        int upperBound = (int) this.upperBound ;


        //Lower bound on column generation solution (stronger is better): In this case, we choose the maximum travelTime between depot and customers
        double maxCost = 0;
        Map<Parcel,Double> minCostMap=new HashMap<>();
        double initialvalue=Constants.costMax + Constants.punishMax;
        for(Parcel parcel:dataModel.instance.getParcelToJob().keySet()){
            minCostMap.put(parcel,initialvalue);
        }


        for (Job job : dataModel.instance.getJobList()) {
            for(Parcel parcel:job.getParcelList()){
                if (job.getCostTotal() < minCostMap.get(parcel)) {
                    minCostMap.put(parcel,job.getCostTotal());

                }
            }
        }
        maxCost = Collections.max(minCostMap.values());
        double lowerBound = maxCost;

        //Create a column generation instance
        ColGen<Crowdsourcing, JobColumn, PricingProblem> cg = new ColGen<>(dataModel, master, pricingProblem, solvers, initSolution, upperBound, lowerBound);

        //OPTIONAL: add a debugger
//        SimpleDebugger debugger = new SimpleDebugger(cg);

        //OPTIONAL: add a logger
        SimpleCGLogger logger = new SimpleCGLogger(cg, new File("./output/Log/crowdSourcing" + dataModel.instance.getName() + ".log"));

        //Solve the problem through column generation
        try {
            cg.solve(System.currentTimeMillis() + 7200000L);
            System.out.println("run column generation"+(System.currentTimeMillis()-runTime));

        } catch (TimeLimitExceededException e) {
            e.printStackTrace();
        }
        //Print solution:
        objectiveValue=cg.getObjective();
        System.out.println("================ Solution ================");
        List<JobColumn> solution = cg.getSolution();
        System.out.println("CG terminated with objective: " + cg.getObjective());
        System.out.println("Number of iterations: " + cg.getNumberOfIterations());
        System.out.println("Time spent on master: " + cg.getMasterSolveTime() + " time spent on pricing: " + cg.getPricingSolveTime());
//        System.out.println("Columns (only non-zero columns are returned):");
//        for (JobColumn column : solution){
//            System.out.println(column);
//        }
        int count=0;
        DescriptiveStatistics stats = new DescriptiveStatistics();

//        IntStream.rangeClosed(1, 10).forEach(i->stats.addValue(i));//add 1,2,3,4,5,6,7,8,9,10 to stats


//
        for (JobReduced jobReduced : dataModel.jobReducedArray) {
            stats.addValue(jobReduced.getReducedCost());
            if (jobReduced.getReducedCost() > this.upperBound-cg.getObjective()) {
                count++;
            }
        }
        System.out.println("max value "+stats.getMax());//max value 10.0

        System.out.println("min value "+stats.getMin());//min value 1.0

        System.out.println("mean value "+stats.getMean());//mean value 5.5

        System.out.println("75% value "+stats.getPercentile(75));//75% value 8.25

        System.out.println("25% value "+stats.getPercentile(25));//25% value 2.75

        System.out.println("std dev "+stats.getStandardDeviation());//

        System.out.println(stats);
//        for(JobColumn jobColumn:cg.getColumns()){
//
//            reducedValue[index]=jobColumn.jobReduced.getReducedCost();
//            stats.addValue(jobColumn.jobReduced.getReducedCost());
//            index++;
//        }

        System.out.println("upper bound: " + this.upperBound);
        System.out.println("useless column number: " + count);
        System.out.println("total column number: " + dataModel.jobReducedArray.size());
        System.out.println("remained column number: " + (dataModel.jobReducedArray.size() - count));
        System.out.println(cg.getObjective() + " " + cg.getNumberOfIterations() + " " + cg.getMasterSolveTime() + " " + cg.getPricingSolveTime());
        String s = "objective and solving time: " + cg.getObjective() + " " + (cg.getMasterSolveTime() + cg.getPricingSolveTime()) + "\niteration number: " + cg.getNumberOfIterations() + "\nmaster solve time: " + cg.getMasterSolveTime() + "\npricing solve time: " + cg.getPricingSolveTime();

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("./output/solutions/prodCG/" + dataModel.instance.getName() + ".txt"));
            writer.write(s);
            writer.flush();
        } catch (IOException e) {
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
        }

        //Clean up:
        cg.close(); //This closes both the master and pricing problems
    }

    public Map<String, double[]> getDualCostsMap(){
        return pricingProblem.dualCostsMap;
    }

    /**
     * Create an initial solution for the Cutting Stock Problem.
     * Simple initial solution: cut each final from its own raw/roll.
     *
     * @param pricingProblem pricing problem
     * @return Initial solution
     */
    private List<JobColumn> getInitialSolution(PricingProblem pricingProblem) {
        List<JobColumn> initSolution = new ArrayList<>();

        List<JobReducedforHeur> jobReducedList = new ArrayList<>(dataModel.jobReducedArray.size());
        for (JobReduced jobReduced:dataModel.jobReducedArray){
            JobReducedforHeur jobReducedforHeur=new JobReducedforHeur(jobReduced.getJob());
            jobReducedforHeur.setReducedCost(jobReduced.getReducedCost());
            jobReducedList.add(jobReducedforHeur);
        }
        Collections.sort(jobReducedList);

        Set<Parcel> parcelServed = new HashSet<>();

        Map<Worker,Boolean> isWorkerChosen=new HashMap<>();
        Map<Station,Integer>stationLoad=new HashMap<>();
        for(Station station:dataModel.instance.getStationList()){
            stationLoad.put(station,0);
        }
        for (Worker worker:dataModel.instance.getWorkerList()){
            isWorkerChosen.put(worker,false);
        }
        Set<Job> jobUnavailable = new HashSet<>();
        Set<JobReduced> jobRemoved = new HashSet<>();
        int unServedPNum = dataModel.instance.getParcelToJob().size();
        int unServedWNum = dataModel.instance.getWorkerList().size();
        while(unServedPNum>0){
            System.out.println("#############################"+unServedPNum);
            System.out.println("upperbound:"+this.upperBound);
            for (JobReduced e : jobReducedList) {
//            boolean isParcelServed = false;
//            for (int i : e.getJobSimple().getParcelIndexSet()) {
//                if (parcelServed.contains(i)) {
//                    isParcelServed = true;
//                    break;
//                }
//            }
//            if(isParcelServed){
//                continue;
//            }
                int leastServeNum = Math.max(unServedPNum - (unServedWNum - 1) * dataModel.instance.getWorkerCapacity(), 0);
                if (e.getJob().getParcelList().size() < leastServeNum) {
                    continue;
                }
                if (jobUnavailable.contains(e.getJob())) {
                    continue;
                }

                int load=stationLoad.get(e.getJob().getStation());
                if (load >= e.getJob().getStation().getCurrentCapRemained()) {
                    continue;
                }
                if (isWorkerChosen.get(e.getJob().getWorker())) {
                    continue;
                }
                initSolution.add(new JobColumn(pricingProblem, false, "initSolution", e));
                dataModel.instance.getJobMipList().add(e.getJob());
                jobRemoved.add(e);
                stationLoad.put(e.getJob().getStation(),load+1);
                isWorkerChosen.put(e.getJob().getWorker(),true) ;
                parcelServed.addAll(e.getJob().getParcelList());
                unServedPNum -= e.getJob().getParcelList().size();
                unServedWNum--;
                for (Parcel parcel: e.getJob().getParcelList()) {
                    jobUnavailable.addAll(dataModel.instance.getParcelToJob().get(parcel));
                }

                this.upperBound += e.getJob().getCostTotal();

            }
            dataModel.jobReducedArray.removeAll(jobRemoved);
            for(JobReduced jobReduced:jobRemoved){
                JobReduced jobReduced1=new JobReduced(jobReduced.getJob());
                dataModel.jobReducedArray.remove(jobReduced1);
            }
            jobReducedList.removeAll(jobRemoved);
        }
        return initSolution;
    }
    public double getObjectiveValue(){
        return objectiveValue;
    }

    public static void main(String[] args) throws TimeLimitExceededException, FileNotFoundException {
        File file = new File("dataset/jobInstance5.txt");
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        System.out.println("现在时间：" + sdf.format(date)); // 输出已经格式化的现在时间（24小时制）
        InstanceJob instance = Reader.readCGInputNew(file);
        Crowdsourcing crowdsourcing = new Crowdsourcing(instance);
        System.out.println("-");
        System.out.println("现在时间：" + sdf.format(date)); // 输出已经格式化的现在时间（24小时制）
        new CrowdsourcingCGSolver(crowdsourcing);
    }
}
