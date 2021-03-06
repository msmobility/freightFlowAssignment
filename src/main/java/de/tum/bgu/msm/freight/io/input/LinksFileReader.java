package de.tum.bgu.msm.freight.io.input;

import de.tum.bgu.msm.freight.data.DataSet;
import de.tum.bgu.msm.freight.io.CSVReader;
import de.tum.bgu.msm.util.MitoUtil;
import org.matsim.api.core.v01.Id;

public class LinksFileReader extends CSVReader {

    private String fileName;
    private int idIndex;

    public LinksFileReader(DataSet dataSet, String fileName) {
        super(dataSet);
        this.fileName = fileName;
    }

    @Override
    protected void processHeader(String[] header) {

        idIndex = MitoUtil.findPositionInArray("ID", header);

    }

    @Override
    protected void processRecord(String[] record) {
        String linkId = record[idIndex];
        dataSet.getObservedCounts().put(Id.createLinkId(linkId), 0);
    }

    @Override
    public void read() {
        super.read(fileName, ",");
    }

}
