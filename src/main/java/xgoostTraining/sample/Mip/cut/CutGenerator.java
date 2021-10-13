package xgoostTraining.sample.Mip.cut;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloRange;
import model.Instance;
import model.InstanceSample;
import xgoostTraining.sample.Mip.MipDataS;

import java.util.List;

/**
 * @author Wang Li
 * @description
 * @date 9/15/21 7:54 PM
 */
public abstract class CutGenerator {
    //    protected final Logger logger = LoggerFactory.getLogger(CutGenerator.class);

    protected final InstanceSample instance;
    protected final MipDataS mipData;
    protected List<IloConstraint> validInequalities;

    public CutGenerator(InstanceSample instance, MipDataS mipData){
        this.instance=instance;
        this.mipData=mipData;
    }
    public abstract List<IloRange> generateInqualities() throws IloException;

}
