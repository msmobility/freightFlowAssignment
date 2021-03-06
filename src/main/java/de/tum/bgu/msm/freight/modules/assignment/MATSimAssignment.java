package de.tum.bgu.msm.freight.modules.assignment;

import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule;
import de.tum.bgu.msm.freight.data.DataSet;
import de.tum.bgu.msm.freight.modules.Module;
import de.tum.bgu.msm.freight.io.output.CountEventHandler;
import de.tum.bgu.msm.freight.io.input.LinksFileReader;
import de.tum.bgu.msm.freight.properties.Properties;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.Facility;

import java.io.IOException;
import java.util.List;

/**
 * Configures MATSim, creates a population and runs MATSim.
 */
public class MATSimAssignment implements Module {

    private Config config;
    private MutableScenario scenario;
    private Network network;
    private Controler controler;

    private Properties properties;
    private DataSet dataSet;
    private MATSimFreightManager matSimFreightManager;

    private MATSimTruckPlanGenerator matSimTruckPlanGenerator;

    public MATSimAssignment() {

    }

    @Override
    public void setup(DataSet dataSet, Properties properties) {
        this.properties = properties;
        this.dataSet = dataSet;

        config = MATSimConfigUtils.configure(ConfigUtils.createConfig(), properties);
        scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
        network = scenario.getNetwork();
        controler = new Controler(scenario);

        matSimFreightManager = new MATSimFreightManager(config, scenario, controler, properties);
        matSimTruckPlanGenerator = new MATSimTruckPlanGenerator();
        matSimTruckPlanGenerator.setup(dataSet, properties);

        if (properties.isRunParcelDelivery()) {
            //configure MATSim for freight extension
            matSimFreightManager.configureMATSimForFreight();
        }

    }

    @Override
    public void run() {
        loadPopulation();
        runMatsim();
    }

    private void loadPopulation() {

        if (!properties.getMatsimBackgroundTraffic().equals("")) {
            new PopulationReader(scenario).readFile(properties.getMatsimBackgroundTraffic());
        } else {
            scenario.setPopulation(PopulationUtils.createPopulation(config));
        }
        matSimTruckPlanGenerator.addTrucks(scenario.getPopulation(), scenario.getNetwork());
        if (properties.isRunParcelDelivery()) {
            generateCarriersPopulation(matSimFreightManager.getCarriers(), matSimFreightManager.getCarrierVehicleTypes());
        }
    }


    private void generateCarriersPopulation(Carriers carriers, CarrierVehicleTypes carrierVehicleTypes) {
        //create carriers
        new CarriersAndServicesGenerator(dataSet, network, properties).generateCarriers(carriers, carrierVehicleTypes);
        //assign plans to carriers
        new CarrierTourDesigner(scenario.getNetwork(), dataSet).generateCarriersPlan(carriers);

    }


    private void runMatsim() {
        CountEventHandler countEventHandler = new CountEventHandler(properties);
        if (properties.isReadEventsForCounts()) {
            LinksFileReader linksFileReader = new LinksFileReader(dataSet, properties.getCountStationLinkListFile());
            linksFileReader.read();

            for (Id linkId : dataSet.getObservedCounts().keySet()) {
                countEventHandler.addLinkById(linkId);
            }
            controler.getEvents().addHandler(countEventHandler);
        }
        controler.run();
        if (properties.isReadEventsForCounts()) {
            try {
                countEventHandler.printOutCounts("./output/" + properties.getRunId() + "/" + properties.getCountsFileName(), properties.getNetworkFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
