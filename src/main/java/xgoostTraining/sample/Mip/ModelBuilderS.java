package xgoostTraining.sample.Mip;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import model.InstanceSample;
import util.Constants;
import xgoostTraining.sample.Mip.MipDataS;
import xgoostTraining.sample.Mip.cut.LazyCutCallbackImpl;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:53 AM
 */
public class ModelBuilderS {

    private final InstanceSample dataModel;
    private MipDataS mipDataS;

    public ModelBuilderS(InstanceSample dataModel,boolean isTimeLimit) throws IloException {
        this.dataModel = dataModel;
        if(isTimeLimit){
            this.buildModelWithDdline();
        }else{
            this.buildModel();
        }
    }

    /**
     * Solve the root node of the Branch and Bound tree.
     */
    public MipDataS getLP() throws IloException {
        mipDataS.cplex.setParam(IloCplex.IntParam.NodeLim, 0);
        return mipDataS;
    }

    /**
     * Solve the entire Branch and Bound tree
     */
    public MipDataS getILP() throws IloException {
        mipDataS.cplex.setParam(IloCplex.IntParam.NodeLim, 210000000); //Continue search
        //mipDataS.cplex.setParam(IloCplex.BooleanParam.PreInd, false);
        mipDataS.cplex.setParam(IloCplex.DoubleParam.TiLim, Constants.MAXBRANCHBOUNDTIME); //set time limit in seconds
        mipDataS.cplex.setParam(IloCplex.IntParam.Threads, Constants.MAXTHREADS);
        mipDataS.cplex.setOut(null); //Disable Cplex output
        return mipDataS;
    }

