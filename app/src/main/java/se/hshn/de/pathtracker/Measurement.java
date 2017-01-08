package se.hshn.de.pathtracker;

/**
 * Created by florens on 08.01.17.
 */

public class Measurement {

    private Long timestamp;
    private Double lat;
    private Double lon;
    private Float accuracy;
    private Float direction;
    private Float length;
    private Float magx;
    private Float magy;
    private Float magz;

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public Float getDirection() {
        return direction;
    }

    public void setDirection(Float direction) {
        this.direction = direction;
    }

    public Float getLength() {
        return length;
    }

    public void setLength(Float length) {
        this.length = length;
    }

    public Float getMagx() {
        return magx;
    }

    public void setMagx(Float magx) {
        this.magx = magx;
    }

    public Float getMagy() {
        return magy;
    }

    public void setMagy(Float magy) {
        this.magy = magy;
    }

    public Float getMagz() {
        return magz;
    }

    public void setMagz(Float magz) {
        this.magz = magz;
    }
}
