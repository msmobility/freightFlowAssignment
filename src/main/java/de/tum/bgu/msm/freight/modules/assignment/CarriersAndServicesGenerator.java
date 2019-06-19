package de.tum.bgu.msm.freight.modules.assignment;

import de.tum.bgu.msm.freight.data.DataSet;
import de.tum.bgu.msm.freight.data.freight.urban.Parcel;
import de.tum.bgu.msm.freight.data.freight.urban.ParcelDistributionType;
import de.tum.bgu.msm.freight.data.geo.DistributionCenter;
import de.tum.bgu.msm.freight.data.geo.MicroDepot;
import de.tum.bgu.msm.freight.properties.Properties;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CarriersAndServicesGenerator {

    private DataSet dataSet;
    private Network network;
    private Properties properties;
    private static final Logger logger = Logger.getLogger(CarriersAndServicesGenerator.class);
    /**
     * this maps keeps the parcels in microdepots to assign first the trips to microdepot
     * with motorized vehicles and later the cargo bike trips from the depot
     */
    private Map<MicroDepot, List<Parcel>> parcelsByMicrodepot = new HashMap<>();

    public CarriersAndServicesGenerator(DataSet dataSet, Network network, Properties properties) {
        this.dataSet = dataSet;
        this.network = network;
        this.properties = properties;
    }

    public void generateCarriers(Carriers carriers, CarrierVehicleTypes types) {

        AtomicInteger carrierCounter = new AtomicInteger(0);

        for (DistributionCenter distributionCenter : dataSet.getParcelsByDistributionCenter().keySet()) {

            List<Parcel> parcelsInThisDistributionCenter = dataSet.getParcelsByDistributionCenter().get(distributionCenter);

            Carrier carrier = CarrierImpl.newInstance(Id.create(carrierCounter.getAndIncrement(), Carrier.class));
            carriers.addCarrier(carrier);

            Coordinate coordinates = distributionCenter.getCoordinates();

            Link link = NetworkUtils.getNearestLink(network, new Coord(coordinates.x, coordinates.y));
            Id<Link> linkId = link.getId();

            CarrierVehicleType type = types.getVehicleTypes().get(Id.create("van", VehicleType.class));
            carrier.getCarrierCapabilities().getVehicleTypes().add(type);

            carrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

            //guess vehicle number and add vehicles
            int numberOfToursPerDay = 2;
            int numberOfVehicles = (int) (parcelsInThisDistributionCenter.size() / type.getCarrierVehicleCapacity()/numberOfToursPerDay);
            for (int i = 0; i < numberOfVehicles; i++) {
                CarrierVehicle vehicle = getSpecificVehicle(type, carrier.getId(), i, linkId,  7 * 60 * 60, 17 * 60 * 60);
                carrier.getCarrierCapabilities().getCarrierVehicles().
                        add(vehicle);

            }



            for(MicroDepot microDepot : distributionCenter.getMicroDeportsServedByThis()) {
                //initialize
                parcelsByMicrodepot.put(microDepot, new ArrayList<>());
            }

            createDeliveriesByMotorizedModes(parcelsInThisDistributionCenter, carrier);

            for (MicroDepot microDepot : distributionCenter.getMicroDeportsServedByThis()) {

                //create a microdepot Carrier (sub-carrier) with only bicycles
                Carrier microDepotCarrier = CarrierImpl.newInstance(Id.create(microDepot.getId() + "_microDepot", Carrier.class));
                carriers.addCarrier(microDepotCarrier);
                Coordinate microDepotCoord = microDepot.getCoord_gk4();
                Link microDepotLink = NetworkUtils.getNearestLink(network, new Coord(microDepotCoord.x, microDepotCoord.y));
                Id<Link> microDepotLinkId = microDepotLink.getId();
                CarrierVehicleType cargoBikeType = types.getVehicleTypes().get(Id.create("cargoBike", VehicleType.class));
                microDepotCarrier.getCarrierCapabilities().getVehicleTypes().add(cargoBikeType);

                List<Parcel> parcelsInThisMicroDepot = parcelsByMicrodepot.get(microDepot);
                int numberOfBikeToursPerDay = 5;
                int numberOfCargoBikes = (int) (parcelsInThisMicroDepot.size() / cargoBikeType.getCarrierVehicleCapacity()/numberOfBikeToursPerDay);
                for (int i = 0; i < numberOfCargoBikes; i++){
                    CarrierVehicle vehicle = getSpecificVehicle(cargoBikeType, microDepotCarrier.getId(), i, microDepotLinkId,  8 * 60 * 60, 17 * 60 * 60);
                    microDepotCarrier.getCarrierCapabilities().getCarrierVehicles().
                            add(vehicle);
                }
                microDepotCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.FINITE);

                //deliver the parcels later with cargo bikes

                createDeliveriesByCargoBikes(parcelsInThisMicroDepot, microDepotCarrier);
            }


        }

    }

    private void createDeliveriesByMotorizedModes(List<Parcel> parcelsInThisDistributionCenter, Carrier carrier) {

        int parcelIndex = 0;
        int parcelsToMicroDepotIndex = 0;
        //Map<MicroDepot, Integer> parcelsToMicroDepots = new HashMap<>();

        for (Parcel parcel : parcelsInThisDistributionCenter) {
            if (properties.getRand().nextDouble() < properties.getSampleFactorForParcels()) {
                parcel.setAssigned(true);
                if (parcel.isToDestination() && parcel.getDestCoord() != null) {

                    if (parcel.getParcelDistributionType().equals(ParcelDistributionType.MOTORIZED)) {
                        Coord parcelCoord = new Coord(parcel.getDestCoord().x, parcel.getDestCoord().y);
                        TimeWindow timeWindow = generateRandomTimeSubWindow(7, 17, 1);
                        double duration_s = 2 * 60;
                        Id<Link> linkParcelDelivery = NetworkUtils.getNearestLink(network, parcelCoord).getId();
                        CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(parcel.getId(),
                                CarrierService.class), linkParcelDelivery);
                        serviceBuilder.setCapacityDemand(1);
                        serviceBuilder.setServiceDuration(duration_s);
                        serviceBuilder.setServiceStartTimeWindow(timeWindow);
                        carrier.getServices().add(serviceBuilder.build());
                        parcelIndex++;
                    } else {
                        MicroDepot microDepot = parcel.getMicroDepot();
//                        if (parcelsToMicroDepots.keySet().contains(microDepot)) {
//                            parcelsToMicroDepots.put(microDepot, parcelsToMicroDepots.get(microDepot) + 1);
//                        } else {
//                            parcelsToMicroDepots.put(microDepot, 1);
//                        }
                        parcelsByMicrodepot.get(microDepot).add(parcel);
                        parcelsToMicroDepotIndex++;
                    }


                }
            }
        }
        for (MicroDepot microDepot : parcelsByMicrodepot.keySet()){
            Coord destCoord = new Coord(microDepot.getCoord_gk4().x, microDepot.getCoord_gk4().y);
            TimeWindow timeWindow = generateRandomTimeSubWindow(7, 8, 1);
            int demandedCapacity = parcelsByMicrodepot.get(microDepot).size();
            double duration_s = Math.min(5 * demandedCapacity, 15*60*60);
            Id<Link> linkParcelDelivery = NetworkUtils.getNearestLink(network, destCoord).getId();
            CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create("to_micro_depot_" + microDepot.getId(), CarrierService.class), linkParcelDelivery);
            serviceBuilder.setCapacityDemand(demandedCapacity);
            serviceBuilder.setServiceDuration(duration_s);
            serviceBuilder.setServiceStartTimeWindow(timeWindow);
            carrier.getServices().add(serviceBuilder.build());
        }
        logger.info("Assigned " + parcelIndex + " parcels at this carrier");
        logger.info("Assigned " + parcelsToMicroDepotIndex + " parcels at this carrier via microDepot");


    }


    private void createDeliveriesByCargoBikes(List<Parcel> parcelsInThisMicroDepot, Carrier carrier) {

        int parcelIndex = 0;
        for (Parcel parcel : parcelsInThisMicroDepot) {
           //no need to scale again
           if (parcel.isToDestination() && parcel.getDestCoord() != null) {
                    Coord parcelCoord;
                    TimeWindow timeWindow;
                    parcelCoord = new Coord(parcel.getDestCoord().x, parcel.getDestCoord().y);
                    timeWindow = generateRandomTimeSubWindow(8, 17, 1);
                    double duration_s = 2 * 60;
                    Id<Link> linkParcelDelivery = NetworkUtils.getNearestLink(network, parcelCoord).getId();
                    CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create(parcel.getId(), CarrierService.class), linkParcelDelivery);
                    serviceBuilder.setCapacityDemand(1);
                    serviceBuilder.setServiceDuration(duration_s);
                    serviceBuilder.setServiceStartTimeWindow(timeWindow);
                    carrier.getServices().add(serviceBuilder.build());
                    parcelIndex++;
                }

        }
        logger.info("Assigned " + parcelIndex + " parcels at this micro-depot carrier");


    }


    private static CarrierVehicle getGenericVehicle(CarrierVehicleType type, Id<?> carrierId, Id<Link> homeId, double start_s, double end_s) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_" +
                carrierId.toString() +
                "_" + type.getId().toString()), Vehicle.class), homeId);
        vBuilder.setEarliestStart(start_s);
        vBuilder.setLatestEnd(end_s);
        vBuilder.setType(type);
        return vBuilder.build();
    }

    private static CarrierVehicle getSpecificVehicle(CarrierVehicleType type, Id<?> carrierId, int index, Id<Link> homeId, double start_s, double end_s) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_" +
                carrierId.toString() +
                "_" + type.getId().toString() + "_" + index), Vehicle.class), homeId);
        vBuilder.setEarliestStart(start_s);
        vBuilder.setLatestEnd(end_s);
        vBuilder.setType(type);
        return vBuilder.build();
    }

