package MIP.mip3index;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.InstanceSimple;
import util.Constants;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:53 AM
 */
@Deprecated
public class ModelBuilder {

    private final InstanceSimple dataModel;
    private MipData mipData;

    public ModelBuilder(InstanceSimple dataModel) throws IloException {
        this.dataModel = dataModel;
        this.buildModel();
    }

    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public MipData getLP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipData;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public MipData getILP() throws IloException {
        mipData.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipData.cplex.setParam(IloCplex.DoubleParam.TiLim, Constants.MAXBRANCHBOUNDTIME); //set time limit in seconds
        mipData.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipData.cplex.setOut(null); //Disable Cplex output
        return mipData;
    }

    private void buildModel() throws IloException {
        IloCplex cplex = new IloCplex();
        int stationNum = dataModel.getStationNum();
        int workerNum = dataModel.getWorkerNum();
        int parcelNum = dataModel.getParcelNum();
        double[][][] cost = dataModel.getCost();
        IloIntVar[][][] vars = new IloIntVar[stationNum][workerNum][parcelNum];

        //create variables

        for (int i = 0; i < stationNum; i++) {
            for (int j = 0; j < workerNum; j++) {
                for (int k = 0; k < parcelNum; k++) {
                    IloIntVar var = cplex.boolVar("x_" + i + "_" + j + "_" + k);
                    vars[i][j][k] = var;
                }
            }
        }


        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for (int i = 0; i < stationNum; i++) {
            for (int j = 0; j < workerNum; j++) {
                for (int k = 0; k < parcelNum; k++) {
                    obj.addTerm(cost[i][j][k], vars[i][j][k]);
                }
            }
        }
        cplex.addMinimize(obj);

        //create constraints
        //1.every parcel is delivered once
        for (int k = 0; k < parcelNum; k++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int i = 0; i < stationNum; i++) {
                for (int j = 0; j < workerNum; j++) {
                    expr.addTerm(1, vars[i][j][k]);
                }
            }
            cplex.addEq(expr, 1, "deliOnce_" + k);
        }
        //2.station capacity

        for (int i = 0; i < stationNum; i++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int k = 0; k < parcelNum; k++) {
                for (int j = 0; j < workerNum; j++) {
                    expr.addTerm(1, vars[i][j][k]);
                }
            }
            cplex.addLe(expr, dataModel.getStationCapacity(), "stationCapacity_" + i);
        }
        //3.worker capacity
        for (int j = 0; j < workerNum; j++) {
            IloLinearIntExpr expr = cplex.linearIntExpr();
            for (int k = 0; k < parcelNum; k++) {
                for (int i = 0; i < stationNum; i++) {
                    expr.addTerm(1, vars[i][j][k]);
                }
            }
            cplex.addLe(expr, dataModel.getWorkerCapacity(), "workerCapacity_" + j);
        }
        mipData=new MipData(cplex, vars);
    }
}
