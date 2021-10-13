package MIP.mipJob;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.InstanceJob;
import model.Job;
import model.Parcel;
import model.Station;
import model.Worker;
import util.Constants;

/**
 * @author Wang Li
 * @description
 * @date 9/1/21 7:15 PM
 */
public class JobModelBuilder {
    private final InstanceJob dataModel;
    private JobMipData mipData;

    public JobModelBuilder(InstanceJob dataModel,boolean isTotalJob) throws IloException {
        this.dataModel = dataModel;
        this.buildModel(isTotalJob);
    }

    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public JobMipData getLP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipData;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public JobMipData getILP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipData.cplex.setParam(IloCplex.DoubleParam.TiLim, Constants.MAXBRANCHBOUNDTIME); //set time limit in seconds
        mipData.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipData.cplex.setParam(IloCplex.IntParam.NodeFileInd,3);
//        mipData.cplex.setParam(IloCplex.IntParam.WorkMem,4096);
        mipData.cplex.setOut(null); //Disable Cplex output
        return mipData;
    }

    private void buildModel(boolean isTotalJob) throws IloException {
        IloCplex cplex = new IloCplex();
        int stationNum = dataModel.getStationList().size();
        int workerNum = dataModel.getWorkerList().size();
        int jobNum;
        if(isTotalJob){
            jobNum=dataModel.getJobList().size();
        }else{
            jobNum=dataModel.getJobMipList().size();
        }
        IloIntVar[]vars = new IloIntVar[jobNum];





        //create variables
        for(int i=0;i<jobNum;i++){
            IloIntVar var = cplex.boolVar("x_" + i);
            vars[i]=var;
        }



        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for(int i=0;i<jobNum;i++){
            if (isTotalJob) {
                obj.addTerm(dataModel.getJobList().get(i).getCostTotal(), vars[i]);
            }else {
                obj.addTerm(dataModel.getJobMipList().get(i).getCostTotal(), vars[i]);
            }
        }

        cplex.addMinimize(obj);

        //create constraints
        //1.every parcel is delivered once
        for(int i=0;i<dataModel.getParcelList().size();i++){
            Parcel parcel=dataModel.getParcelList().get(i);
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for(int j=0;j<jobNum;j++){
                Job job;
                if(isTotalJob){
                    job =dataModel.getJobList().get(j);
                }else {
                    job =dataModel.getJobMipList().get(j);
                }

                if(job.getParcelList().contains(parcel)){
                    expr.addTerm(1,vars[j]);
                }
            }
            cplex.addEq(expr, 1, "parcelvisitedConstraints_" + i);
        }

        //2.station capacity

        for (int i = 0; i < stationNum; i++) {
            Station station=dataModel.getStationList().get(i);
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for(int j=0;j<jobNum;j++){
                Job job;
                if(isTotalJob){
                    job =dataModel.getJobList().get(j);
                }else {
                    job =dataModel.getJobMipList().get(j);
                }
                if(job.getStation().equals(station)){
                    expr.addTerm(1,vars[j]);
                }
            }
            cplex.addLe(expr, station.getCurrentCapRemained(), "stationCapacity_" + i);
        }
        //3.worker capacity
        for (int i = 0; i < workerNum; i++) {
            Worker worker=dataModel.getWorkerList().get(i);
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for(int j=0;j<jobNum;j++){
                Job job;
                if(isTotalJob){
                    job =dataModel.getJobList().get(j);
                }else {
                    job =dataModel.getJobMipList().get(j);
                }
                if(job.getWorker().equals(worker)){
                    expr.addTerm(1,vars[j]);
                }
            }
            cplex.addLe(expr, 1, "workerOneJobAtMost_" + i);
        }
        mipData=new JobMipData(cplex, vars);
        cplex.exportModel("fuelmip.lp");
    }

}
