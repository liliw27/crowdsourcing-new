package xgoostTraining.sample.Mip.cut;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import model.Instance;
import model.InstanceSample;
import org.jgrapht.graph.DefaultWeightedEdge;
import xgoostTraining.sample.Mip.MipDataS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 9/16/21 10:23 AM
 */
public class LazyCutCallbackImpl extends IloCplex.LazyConstraintCallback{

    public static final double PRECISION = 1.0E-6D;
    private final InstanceSample instance;
    private final MipDataS mipData;

    private final CutGenerator[] cutGenerators;

    public LazyCutCallbackImpl(InstanceSample instance, MipDataS mipData) throws IloException
    {
        this.instance = instance;
        this.mipData = mipData;

        cutGenerators = new CutGenerator[]{
                /*new CliqueCutGenerator(instance, mipData),
				new CapacityCutGenerator(instance, mipData),*/
                new SubtourCutGenerator (instance, mipData)
        };
    }

    @Override
    protected void main() throws IloException {

        //Step 1: Get x_ij^tk values. Find used Edges.



        Map<DefaultWeightedEdge, Double> aggregatedArcUsageValues=new LinkedHashMap<>();

        for(DefaultWeightedEdge edge:instance.getRoutingGraph().edgeSet()){
            int i=instance.getRoutingGraph().getEdgeSource(edge);
            int j=instance.getRoutingGraph().getEdgeTarget(edge);
            double value=this.getValue(mipData.vars[i][j]);
            aggregatedArcUsageValues.put(edge,value) ;
        }


        mipData.aggregatedArcUsageValues =aggregatedArcUsageValues;

//		logger.info("Invoking separation for user cut callback");
        //Find for a cut which violates the capacity constraints.
        List<IloRange> validInequalities=new ArrayList<IloRange>();
//        System.out.println("Separating ineq");
        for(CutGenerator cutGen: cutGenerators) {
            validInequalities.addAll(cutGen.generateInqualities());
            if(!validInequalities.isEmpty()) //Found one or more cuts
                break;
        }

        for(IloRange inequality: validInequalities){
            this.add(inequality);
        }

//		logger.info("Finished invoking separation for user cut callback");
        //System.out.println("END Running user cuts");
    }
}
