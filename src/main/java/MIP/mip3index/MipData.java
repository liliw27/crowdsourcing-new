package MIP.mip3index;

import ilog.concert.IloIntVar;
import ilog.cplex.IloCplex;

import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 8/6/21 9:52 AM
 */
@Deprecated
public class MipData {
    public final IloCplex cplex;
    public final IloIntVar[][][] vars;

    public MipData(IloCplex cplex, IloIntVar[][][] vars) {
        this.cplex=cplex;
        this.vars=vars;
    }
}
