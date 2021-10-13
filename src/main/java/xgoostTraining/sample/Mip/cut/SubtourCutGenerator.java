package xgoostTraining.sample.Mip.cut;

import ilog.concert.IloException;
import ilog.concert.IloLinearIntExpr;
import ilog.concert.IloRange;
import model.Instance;
import model.InstanceSample;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jorlib.alg.tsp.separation.SubtourSeparator;
import xgoostTraining.sample.Mip.MipDataS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 9/15/21 7:55 PM
 */
public class SubtourCutGenerator extends CutGenerator {
    private Map<Set<Integer>, IloRange> subtourCuts;


    //We use the subtour separator provided in jORLib
    private final SubtourSeparator<Integer, DefaultWeightedEdge> subtourSeparator;

    public SubtourCutGenerator(InstanceSample instance, MipDataS mipData) {
        super(instance, mipData);
        subtourSeparator=new SubtourSeparator<>(instance.getRoutingGraph());
        subtourCuts=new HashMap<>();
    }

    @Override
    public List<IloRange> generateInqualities() throws IloException {
        List<IloRange> separatedInequalities=new ArrayList<>();
        subtourSeparator.separateSubtour(mipData.aggregatedArcUsageValues);
        if(subtourSeparator.hasSubtour()){
            Set<Integer> cut=subtourSeparator.getCutSet();
            if(cut.contains(0)){ //Get the other side
                Set<Integer> otherCut=new LinkedHashSet<>(instance.getRoutingGraph().vertexSet());
                otherCut.removeAll(cut);
                cut=otherCut;

//                throw new RuntimeException("Found violated subour ineq!");
            }
            IloRange inequality=this.buildInequality(cut);
//            System.out.println("Found violated subtour ineq!");
            separatedInequalities.add(inequality);
        }
//        else{
//            System.out.println("Cut value: "+subtourSeparator.getCutValue());
//        }
        return separatedInequalities;
    }

    private IloRange buildInequality(Set<Integer> cut) throws IloException {
        if(subtourCuts.containsKey(cut))
            return subtourCuts.get(cut);
        IloLinearIntExpr expr=mipData.cplex.linearIntExpr();
        for(int i : cut){
            for(int j : cut){
                if(i==j || !instance.getRoutingGraph().containsEdge(i,j)){
                    continue;
                }
                    expr.addTerm(1, mipData.vars[i][j]);
            }
        }
        IloRange inequality=mipData.cplex.le(expr, cut.size()-1, "subtour"+subtourCuts.size());
        subtourCuts.put(cut, inequality);
        return  inequality;
    }
}
