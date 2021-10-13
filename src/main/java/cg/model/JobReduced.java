package cg.model;

import lombok.Data;
import model.Job;


/**
 * @author Wang Li
 * @description this class is used to record the current reduced cost of the corresponding jobSimple
 * @date 8/31/21 4:42 PM
 */
@Data
public class JobReduced<E extends JobReduced> implements Comparable<E> {
    private Job job;
    private double reducedCost;

    public JobReduced(Job job) {
        this.job = job;
        this.reducedCost= job.getCostTotal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JobReduced))
            return false;
        JobReduced other = (JobReduced) o;
        return other.job.equals(this.job) ;
    }

    @Override
    public int hashCode() {
        return this.job.hashCode();
    }

    @Override
    public int compareTo(E o) {
        return Double.compare(this.getReducedCost(), o.getReducedCost());
//        return Double.compare(this.reducedCost, o.reducedCost);
    }
}
