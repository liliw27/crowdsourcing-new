package cg.masterProblem;

import cg.column.JobColumn;
import cg.model.Crowdsourcing;
import cg.pricing.PricingProblem;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import model.Parcel;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:41 AM
 */
public class Master extends AbstractMaster<Crowdsourcing, JobColumn, PricingProblem, CrowdsoucingMasterData> {
    private IloObjective obj; //Objective function
    private IloRange[] parcelvisitedConstraints; //Constraint:every customer is visited
    private IloRange[] stationCapacity; //Constraint:station capacity Limitation
    private IloRange[] workerOneJobAtMost; //Constraint:every worker perform at most one jobSimple

    public Master(Crowdsourcing dataModel, PricingProblem pricingProblem) {
        super(dataModel, pricingProblem, OptimizationSense.MINIMIZE);
    }

    @Override
    protected CrowdsoucingMasterData buildModel() {
        IloCplex cplex = null; //Cplex instance
        try {
            cplex = new IloCplex(); //Create cplex instance
            cplex.setOut(null); //Disable cplex output
            cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS); //Set number of threads that may be used by the cplex

            //Define the objective
            obj = cplex.addMinimize();

            //Define constraints
            parcelvisitedConstraints = new IloRange[dataModel.instance.getParcelToJob().size()];
            stationCapacity = new IloRange[dataModel.instance.getStationList().size()];
            workerOneJobAtMost = new IloRange[dataModel.instance.getWorkerList().size()];

            for (int i = 0; i < dataModel.instance.getParcelToJob().size(); i++) {
                parcelvisitedConstraints[i] = cplex.addRange(1, 1, "parcelvisitedConstraints_" + i);
            }
            for (int i = 0; i < dataModel.instance.getWorkerList().size(); i++) {
                workerOneJobAtMost[i] = cplex.addRange(0, 1, "workerOneJobAtMost_" + i);
            }
            for (int i = 0; i < dataModel.instance.getStationList().size(); i++) {
                stationCapacity[i] = cplex.addRange(0, dataModel.instance.getStationList().get(i).getCurrentCapRemained(), "stationCapacity_" + i);
            }


        } catch (IloException e) {
            e.printStackTrace();
        }
        //Define a container for the variables
        Map<PricingProblem, OrderedBiMap<JobColumn, IloNumVar>> varMap = new LinkedHashMap<>();
        for (PricingProblem pricingProblem : pricingProblems) {
            varMap.put(pricingProblem, new OrderedBiMap<>());
        }

