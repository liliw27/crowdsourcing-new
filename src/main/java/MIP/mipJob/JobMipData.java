package MIP.mipJob;

import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

/**
 * @author Wang Li
 * @description
 * @date 9/1/21 7:15 PM
 */
public class JobMipData {
    public final IloCplex cplex;
    public final IloIntVar[] vars;

    public JobMipData(IloCplex cplex, IloIntVar[] vars) {
        this.cplex=cplex;
        this.vars=vars;
    }
}
