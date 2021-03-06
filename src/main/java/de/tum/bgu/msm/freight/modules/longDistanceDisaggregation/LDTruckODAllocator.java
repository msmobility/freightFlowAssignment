package de.tum.bgu.msm.freight.modules.longDistanceDisaggregation;

import de.tum.bgu.msm.freight.FreightFlowUtils;
import de.tum.bgu.msm.freight.data.DataSet;
import de.tum.bgu.msm.freight.data.freight.*;
import de.tum.bgu.msm.freight.data.freight.longDistance.FlowSegment;
import de.tum.bgu.msm.freight.data.freight.longDistance.LDDistributionType;
import de.tum.bgu.msm.freight.data.freight.longDistance.LDTruckTrip;
import de.tum.bgu.msm.freight.data.freight.longDistance.SegmentType;
import de.tum.bgu.msm.freight.data.freight.Bound;
import de.tum.bgu.msm.freight.data.geo.*;
import de.tum.bgu.msm.freight.modules.Module;
import de.tum.bgu.msm.freight.modules.common.DistributionCenterUtils;
import de.tum.bgu.msm.freight.modules.common.SpatialDisaggregator;
import de.tum.bgu.msm.freight.properties.Properties;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * For each long distance truck, allocates and origin and a destination, whihc can be a location (zone), a microLocation (company), a distribution center
 * or an intermodal terminal. Moreover, aggregates flows by commodity and distribution center, if distribution centers are used.
 */
public class LDTruckODAllocator implements Module {

    private Properties properties;
    private DataSet dataSet;
    private double cumulatedV = 0; //debug-only variable
    private static final Logger logger = Logger.getLogger(LDTruckODAllocator.class);

    private Map<Integer, Map<CommodityGroup, Map<DistributionCenter, Double>>> weightDistributionCenters;


    @Override
    public void setup(DataSet dataset, Properties properties) {
        this.dataSet = dataset;
        this.properties = properties;
        initializeDistributionCenterWeight();

    }

    private void initializeDistributionCenterWeight() {
        weightDistributionCenters = new HashMap<>();
        for (int zoneId : dataSet.getZones().keySet()) {
            //logger.warn("Zone: " + zoneId);
            Zone zone = dataSet.getZones().get(zoneId);
            if (zone.isInStudyArea()) {
                weightDistributionCenters.putIfAbsent(zoneId, new HashMap<>());
                for (CommodityGroup commodityGroup : CommodityGroup.values()) {
                    logger.warn("Commodity: " + commodityGroup.toString());
                    if (!commodityGroup.getLongDistanceGoodDistribution().equals(LDDistributionType.DOOR_TO_DOOR)) {
                        weightDistributionCenters.get(zoneId).putIfAbsent(commodityGroup, new HashMap<>());

                        //count number of dc serving each micro zone
                        Map<Integer, Integer> dcServingEachMicroZone = new HashMap<>();
                        for (DistributionCenter distributionCenter : dataSet.getDistributionCenters().get(zoneId).get(commodityGroup).values()) {
                            for (InternalMicroZone internalMicroZone : distributionCenter.getZonesServedByThis()) {
                                dcServingEachMicroZone.put(internalMicroZone.getId(), dcServingEachMicroZone.getOrDefault(internalMicroZone.getId(), 0)+ 1 );
                            }

                        }

                        for (DistributionCenter distributionCenter : dataSet.getDistributionCenters().get(zoneId).get(commodityGroup).values()) {
                            double weight = 0;
                            for (InternalMicroZone internalMicroZone : distributionCenter.getZonesServedByThis()) {
                                double thisZoneWeight = internalMicroZone.getAttribute("population");
                                for (String jobType : properties.getJobTypes()){
                                    thisZoneWeight += internalMicroZone.getAttribute(jobType);
                                }
                                thisZoneWeight = thisZoneWeight / dcServingEachMicroZone.get(internalMicroZone.getId());
                                weight += thisZoneWeight;
                            }
                            logger.warn("DC: " + distributionCenter.getName() +  " has weight: " + weight);
                            weightDistributionCenters.get(zoneId).get(commodityGroup).put(distributionCenter, weight);
                        }
                    }
                }
            }
        }



        logger.info("Assigned weights to the distribution centers based on number of microZones");
    }

    @Override
    public void run() {
        subsampleTrucksAndAssignCoordinates();
        logger.warn(cumulatedV);
    }


