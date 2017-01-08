package se.hshn.de.pathtracker;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by florens on 08.01.17.
 */

public class MeasurementDataset {

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private Long id;

    public String getMeasurements() {
        return measurements;
    }

    public void setMeasurements(String measurements) {
        this.measurements = measurements;
    }

    private String measurements;
}
