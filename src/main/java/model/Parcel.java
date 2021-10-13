package model;

import lombok.Data;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 10:45 AM
 */
@Data
public class Parcel {
    private int index;
    private int nodeIndex;
    private int lat;
    private int lng;
    private int deadline;
    @Deprecated
    private Station nearestStation;

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o)
            return true;
        if (!(o instanceof Parcel))
            return false;
        Parcel other = (Parcel) o;
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
