package cg.column;

import cg.model.Crowdsourcing;
import cg.model.JobReduced;
import cg.pricing.PricingProblem;
import model.Parcel;
import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:39 AM
 */

public class JobColumn extends AbstractColumn<Crowdsourcing, PricingProblem> implements Comparable<JobColumn>{

    public final JobReduced jobReduced;


    /**
     * Constructs a new column
     *
     * @param associatedPricingProblem Pricing problem to which this column belongs
     * @param isArtificial             Is this an artificial column?
     * @param creator                  Who/What created this column?
     */
    public JobColumn(PricingProblem associatedPricingProblem, boolean isArtificial, String creator, JobReduced jobReduced) {
        super(associatedPricingProblem, isArtificial, creator);
        this.jobReduced = jobReduced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JobColumn))
            return false;
        JobColumn other = (JobColumn) o;
        return other.jobReduced.getJob().equals(this.jobReduced.getJob());
    }

    @Override
    public int hashCode() {
        return jobReduced.getJob().getJobIndex();
    }


    @Override
    public String toString() {
        String parcels="";
        for(Parcel parcel:this.jobReduced.getJob().getParcelList()){
            parcels+=parcel.getIndex()+", ";
        }

        String s="Value: " + this.value +"Reduced cost: "+jobReduced.getReducedCost()+"job cost: "+jobReduced.getJob().getCostTotal()+ " Parcel set: "+parcels
                + " Worker Index: " + this.jobReduced.getJob().getWorker().getIndex() + " Station Index: " + this.jobReduced.getJob().getStation().getIndex()
                + " travelTime: " + this.jobReduced.getJob().getTravelTime() + " otPunish: " + this.jobReduced.getJob().getOvertimePunish() + " creator: " + this.creator;
        return  s;
    }

    @Override
    public int compareTo(JobColumn o) {
        return Double.compare(o.jobReduced.getReducedCost(), this.jobReduced.getReducedCost());
    }


}