    private void subsampleTrucksAndAssignCoordinates() {

        AtomicInteger counter = new AtomicInteger(0);
        for (FlowSegment flowSegment : dataSet.getAssignedFlowSegments()) {
            for (de.tum.bgu.msm.freight.data.freight.longDistance.LDTruckTrip LDTruckTrip : flowSegment.getTruckTrips()) {
                boolean validOrigin = true;
                boolean validDestination = true;
                validOrigin = setOrigin(LDTruckTrip);
                validDestination = setDestination(LDTruckTrip);

                if (validDestination && validOrigin) {
                    dataSet.getLDTruckTrips().add(LDTruckTrip);
                }

                if (counter.incrementAndGet() % 100000 == 0) {
                    logger.info("Assigned o/d to " + counter.get() + " LD trucks");
                }
            }
        }
        logger.warn(properties.getRand().nextDouble());
        logger.info("Assigned o/d to " + counter.get() + " LD trucks");

    }

    private boolean setOrigin(LDTruckTrip LDTruckTrip) {

        FlowSegment flowSegment = LDTruckTrip.getFlowSegment();

        Zone originZone = dataSet.getZones().get(flowSegment.getSegmentOrigin());
        Zone destinationZone = dataSet.getZones().get(flowSegment.getSegmentDestination());

        Coordinate origCoord;
        Commodity commodity = flowSegment.getCommodity();

        Bound bound;

        double thisTruckEffectiveLoad = LDTruckTrip.getLoad_tn();

        if (originZone.isInStudyArea()) {
            if (originZone.equals(destinationZone)) {
                bound = Bound.INTRAZONAL;
            } else {
                bound = Bound.OUTBOUND;
            }
        } else {
            bound = Bound.EXTRAZONAL;
        }


        if (flowSegment.getSegmentType().equals(SegmentType.POST)) {
            try {
                Terminal originTerminal = dataSet.getTerminals().get(flowSegment.getOriginTerminal());
                origCoord = originTerminal.getCoordinates();
            } catch (NullPointerException e) {
                origCoord = null;
            }

        } else {
            switch (commodity.getCommodityGroup().getLongDistanceGoodDistribution()) {
                case DOOR_TO_DOOR:
                    if (!originZone.isInStudyArea()) {
                        origCoord = originZone.getCoordinates(properties.getRand());
                    } else {
                        InternalZone internalZone = (InternalZone) originZone;
                        int microZoneId = SpatialDisaggregator.disaggregateToMicroZoneBusiness(commodity, internalZone.getMicroZones().values(), dataSet.getMakeTable(), properties.getRand());
                        origCoord = internalZone.getMicroZones().get(microZoneId).getCoordinates(properties.getRand());
                    }
                    break;
                case SINGLE_VEHICLE:
                    if (!originZone.isInStudyArea()) {
                        origCoord = originZone.getCoordinates(properties.getRand());
                    } else {
                        DistributionCenter originDistributionCenter = chooseDistributionCenterByWeight(originZone.getId(), commodity.getCommodityGroup());
                        LDTruckTrip.setOriginDistributionCenter(originDistributionCenter);
                        origCoord = originDistributionCenter.getCoordinates();
                        DistributionCenterUtils.addVolumeForSmallTruckDelivery(originDistributionCenter, commodity, bound, thisTruckEffectiveLoad, dataSet);
                    }
                    break;
                case PARCEL_DELIVERY:
                    if (!originZone.isInStudyArea()) {
                        //if zone does not have microzone
                        origCoord = originZone.getCoordinates(properties.getRand());
                    } else {
                        DistributionCenter originDistributionCenter = chooseDistributionCenterByWeight(originZone.getId(), commodity.getCommodityGroup());
                        origCoord = originDistributionCenter.getCoordinates();
                        LDTruckTrip.setOriginDistributionCenter(originDistributionCenter);
                        DistributionCenterUtils.addVolumeForParcelDelivery(originDistributionCenter, commodity, bound, thisTruckEffectiveLoad, dataSet);
                    }
                    break;
                default:
                    throw new RuntimeException("Unaccepted good distribution type");
            }
        }


        if (origCoord != null) {
            LDTruckTrip.setOrigCoord(origCoord);
            return true;
        } else {
            logger.warn("Cannot assign origin coordinates to flow with id: " + flowSegment.toString());
            return false;
        }
    }

