package de.tum.bgu.msm.freight.modules.assignment;

import de.tum.bgu.msm.freight.data.*;
import de.tum.bgu.msm.freight.properties.Properties;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowsToVehicleAssignment {

    private static Logger logger = Logger.getLogger(FlowsToVehicleAssignment.class);

    private FreightFlowsDataSet dataSet;
    private UncongestedTravelTime uncongestedTravelTime;
    private Properties properties;

    private CoordinateTransformation ct;
    private DepartureTimeDistribution departureTimeDistribution;

    private ArrayList<StoredFlow> flowsByTruck = new ArrayList<>();

    public FlowsToVehicleAssignment(FreightFlowsDataSet dataSet, Properties properties) {

        ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
        departureTimeDistribution = new NormalDepartureTimeDistribution();

        this.dataSet = dataSet;
        this.properties = properties;
        if (properties.isStoreExpectedTimes()) {
            uncongestedTravelTime = new UncongestedTravelTime(properties.getSimpleNetworkFile());
            uncongestedTravelTime.calculateTravelTimeMatrix(ct, dataSet);
        }
    }

    public Population disaggregateToVehicles(Config config) {
        Population population = PopulationUtils.createPopulation(config);
        PopulationFactory factory = population.getFactory();

        AtomicInteger counter = new AtomicInteger(0);



        Set<Integer> destinations = new HashSet<>();
        for (int destId : properties.getSelectedDestinations()) {
            if (destId == -1) {
                destinations = dataSet.getFlowMatrix().columnKeySet();
                break;
            } else {
                destinations.add(destId);
            }
        }

        for (int origin : dataSet.getFlowMatrix().rowKeySet()) {
            for (int destination : destinations) {
                if (dataSet.getFlowMatrix().contains(origin, destination)) {
                    if (dataSet.getZones().containsKey(origin) &&
                            dataSet.getZones().containsKey(destination)) {
                        ArrayList<OrigDestFlow> flowsThisOrigDest = dataSet.getFlowMatrix().get(origin, destination);
                        for (OrigDestFlow origDestFlow : flowsThisOrigDest) {
                            for (Trip trip : origDestFlow.getTrips().values()) {
                                if (trip.getMode().equals(Mode.ROAD)) {
                                    int tripOrigin = trip.getOrigin();
                                    int tripDestination = trip.getDestination();
                                    Zone originZone = dataSet.getZones().get(tripOrigin);
                                    Zone destinationZone = dataSet.getZones().get(tripDestination);

                                    Coord origCoord;
                                    Coord destCoord;
                                    try {
                                        origCoord = originZone.getRandomCoord();
                                    } catch (NullPointerException e) {
                                        origCoord = null;
                                    }

                                    try {
                                        destCoord = destinationZone.getRandomCoord();
                                    } catch (NullPointerException e) {
                                        destCoord = null;
                                    }

                                    if (origCoord != null && destCoord != null) {

                                        origCoord = ct.transform(origCoord);
                                        destCoord = ct.transform(destCoord);

                                        double beelineDistance_km = NetworkUtils.getEuclideanDistance(origCoord, destCoord) / 1000;
                                        DistanceBin distanceBin = DistanceBin.getDistanceBin(beelineDistance_km);
                                        double truckLoad = dataSet.getTruckLoadsByDistanceAndCommodity().get(trip.getCommodity(), distanceBin);
                                        double proportionEmpty = dataSet.getEmptyTrucksProportionsByDistanceAndCommodity().get(trip.getCommodity(), distanceBin);

                                        double numberOfVehicles_double = trip.getVolume_tn() / properties.getDaysPerYear() / truckLoad;
                                        numberOfVehicles_double = numberOfVehicles_double / (1 - proportionEmpty);
                                        int numberOfVehicles_int = (int) Math.floor(numberOfVehicles_double);

                                        if (properties.getRand().nextDouble() < (numberOfVehicles_double - numberOfVehicles_int)) {
                                            numberOfVehicles_int++;
                                        }

                                        flowsByTruck.add(new StoredFlow(trip.getOrigin(), trip.getDestination(), trip.getCommodity(),
                                                beelineDistance_km, trip.getVolume_tn(), numberOfVehicles_int,
                                                dataSet.getUncongestedTravelTime(trip.getOrigin(), trip.getDestination())));

                                        for (int vehicle = 0; vehicle < numberOfVehicles_int; vehicle++) {

                                            if (properties.getRand().nextDouble() < properties.getScaleFactor()) {
                                                String idOfVehicle = tripOrigin + "-" +
                                                        tripDestination + "-" +
                                                        trip.getCommodity().getCommodityGroup() + "-" +
                                                        vehicle + "-" +
                                                        counter;

                                                if (!trip.getSegment().equals(Segment.MAIN)) {
                                                    idOfVehicle += "-" + trip.getSegment().toString();
                                                }

                                                if (properties.getRand().nextDouble() < proportionEmpty){
                                                    idOfVehicle += "-IS_EMPTY";
                                                }

                                                if (trip.getFlowType().equals(FlowType.CONTAINER_RO_RO)) {
                                                    idOfVehicle += "-" + trip.getFlowType().toString();
                                                }

                                                Person person = factory.createPerson(Id.createPersonId(idOfVehicle));
                                                Plan plan = factory.createPlan();
                                                person.addPlan(plan);
                                                population.addPerson(person);

                                                Activity originActivity = factory.createActivityFromCoord("start", origCoord);
                                                originActivity.setEndTime(departureTimeDistribution.getDepartureTime(0)*60);
                                                plan.addActivity(originActivity);

                                                plan.addLeg(factory.createLeg(TransportMode.truck));

                                                Activity destinationActivity = factory.createActivityFromCoord("end", destCoord);
                                                plan.addActivity(destinationActivity);
                                                counter.incrementAndGet();

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return population;
    }


    public void printOutResults() throws IOException {

        File file = new File("./output/" + properties.getRunId());
        file.mkdirs();

        PrintWriter pw = new PrintWriter(new FileWriter("./output/" + properties.getRunId() + "/truckFlows.csv"));

        pw.println("origin,destination,commodity,distanceBin,volume_tn,trucks,tt");

        for (StoredFlow storedFlow : flowsByTruck) {
            pw.println(storedFlow.origin + "," +
                    storedFlow.destination + "," +
                    storedFlow.commodity + "," +
                    storedFlow.distance_km + "," +
                    storedFlow.volume_tn / properties.getDaysPerYear() + "," +
                    storedFlow.numberOfTrucks + "," +
                    storedFlow.tt_s);
        }

        pw.close();
    }


    private class StoredFlow {
        private int origin;
        private int destination;
        private Commodity commodity;
        private double distance_km;
        private double volume_tn;
        private int numberOfTrucks;
        private double tt_s;

        public StoredFlow(int origin, int destination, Commodity commodity, double distance_km, double volume_tn, int numberOfTrucks, double tt_s) {
            this.origin = origin;
            this.destination = destination;
            this.commodity = commodity;
            this.distance_km = distance_km;
            this.volume_tn = volume_tn;
            this.numberOfTrucks = numberOfTrucks;
            this.tt_s = tt_s;
        }


    }
}