//    private static CarrierVehicle getLightVehicle(Id<?> id, Id<Link> homeId, String depot) {
//        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_" + id.toString() + "_lightVehicle_" + depot), Vehicle.class), homeId);
//        vBuilder.setEarliestStart(6 * 60 * 60);
//        vBuilder.setLatestEnd(16 * 60 * 60);
//        vBuilder.setType(createLightType());
//        return vBuilder.build();
//    }
//
//    private static CarrierVehicleType createLightType() {
//        CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("small", VehicleType.class));
//        typeBuilder.setCapacity(6);
//        typeBuilder.setFixCost(80.0);
//        typeBuilder.setCostPerDistanceUnit(0.00047);
//        typeBuilder.setCostPerTimeUnit(0.008);
//        return typeBuilder.build();
//    }
//
//    private static CarrierVehicle getHeavyVehicle(Id<?> id, Id<Link> homeId, String depot) {
//        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create("carrier_" + id.toString() + "_heavyVehicle_" + depot, Vehicle.class), homeId);
//        vBuilder.setEarliestStart(6 * 60 * 60);
//        vBuilder.setLatestEnd(16 * 60 * 60);
//        vBuilder.setType(createHeavyType());
//        return vBuilder.build();
//    }
//
//    private static CarrierVehicleType createHeavyType() {
//        CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class));
//        typeBuilder.setCapacity(25);
//        typeBuilder.setFixCost(130.0);
//        typeBuilder.setCostPerDistanceUnit(0.00077);
//        typeBuilder.setCostPerTimeUnit(0.008);
//        return typeBuilder.build();
//    }

    public TimeWindow generateRandomTimeSubWindow(double minTime_h, double maxTime_h, int slices){
        double interval_s = ( maxTime_h - minTime_h) * 3600 / slices;
        int i = 0;
        double randomNumber = properties.getRand().nextDouble();
        while ( randomNumber*slices  >  i ) {
            i++;
        }
        return TimeWindow.newInstance(3600 * minTime_h + (i - 1) * interval_s, 3600 * minTime_h + i* interval_s );
    }

}