    private boolean setDestination(LDTruckTrip LDTruckTrip) {

        FlowSegment flowSegment = LDTruckTrip.getFlowSegment();

        Zone originZone = dataSet.getZones().get(flowSegment.getSegmentOrigin());
        Zone destinationZone = dataSet.getZones().get(flowSegment.getSegmentDestination());

        Coordinate destCoord;
        Commodity commodity = flowSegment.getCommodity();

        Bound bound;

        double thisTruckEffectiveLoad = LDTruckTrip.getLoad_tn();

        if (destinationZone.isInStudyArea()) {
            if (originZone.equals(destinationZone)) {
                bound = Bound.INTRAZONAL;
            } else {
                bound = Bound.INBOUND;
            }
        } else {
            bound = Bound.EXTRAZONAL;
        }

        if (flowSegment.getSegmentType().equals(SegmentType.PRE)) {
            try {
                Terminal destinationTerminal = dataSet.getTerminals().get(flowSegment.getDestinationTerminal());
                destCoord = destinationTerminal.getCoordinates();
            } catch (NullPointerException e) {
                destCoord = null;
            }

        } else {
            switch (commodity.getCommodityGroup().getLongDistanceGoodDistribution()) {
                case DOOR_TO_DOOR:
                    if (!destinationZone.isInStudyArea()) {
                        destCoord = destinationZone.getCoordinates(properties.getRand());
                    } else {
                        InternalZone internalZone = (InternalZone) destinationZone;
                        int microZoneId = SpatialDisaggregator.disaggregateToMicroZoneBusiness(commodity, internalZone.getMicroZones().values(), dataSet.getUseTable(), properties.getRand());
                        destCoord = internalZone.getMicroZones().get(microZoneId).getCoordinates(properties.getRand());
                    }
                    break;
                case SINGLE_VEHICLE:
                    if (!destinationZone.isInStudyArea()) {
                        destCoord = destinationZone.getCoordinates(properties.getRand());
                    } else {
                        DistributionCenter destinationDistributionCenter = chooseDistributionCenterByWeight(destinationZone.getId(), commodity.getCommodityGroup());
                        destCoord = destinationDistributionCenter.getCoordinates();
                        LDTruckTrip.setDestinationDistributionCenter(destinationDistributionCenter);
                        DistributionCenterUtils.addVolumeForSmallTruckDelivery(destinationDistributionCenter, commodity, bound, thisTruckEffectiveLoad, dataSet);
                    }
                    break;
                case PARCEL_DELIVERY:
                    if (!destinationZone.isInStudyArea()) {
                        destCoord = destinationZone.getCoordinates(properties.getRand());
                    } else {
                        DistributionCenter destinationDistributionCenter = chooseDistributionCenterByWeight(destinationZone.getId(), commodity.getCommodityGroup());
                        destCoord = destinationDistributionCenter.getCoordinates();
                        LDTruckTrip.setDestinationDistributionCenter(destinationDistributionCenter);
                        DistributionCenterUtils.addVolumeForParcelDelivery(destinationDistributionCenter, commodity, bound, thisTruckEffectiveLoad, dataSet);
                    }
                    break;
                default:
                    throw new RuntimeException("Unaccepted good distribution type");

            }
        }

        if (destCoord != null) {
            LDTruckTrip.setDestCoord(destCoord);
            return true;
        } else {
            logger.warn("Cannot assign destination coordinates to flow with id: " + flowSegment.toString());
            return false;
        }
    }

    @Deprecated
    private DistributionCenter chooseRandomDistributionCenter(int zoneId, CommodityGroup commodityGroup) {
        //todo probably not the best way to divide. Think on capacity

        Map<DistributionCenter, Double> probabilities = new LinkedHashMap<>();
        Map<Integer, DistributionCenter> distributionCenters = dataSet.getDistributionCentersForZoneAndCommodityGroup(zoneId, commodityGroup);
        ArrayList<Integer> listOfDc = distributionCenters.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
        for (int key : listOfDc){
            DistributionCenter distributionCenter = distributionCenters.get(key);
            probabilities.put(distributionCenter, 1.);
        }

        return FreightFlowUtils.select(probabilities, FreightFlowUtils.getSum(probabilities.values()), properties.getRand());
    }

    //todo not sure how this works!
    private DistributionCenter chooseDistributionCenterByWeight(int zoneId, CommodityGroup commodityGroup) {
        DistributionCenter dc = FreightFlowUtils.select(weightDistributionCenters.get(zoneId).get(commodityGroup),
                FreightFlowUtils.getSum(weightDistributionCenters.get(zoneId).get(commodityGroup).values()), properties.getRand());
        return dc;
    }


}
