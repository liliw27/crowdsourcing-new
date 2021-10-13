package cg.masterProblem;

import cg.column.JobColumn;
import cg.model.Crowdsourcing;
import cg.pricing.PricingProblem;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:41 AM
 */
public class CrowdsoucingMasterData extends MasterData<Crowdsourcing, JobColumn, PricingProblem, IloNumVar> {

    /** Cplex instance **/
    public final IloCplex cplex;
    /** List of pricing problems **/
    public final List<PricingProblem> pricingProblems;

    //Record all positive-value columns
    public List<JobColumn> jobColumns;

    /**
     * Creates a new MasterData object
     *
     * @param varMap A double map which stores the variables. The first key is the pricing problem, the second key is a column and the value is a variable object, e.g. an IloNumVar in cplex.
     */
    public CrowdsoucingMasterData(IloCplex cplex, List<PricingProblem> pricingProblems, Map<PricingProblem, OrderedBiMap<JobColumn, IloNumVar>> varMap) {
        super(varMap);
        this.cplex=cplex;
        this.pricingProblems=pricingProblems;
    }
}
