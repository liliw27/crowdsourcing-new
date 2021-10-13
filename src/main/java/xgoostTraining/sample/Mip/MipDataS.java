package xgoostTraining.sample.Mip;

import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:52 AM
 */
public class MipDataS {
    public final IloCplex cplex;
    public final IloIntVar[][] vars;
    public  IloIntVar[] timeVars;
    public Map<DefaultWeightedEdge, Double> aggregatedArcUsageValues;

    public MipDataS(IloCplex cplex, IloIntVar[][] vars) {
        this.cplex=cplex;
        this.vars=vars;
    }
    public MipDataS(IloCplex cplex, IloIntVar[][] vars,IloIntVar[] timeVars) {
        this.cplex=cplex;
        this.vars=vars;
        this.timeVars=timeVars;
    }
}
