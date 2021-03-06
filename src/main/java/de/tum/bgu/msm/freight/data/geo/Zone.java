package de.tum.bgu.msm.freight.data.geo;

import org.locationtech.jts.geom.Coordinate;

import java.util.Random;

public interface Zone {

    /**
     * get random coordinate whithin a zone.
     * @return
     */
    Coordinate getCoordinates(Random random);

    String getName();

    int getId();

    /**
     * @return true if the zone is in the study area - destination or origin is at one of the selected cities
     */
    boolean isInStudyArea();

}
