package cg.pricing;

import cg.column.JobColumn;
import cg.model.Crowdsourcing;
import cg.model.JobReduced;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import util.Constants;
import util.TopK;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:57 AM
 */
public class FkPricingSolver extends AbstractPricingProblemSolver<Crowdsourcing, JobColumn, PricingProblem> {
    public List<JobColumn> columns;

    /**
     * Creates a new solver instance for a particular pricing problem
     *
     * @param dataModel      data model
     * @param pricingProblem pricing problem
     */
    public FkPricingSolver(Crowdsourcing dataModel, PricingProblem pricingProblem) {
        super(dataModel, pricingProblem);
    }

    @Override
    protected List<JobColumn> generateNewColumns() throws TimeLimitExceededException {
        PriorityQueue<JobReduced> queue = TopK.bottomK(Constants.columnNumIte, dataModel.jobReducedArray, Constants.gapCG);
        List<JobColumn> newColumList=new ArrayList<>();
        Set<JobReduced> jobRemoved=new HashSet<>();
        for(JobReduced jobReduced:queue){
            JobColumn jobColumn=new JobColumn(pricingProblem,false,"FkPricingSolver",jobReduced);
            newColumList.add(jobColumn);
            if (Constants.isCGAllColumns) {
                dataModel.instance.getJobMipList().add(jobColumn.jobReduced.getJob());
            }
            jobRemoved.add(jobReduced);
        }

        dataModel.jobReducedArray.removeAll(jobRemoved);
        return newColumList;
    }

    @Override
    protected void setObjective() {

    }

    @Override
    public void close() {

    }
}