    private void buildModel() throws IloException {
        IloCplex cplex = new IloCplex();
        int parcelNum = dataModel.getParcelNum();
        int[][] travelTimeMatrix = dataModel.getTravelTimeMatrix();
        IloIntVar[][] vars = new IloIntVar[parcelNum + 2][parcelNum + 2];

        //create variables

//        for (int i = 0; i < parcelNum + 2; i++) {
//            for (int j = 0; j < parcelNum + 2; j++) {
//                IloIntVar var = cplex.boolVar("x_" + i + "_" + j);
//                vars[i][j] = var;
//            }
//        }
        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                IloIntVar var = cplex.boolVar("x_" + i + "_" + j);
                vars[i][j] = var;
            }
        }

        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                obj.addTerm(travelTimeMatrix[i][j], vars[i][j]);
            }
        }
        cplex.addMinimize(obj);

        //create constraints
        //1.depart from station
        IloLinearIntExpr expr = cplex.linearIntExpr();
        for (int i = 1; i < parcelNum + 1; i++) {
            expr.addTerm(1, vars[0][i]);
        }
        cplex.addEq(expr, 1, "depart");

        //2.arrive at destination
        expr = cplex.linearIntExpr();
        for (int i = 1; i < parcelNum + 1; i++) {
            expr.addTerm(1, vars[i][parcelNum + 1]);
        }
        cplex.addEq(expr, 1, "arrive");

        //3.out flow
        for (int i = 1; i < parcelNum + 1; i++) {
            expr = cplex.linearIntExpr();
            for (int j = 1; j < parcelNum + 2; j++) {
                if(i==j){
                    continue;
                }
                expr.addTerm(1, vars[i][j]);
            }
            cplex.addEq(expr, 1, "out flow_" + i);
        }

        //4.in flow
        for (int j = 1; j < parcelNum + 1; j++) {
            expr = cplex.linearIntExpr();
            for (int i = 0; i < parcelNum + 1; i++) {
                if(i==j){
                    continue;
                }
                expr.addTerm(1, vars[i][j]);
            }
            cplex.addEq(expr, 1, "in flow_" + j);
        }

        mipDataS = new MipDataS(cplex, vars);
        cplex.use(new LazyCutCallbackImpl(dataModel,mipDataS));
    }
    private void buildModelWithDdline() throws IloException {
        IloCplex cplex = new IloCplex();
        int parcelNum = dataModel.getParcelNum();
        int[][] travelTimeMatrix = dataModel.getTravelTimeMatrix();
        IloIntVar[][] vars = new IloIntVar[parcelNum + 2][parcelNum + 2];
        IloIntVar[]timeVars=new IloIntVar[parcelNum+2];

        //create variables

//        for (int i = 0; i < parcelNum + 2; i++) {
//            for (int j = 0; j < parcelNum + 2; j++) {
//                IloIntVar var = cplex.boolVar("x_" + i + "_" + j);
//                vars[i][j] = var;
//            }
//        }
        timeVars[0]=cplex.intVar(dataModel.getTimeLimit()[0],Integer.MAX_VALUE,"t_0");
        for (int i=1;i<parcelNum+2;i++){
            timeVars[i]=cplex.intVar(0,dataModel.getTimeLimit()[i],"t_"+i);
        }

        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                IloIntVar var = cplex.boolVar("x_" + i + "_" + j);
                vars[i][j] = var;
            }
        }

        //Create objective: Minimize weighted travel travelTime
        IloLinearNumExpr obj = cplex.linearNumExpr();

        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                obj.addTerm(travelTimeMatrix[i][j], vars[i][j]);
            }
        }
        cplex.addMinimize(obj);

        //create constraints
        //1.depart from station
        IloLinearIntExpr expr = cplex.linearIntExpr();
        for (int i = 1; i < parcelNum + 1; i++) {
            expr.addTerm(1, vars[0][i]);
        }
        cplex.addEq(expr, 1, "depart");

        //2.arrive at destination
        expr = cplex.linearIntExpr();
        for (int i = 1; i < parcelNum + 1; i++) {
            expr.addTerm(1, vars[i][parcelNum + 1]);
        }
        cplex.addEq(expr, 1, "arrive");

        //3.out flow
        for (int i = 1; i < parcelNum + 1; i++) {
            expr = cplex.linearIntExpr();
            for (int j = 1; j < parcelNum + 2; j++) {
                if(i==j){
                    continue;
                }
                expr.addTerm(1, vars[i][j]);
            }
            cplex.addEq(expr, 1, "out flow_" + i);
        }

        //4.in flow
        for (int j = 1; j < parcelNum + 1; j++) {
            expr = cplex.linearIntExpr();
            for (int i = 0; i < parcelNum + 1; i++) {
                if(i==j){
                    continue;
                }
                expr.addTerm(1, vars[i][j]);
            }
            cplex.addEq(expr, 1, "in flow_" + j);
        }
        //5.working duration
        expr = cplex.linearIntExpr();
        expr.addTerm(1, timeVars[0]);
        cplex.addGe(expr,dataModel.getTimeLimit()[0],"earliest departure");
        expr = cplex.linearIntExpr();
        expr.addTerm(1, timeVars[parcelNum+1]);
        cplex.addLe(expr,dataModel.getTimeLimit()[parcelNum+1],"latest arrival");

        //6.deadline of the parcels
        for(int i=1;i<parcelNum+1;i++){
            expr = cplex.linearIntExpr();
            expr.addTerm(1, timeVars[i]);
            cplex.addLe(expr,dataModel.getTimeLimit()[i],"deadline_"+i);
        }

        //7.time relationship of adjacent vertex
        for (int i = 0; i < travelTimeMatrix.length - 1; i++) {
            for (int j = 1; j < travelTimeMatrix.length; j++) {
                if (i == j)
                    continue;
                if (i == 0 && j == travelTimeMatrix.length-1) {
                    continue;
                }
                int M=1000;
                int distanceIJ=travelTimeMatrix[i][j];
                expr = cplex.linearIntExpr();
                expr.addTerm(1, timeVars[i]);
                expr.addTerm(-1, timeVars[j]);
                expr.addTerm(M,vars[i][j]);
                cplex.addLe(expr,M-distanceIJ,"timeConstraint_"+i+"_"+j);
            }
        }
        mipDataS = new MipDataS(cplex, vars,timeVars);
//        cplex.exportModel("mipTimeLimit.lp");
//        cplex.use(new LazyCutCallbackImpl(dataModel,mipDataS));
    }
}
