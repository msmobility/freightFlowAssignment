package de.tum.bgu.msm.freight.properties;

public class SDProperties extends PropertiesGroup {

    private double shareOfCargoBikesAtZonesServedByMicroDepot = 1.;
    private int[] selectedDistributionCenters = new int[]{-1};
    private int maxNumberOfParcelsByCarrier = 500;

    public int[] getSelectedDistributionCenters() {
        return selectedDistributionCenters;
    }

    public void setSelectedDistributionCenters(int[] selectedDistributionCenters) {
        this.selectedDistributionCenters = selectedDistributionCenters;
    }

    public double getShareOfCargoBikesAtZonesServedByMicroDepot() {
        return shareOfCargoBikesAtZonesServedByMicroDepot;
    }

    public void setShareOfCargoBikesAtZonesServedByMicroDepot(double shareOfCargoBikesAtZonesServedByMicroDepot) {
        this.shareOfCargoBikesAtZonesServedByMicroDepot = shareOfCargoBikesAtZonesServedByMicroDepot;
    }

    public int getMaxParcelsByCarrier(){
        return maxNumberOfParcelsByCarrier;
    }
}
