package MIP.mipJob;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import lombok.Data;
import model.InstanceJob;
import model.Job;
import model.Solution;
import util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 9/1/21 7:14 PM
 */
@Data
public class JobMip {
    private final InstanceJob dataModel;
    private JobModelBuilder modelBuilder;
    private JobMipData mipData;
    private boolean isTotalJob;

    private double objectiveValue=-1.0; //Best objective found after MIP
    private boolean optimal=false; //Solution is optimal
    //    private Solution solution; //Best solution
    private boolean isFeasible=true; //Solution is feasible
    private Solution solution=null;
    public JobMip(InstanceJob instance,boolean isTotalJob){
        this.dataModel=instance;
        this.isTotalJob=isTotalJob;

        try {
            modelBuilder=new JobModelBuilder(dataModel,isTotalJob);
            mipData=modelBuilder.getILP();

        } catch (IloException e) {
            e.printStackTrace();
        }
    }

    public void solve() throws IloException {
//		mipDataS.cplex.exportModel("mip.lp");
        //mipDataS.cplex.writeParam(arg0)
//		mipDataS.cplex.exportModel("mip.sav");
//		mipDataS.cplex.exportModel("mip.mps");
//		mipDataS.cplex.writeMIPStart("start.mst");
//		mipDataS.cplex.writeParam("param.prm");
//		mipDataS.cplex.setParam(BooleanParam.PreInd, false); //Disable presolve.
//		mipDataS.cplex.setParam(IntParam.Threads, 2);

        if ( mipData.cplex.solve() && (mipData.cplex.getStatus()== IloCplex.Status.Feasible || mipData.cplex.getStatus()== IloCplex.Status.Optimal)) {
            this.objectiveValue=mipData.cplex.getObjValue();// MathProgrammingUtil.doubleToInt(mipDataS.cplex.getObjValue());//(int)Math.round(mipDataS.cplex.getObjValue());
            //this.printSolution();
            this.optimal=mipData.cplex.getStatus()== IloCplex.Status.Optimal;
            this.isFeasible=true;
            //Verify solution
//			SolutionValidator.validate(btsp, solution);
//			if(solution.getObjective()!=this.objectiveValue)
//				throw new RuntimeException("Objective constructed solution deviates from MIP objective. MIP obj: "+this.objectiveValue+" constr sol obj: "+solution.getObjective());
//			System.out.println("Solution correct");
//			System.out.println("Solution:\n{}"+solution);
        } else if(mipData.cplex.getStatus()== IloCplex.Status.Infeasible) {
//			throw new RuntimeException("Mip infeasible");
            this.isFeasible=false;
            this.optimal=true;
        }else if(mipData.cplex.getCplexStatus()== IloCplex.CplexStatus.AbortTimeLim){
            System.out.println("No solution could be found in the given amount of time");
            this.isFeasible=true; //Technically there is no proof whether or not a feasible solution exists
            this.optimal=false;
        }else{
            //NOTE: when cplex does not find a solution before the default time out, it throws a Status Unknown exception
            //NOTE2: Might be required to extend the default runtime.
            throw new RuntimeException("Cplex solve terminated with status: "+mipData.cplex.getStatus());
        }
        solution = buildSolution();
        mipData.cplex.end();
    }

    /**
     * Get bound on objective value
     */
    public double getLowerBound(){
        try {
            if(this.isOptimal())
                return this.getObjectiveValue();
            else
                return mipData.cplex.getBestObjValue();
        } catch (IloException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public Solution buildSolution() throws IloException {
        Solution solution=new Solution();
        List<Job> solutionJobs=new ArrayList<>();
        double[] varValues=mipData.cplex.getValues(mipData.vars);
        for(int i=0;i<varValues.length;i++){
           if(!Util.doubleToBoolean(varValues[i])){
               continue;
           }
           if(isTotalJob){
               solutionJobs.add(dataModel.getJobList().get(i));
           }else {
               solutionJobs.add(dataModel.getJobMipList().get(i));
           }
        }
        solution.setSolutionJobs(solutionJobs);
        return solution;
    }

    public Solution getSolution(){
        return solution;
    }

    /**
     * Indicates whether solution is optimal
     */
    public boolean isOptimal(){
        return optimal;
    }

    /**
     * Returns size of search tree (nr of nodes)
     */
    public int getNrOfNodes(){
        return mipData.cplex.getNnodes();
    }


}
