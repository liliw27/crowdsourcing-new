package cg.pricing;

import cg.model.Crowdsourcing;
import cg.model.JobReduced;
import model.Job;
import model.Parcel;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

import java.util.Map;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:53 AM
 */
public class PricingProblem extends AbstractPricingProblem<Crowdsourcing> {
    public Map<String, double[]> dualCostsMap;
//    public List<JobColumn> jobColumns;

    /**
     * Create a new Pricing Problem
     *
     * @param dataModel Data model
     * @param name      Name of the pricing problem
     */
    public PricingProblem(Crowdsourcing dataModel, String name) {
        super(dataModel, name);
    }
    public void initPricingProblem(Map<String, double[]> dualCostsMap) {
        this.dualCostsMap = dualCostsMap;
//        this.jobColumns = jobColumns;

        /*update reduced cost of each jobSimple*/
        for(JobReduced jobReduced:dataModel.jobReducedArray){
            Job job =jobReduced.getJob();
            double reducedCost= job.getCostTotal();
            reducedCost-=dualCostsMap.get("stationCapacity")[job.getStation().getIndex()];
            reducedCost-=dualCostsMap.get("workerOneJobAtMost")[dataModel.instance.getWorkerIdxMap().get(job.getWorker())];

            for(Parcel parcel:job.getParcelList()){
                int index=dataModel.instance.getParcelIdxMap().get(parcel);
                reducedCost-=dualCostsMap.get("parcelvisitedConstraints")[index];
            }
            jobReduced.setReducedCost(reducedCost);
        }

    }
}
