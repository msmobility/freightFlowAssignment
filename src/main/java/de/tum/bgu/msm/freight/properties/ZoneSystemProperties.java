package de.tum.bgu.msm.freight.properties;

public class ZoneSystemProperties extends PropertiesGroup {

    private String zoneInputFile = "./input/zones_edit.csv";
    private String zoneShapeFile = "./input/shp/de_lkr_4326.shp";
    private String munichMicroZonesShapeFile = "input/shp/zones_4326_jobs.shp";
    private String regensburgMicroZonesShapeFile = "input/shp/zones_regensburg_4326.shp";
    private String idFieldInZonesShp = "RS";
    private String idFieldInMicroZonesShp = "id";

    public String getZoneInputFile() {
        return zoneInputFile;
    }
    public String getZoneShapeFile() {
        return zoneShapeFile;
    }

    public String getMunichMicroZonesShapeFile() {
        return munichMicroZonesShapeFile;
    }

    public String getRegensburgMicroZonesShapeFile() {
        return regensburgMicroZonesShapeFile;
    }

    public String getIdFieldInZonesShp() {
        return idFieldInZonesShp;
    }

    public void setZoneInputFile(String zoneInputFile) {
        this.zoneInputFile = zoneInputFile;
    }

    public void setZoneShapeFile(String zoneShapeFile) {
        this.zoneShapeFile = zoneShapeFile;
    }

    public void setMunichMicroZonesShapeFile(String munichMicroZonesShapeFile) {
        this.munichMicroZonesShapeFile = munichMicroZonesShapeFile;
    }

    public void setRegensburgMicroZonesShapeFile(String regensburgMicroZonesShapeFile) {
        this.regensburgMicroZonesShapeFile = regensburgMicroZonesShapeFile;
    }

    public void setIdFieldInZonesShp(String idFieldInZonesShp) {
        this.idFieldInZonesShp = idFieldInZonesShp;
    }

    public String getIdFieldInMicroZonesShp() {
        return idFieldInMicroZonesShp;
    }

    public void setIdFieldInMicroZonesShp(String idFieldInMicroZonesShp) {
        this.idFieldInMicroZonesShp = idFieldInMicroZonesShp;
    }



}
