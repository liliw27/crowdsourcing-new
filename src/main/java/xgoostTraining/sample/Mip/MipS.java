package xgoostTraining.sample.Mip;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import lombok.Data;
import model.InstanceSample;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:49 AM
 */
@Data
public class MipS {
    private final InstanceSample dataModel;
    private ModelBuilderS modelBuilderS;
    private MipDataS mipDataS;

    private int objectiveValue = -1; //Best objective found after MIP
    private boolean optimal = false; //Solution is optimal
    //    private Solution solution; //Best solution
    private boolean isFeasible = true; //Solution is feasible
    double[][] varValues;
    double[] timeVarValues;
    boolean isTimeLimit;

    public MipS(InstanceSample instanceSample, boolean isTimeLimit) {
        this.dataModel = instanceSample;
        varValues = new double[dataModel.getParcelNum() + 2][dataModel.getParcelNum() + 2];
        timeVarValues = new double[dataModel.getParcelNum() + 2];
        this.isTimeLimit=isTimeLimit;
        try {
            modelBuilderS = new ModelBuilderS(dataModel, isTimeLimit);
            mipDataS = modelBuilderS.getILP();

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

        if (mipDataS.cplex.solve() && (mipDataS.cplex.getStatus() == IloCplex.Status.Feasible || mipDataS.cplex.getStatus() == IloCplex.Status.Optimal)) {
            this.objectiveValue = (int) Math.round(mipDataS.cplex.getObjValue());// MathProgrammingUtil.doubleToInt(mipDataS.cplex.getObjValue());//(int)Math.round(mipDataS.cplex.getObjValue());
            //this.printSolution();
            this.optimal = mipDataS.cplex.getStatus() == IloCplex.Status.Optimal;
            this.isFeasible = true;
            for (int i = 0; i < varValues.length - 1; i++) {
                for (int j = 1; j < varValues.length; j++) {
                    if (i == j)
                        continue;
                    if (i == 0 && j == varValues.length - 1) {
                        continue;
                    }
                    varValues[i][j] = mipDataS.cplex.getValue(mipDataS.vars[i][j]);
                }
            }
            if(isTimeLimit){
                for (int i = 0; i < timeVarValues.length - 1; i++) {
                    timeVarValues[i] = mipDataS.cplex.getValue(mipDataS.timeVars[i]);
                }
            }

            //Verify solution
//			SolutionValidator.validate(btsp, solution);
//			if(solution.getObjective()!=this.objectiveValue)
//				throw new RuntimeException("Objective constructed solution deviates from MIP objective. MIP obj: "+this.objectiveValue+" constr sol obj: "+solution.getObjective());
//			System.out.println("Solution correct");
//			System.out.println("Solution:\n{}"+solution);
        } else if (mipDataS.cplex.getStatus() == IloCplex.Status.Infeasible) {
//			throw new RuntimeException("MipS infeasible");
            this.isFeasible = false;
            this.optimal = true;
        } else if (mipDataS.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
            System.out.println("No solution could be found in the given amount of time");
            this.isFeasible = true; //Technically there is no proof whether or not a feasible solution exists
            this.optimal = false;
        } else {
            //NOTE: when cplex does not find a solution before the default time out, it throws a Status Unknown exception
            //NOTE2: Might be required to extend the default runtime.
            throw new RuntimeException("Cplex solve terminated with status: " + mipDataS.cplex.getStatus());
        }

        mipDataS.cplex.end();
    }

    /**
     * Get bound on objective value
     */
    public double getLowerBound() {
        try {
            if (this.isOptimal())
                return this.getObjectiveValue();
            else
                return mipDataS.cplex.getBestObjValue();
        } catch (IloException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Indicates whether solution is optimal
     */
    public boolean isOptimal() {
        return optimal;
    }

    /**
     * Returns size of search tree (nr of nodes)
     */
    public int getNrOfNodes() {
        return mipDataS.cplex.getNnodes();
    }
}
