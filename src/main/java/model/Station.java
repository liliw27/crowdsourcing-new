package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:48 AM
 */
@Data
public class Station {
    private int index;
    private int nodeIndex;
    private int lat;
    private int lng;
    private int currentCapRemained;

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o)
            return true;
        if (!(o instanceof Worker))
            return false;
        Worker other = (Worker) o;
        if (other.getIndex() != this.getIndex()){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return index;
    }
}
