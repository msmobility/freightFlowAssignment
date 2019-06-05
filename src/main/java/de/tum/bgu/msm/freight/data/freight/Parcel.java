package de.tum.bgu.msm.freight.data.freight;

import de.tum.bgu.msm.freight.data.geo.DistributionCenter;
import de.tum.bgu.msm.freight.data.geo.MicroDepot;
import de.tum.bgu.msm.freight.data.geo.ParcelShop;
import org.matsim.api.core.v01.Coord;

public class Parcel {

    private final int id;
    private Transaction transaction;
    private final boolean toDestination;
    private final double volume_m3;
    private final double weight_kg;
    private final Commodity commodity;
    private final DistributionCenter distributionCenter;

    private ParcelDistributionType parcelDistributionType;
    private MicroDepot microDepot = null;
    private ParcelShop parcelShop = null;
    private Coord originCoord;
    private Coord destCoord;
    private int origMicroZoneId;
    private int destMicroZoneId;

    public Parcel(int id, boolean toDestination, double volume_m3, double weight_kg,
                  DistributionCenter distributionCenter, Commodity commodity) {
        this.id = id;
        this.toDestination = toDestination;
        this.volume_m3 = volume_m3;
        this.weight_kg = weight_kg;
        this.distributionCenter = distributionCenter;
        this.commodity = commodity;
    }

    public static String getHeader() {

        StringBuilder builder = new StringBuilder();

        builder.append("id").append(",").
                append("toDestination").append(",").
                append("weight_kg").append(",").
                append("commodity").append(",").
                append("originX").append(",").
                append("originY").append(",").
                append("destX").append(",").
                append("destY").append(",").
                append("origMicroZone").append(",").
                append("destMicroZone").append(",").
                append("distributionCenter").append(",").
                append("transaction").append(",").
                append("microDepot").append(",").
                append("parcelShop");


        return builder.toString();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(id).append(",").
                append(toDestination).append(",").
                append(weight_kg).append(",").
                append(commodity).append(",");

        if (this.getTransaction().equals(Transaction.BUSINESS_CUSTOMER) || this.getTransaction().equals(Transaction.PRIVATE_CUSTOMER)) {
            builder.append(originCoord.getX()).append(",").
                    append(originCoord.getY()).append(",").
                    append(destCoord.getX()).append(",").
                    append(destCoord.getY()).append(",").
                    append(origMicroZoneId).append(",").
                    append(destMicroZoneId).append(",");
        } else {
            builder.append("null").append(",").
                    append("null").append(",").
                    append("null").append(",").
                    append("null").append(",").
                    append("null").append(",").
                    append("null").append(",");
        }


        builder.append(distributionCenter.getId()).append(",").
                append(transaction.toString()).append(",").
                append(microDepot).append(",").
                append(parcelShop);

        return builder.toString();

    }

    public int getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public boolean isToDestination() {
        return toDestination;
    }

    public double getVolume_m3() {
        return volume_m3;
    }

    public double getWeight_kg() {
        return weight_kg;
    }

    public DistributionCenter getDistributionCenter() {
        return distributionCenter;
    }

    public Coord getOriginCoord() {
        return originCoord;
    }

    public void setOriginCoord(Coord originCoord) {
        this.originCoord = originCoord;
    }

    public Coord getDestCoord() {
        return destCoord;
    }

    public void setDestCoord(Coord destCoord) {
        this.destCoord = destCoord;
    }

    public Commodity getCommodity() {
        return commodity;
    }

    public void setOrigMicroZone(int microZone) {
        this.origMicroZoneId = microZone;
    }

    public void setDestMicroZone(int microZone) {
        this.destMicroZoneId = microZone;
    }

    public int getDestMicroZoneId() {
        return destMicroZoneId;
    }

    public int getOrigMicroZoneId(){
        return origMicroZoneId;
    }

    public ParcelDistributionType getParcelDistributionType() {
        return parcelDistributionType;
    }

    public void setParcelDistributionType(ParcelDistributionType parcelDistributionType) {
        this.parcelDistributionType = parcelDistributionType;
    }

    public MicroDepot getMicroDepot() {
        return microDepot;
    }

    public void setMicroDepot(MicroDepot microDepot) {
        this.microDepot = microDepot;
    }

    public ParcelShop getParcelShop() {
        return parcelShop;
    }

    public void setParcelShop(ParcelShop parcelShop) {
        this.parcelShop = parcelShop;
    }
}
