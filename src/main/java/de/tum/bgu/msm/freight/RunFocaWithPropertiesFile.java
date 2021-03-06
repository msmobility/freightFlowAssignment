package de.tum.bgu.msm.freight;

import de.tum.bgu.msm.freight.properties.Properties;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;


/**
 * Created by Kamil Moreau on 12/10/2019.
 */
@Deprecated
public class RunFocaWithPropertiesFile {

    private static final Logger logger = Logger.getLogger(RunFocaWithPropertiesFile.class);


    public static void main(String[] args) throws FileNotFoundException {

        Properties properties = new Properties(Properties.initializeResourceBundleFromFile(args[0]));
        properties.logProperties("./output/" + properties.getRunId());
        logger.info(properties.flows().getMatrixFileNamePrefix());

        logger.info(properties.getRunId());
        logger.info(properties.getYear());
        logger.info(properties.flows().getMatrixFolder());

        RunFocaWithModeChoice freightFlows = new RunFocaWithModeChoice();
        logger.info("Start simulation " + properties.getRunId());
        freightFlows.run(properties);
        logger.info("End simulation " + properties.getRunId());

    }



}