        //Return a new data object which will hold data from the Master Problem. This object automatically be passed to the CutHandler class.
        return new CrowdsoucingMasterData(cplex, pricingProblems, varMap);
    }

    /**
     * Solve the master problem
     *
     * @param timeLimit Future point in time by which the solve procedure must be completed
     * @return true if the master problem has been solved
     * @throws TimeLimitExceededException TimeLimitExceededException
     */
    @Override
    protected boolean solveMasterProblem(long timeLimit) throws TimeLimitExceededException {
        try {
            //Set time limit
            double timeRemaining = Math.max(1, (timeLimit - System.currentTimeMillis()) / 1000.0);
            masterData.cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); //set time limit in seconds
            //Potentially export the model
            if (config.EXPORT_MODEL) {
                masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + "master_" + this.getIterationCount() + ".lp");
            }
            exportModel("master_" + this.getIterationCount() + ".lp");
            //Solve the model
            if (!masterData.cplex.solve() || masterData.cplex.getStatus() != IloCplex.Status.Optimal) {
                if (masterData.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) //Aborted due to time limit
                    throw new TimeLimitExceededException();
                else
                    throw new RuntimeException("Master problem solve failed! Status: " + masterData.cplex.getStatus());
            } else {
                masterData.objectiveValue = masterData.cplex.getObjValue();
            }
        } catch (IloException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Extracts information from the master problem which is required by the pricing problems, e.g. the reduced costs/dual values
     *
     * @param pricingProblem pricing problem
     */
    @Override
    public void initializePricingProblem(PricingProblem pricingProblem) {
        try {
            Map<String, double[]> dualMap = new HashMap<>();
            double[] dualP=masterData.cplex.getDuals(parcelvisitedConstraints);
            double[] dualS=masterData.cplex.getDuals(stationCapacity);
            double[] dualW=masterData.cplex.getDuals(workerOneJobAtMost);

            dualMap.put("parcelvisitedConstraints", dualP);
            dualMap.put("stationCapacity",dualS);
            dualMap.put("workerOneJobAtMost",dualW);

//            pricingProblem.initPricingProblem(dualMap, getSolution());
            pricingProblem.initPricingProblem(dualMap);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    /**
     * Function which adds a new column to the master problem
     */
    @Override
    public void addColumn(JobColumn column) {
        try {
            double objCoe=column.jobReduced.getJob().getCostTotal();

            //Set linear coefficient for parcel visited Constraints
            int pConstraintCoe[]= new int[dataModel.instance.getParcelToJob().size()];
            for(Parcel parcel:column.jobReduced.getJob().getParcelList()){
                int index=dataModel.instance.getParcelIdxMap().get(parcel);
                pConstraintCoe[index]=1;
            }
            //Set linear coefficient for station Capacity
            int sConstraintCoe[]= new int[dataModel.instance.getStationList().size()];
            sConstraintCoe[column.jobReduced.getJob().getStation().getIndex()]=1;
            //Set linear coefficient for worker One JobSimple At Most
            int wConstraintCoe[]= new int[dataModel.instance.getWorkerList().size()];
            wConstraintCoe[dataModel.instance.getWorkerIdxMap().get(column.jobReduced.getJob().getWorker())]=1;


            //Register column with objective
            IloColumn iloColumn = masterData.cplex.column(obj, objCoe);

            //Register column with constraints
            for (int i = 0; i < dataModel.instance.getParcelToJob().size(); i++) {
                iloColumn=iloColumn.and(masterData.cplex.column(parcelvisitedConstraints[i],pConstraintCoe[i]));
            }
            for (int i = 0; i < dataModel.instance.getWorkerList().size(); i++) {
                iloColumn=iloColumn.and(masterData.cplex.column(workerOneJobAtMost[i],wConstraintCoe[i]));
            }
            for (int i = 0; i < dataModel.instance.getStationList().size(); i++) {
                iloColumn=iloColumn.and(masterData.cplex.column(stationCapacity[i],sConstraintCoe[i]));
            }

            //Create the variable and store it
            IloNumVar var = masterData.cplex.numVar(iloColumn, 0, 1, "w_"+ masterData.getNrColumns());
            masterData.cplex.add(var);
            masterData.addColumn(column, var);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<JobColumn> getSolution() {

        List<JobColumn> solution = new ArrayList<>();
        try {
            JobColumn[] jobColumns = masterData.getVarMap().getKeysAsArray(new JobColumn[masterData.getNrColumns()]);
            IloNumVar[] vars = masterData.getVarMap().getValuesAsArray(new IloNumVar[masterData.getNrColumns()]);

            //Iterate over each column and add it to the solution if it has a non-zero value
            for (int i = 0; i < masterData.getNrColumns(); i++) {
                jobColumns[i].value= masterData.cplex.getValue(vars[i]);
                if (jobColumns[i].value >= config.PRECISION) {
                    solution.add(jobColumns[i]);
                }
            }


        } catch (IloException e) {
            e.printStackTrace();
        }

        return solution;
    }

    @Override
    public void printSolution() {
        System.out.println("Master solution:");
        for(JobColumn jc : this.getSolution())
            System.out.println(jc);
    }

    @Override
    public void close() {
        masterData.cplex.end();
    }
    /**
     * Export the model to a file
     */
    @Override
    public void exportModel(String fileName) {
        try {
            masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + fileName);
        } catch (IloException e) {
            e.printStackTrace();
        }
    }
}
