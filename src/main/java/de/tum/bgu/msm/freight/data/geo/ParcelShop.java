package de.tum.bgu.msm.freight.data.geo;

import de.tum.bgu.msm.freight.data.freight.CommodityGroup;
import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.List;

public class ParcelShop {

    private final int id;
    private final String name;
    private final Coord coord_gk4;
    private final CommodityGroup commodityGroup;
    private final DistributionCenter distributionCenter;
    private final int zoneId;
    private final int microZoneId;
    private final List<InternalMicroZone> zonesServedByThis;


    public ParcelShop(int id, String name, Coord coord_gk4, CommodityGroup commodityGroup,
                      DistributionCenter distributionCenter, int zoneId, int microZoneId) {
        this.id = id;
        this.name = name;
        this.coord_gk4 = coord_gk4;
        this.commodityGroup = commodityGroup;
        this.distributionCenter = distributionCenter;
        this.zoneId = zoneId;
        this.microZoneId = microZoneId;
        this.zonesServedByThis = new ArrayList<>();
    }
}
