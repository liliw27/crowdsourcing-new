package cg.model;

import lombok.Data;
import model.Job;


/**
 * @author Wang Li
 * @description this class is used to record the current reduced cost of the corresponding jobSimple
 * @date 8/31/21 4:42 PM
 */
@Deprecated
public class JobReducedforHeur extends JobReduced<JobReducedforHeur> implements Comparable<JobReducedforHeur> {

    public JobReducedforHeur(Job job) {
        super(job);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JobReducedforHeur))
            return false;
        JobReducedforHeur other = (JobReducedforHeur) o;
        return other.getJob().equals(this.getJob()) ;
    }

    @Override
    public int hashCode() {
        return this.getJob().hashCode();
    }

    @Override
    public int compareTo(JobReducedforHeur o) {
        return Double.compare(this.getReducedCost()/this.getJob().getParcelList().size(), o.getReducedCost()/o.getJob().getParcelList().size());
//        return Double.compare(this.reducedCost, o.reducedCost);
    }
}
