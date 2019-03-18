package de.tum.bgu.msm.freight.data.freight;

import org.matsim.api.core.v01.Coord;

public class LongDistanceTruckTrip implements TruckTrip {
    private Coord origCoord;
    private Coord destCoord;
    private FlowSegment flowSegment;
    private boolean load;

    public LongDistanceTruckTrip(Coord origCoord, Coord destCoord, FlowSegment FlowSegment, boolean load) {
        this.origCoord = origCoord;
        this.destCoord = destCoord;
        this.flowSegment = FlowSegment;
        this.load = load;
    }

    public static String getHeader() {
        StringBuilder builder = new StringBuilder();

        builder.append("id").append(",").
                append("toDestination").append(",").
                append("weight_kg").append(",").
                append("commodity").append(",").
                append("originX()").append(",").
                append("originY()").append(",").
                append("destX()").append(",").
                append("destY").append(",").
                append("distributionCenter");

        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("").append(",").
                append("").append(",").
                append("0").append(",").
                append(flowSegment.getCommodity()).append(",").
                append(origCoord.getX()).append(",").
                append(destCoord.getY()).append(",").
                append(destCoord.getX()).append(",").
                append(destCoord.getY()).append(",").
                append("");

        return builder.toString();


    }

    @Override
    public Coord getOrigCoord() {
        return origCoord;
    }

    @Override
    public Coord getDestCoord() {
        return destCoord;
    }

    public de.tum.bgu.msm.freight.data.freight.FlowSegment getFlowSegment() {
        return flowSegment;
    }

    public boolean isLoad() {
        return load;
    }
}
