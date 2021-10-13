package cg.model;

import model.InstanceJob;
import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Wang Li
 * @description
 * @date 8/30/21 9:44 AM
 */
public class Crowdsourcing implements ModelInterface {
    public final InstanceJob instance;
    public final Set<JobReduced> jobReducedArray;
    public Crowdsourcing(InstanceJob instance){
        this.instance=instance;
        jobReducedArray=new HashSet<>(instance.getJobList().size());
        for(int i=0;i<instance.getJobList().size();i++){
            JobReduced jobReduced=new JobReduced(instance.getJobList().get(i));
            jobReducedArray.add(jobReduced);
        }
    }
    @Override
    public String getName() {
        return null;
    }
}
