package se.hshn.de.pathtracker;

import java.util.Objects;

/**
 * Created by florens on 08.01.17.
 */

public class Store {


    private Long id;


    private String name;


    private String street;


    private String city;


    private String zip;


    private String country;


    private Float lat;


    private Float lon;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Store name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreet() {
        return street;
    }

    public Store street(String street) {
        this.street = street;
        return this;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public Store city(String city) {
        this.city = city;
        return this;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZip() {
        return zip;
    }

    public Store zip(String zip) {
        this.zip = zip;
        return this;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public Store country(String country) {
        this.country = country;
        return this;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Float getLat() {
        return lat;
    }

    public Store lat(Float lat) {
        this.lat = lat;
        return this;
    }

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLon() {
        return lon;
    }

    public Store lon(Float lon) {
        this.lon = lon;
        return this;
    }

    public void setLon(Float lon) {
        this.lon = lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Store store = (Store) o;
        if (store.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, store.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Store{" +
                "id=" + id +
                ", name='" + name + "'" +
                ", street='" + street + "'" +
                ", city='" + city + "'" +
                ", zip='" + zip + "'" +
                ", country='" + country + "'" +
                ", lat='" + lat + "'" +
                ", lon='" + lon + "'" +
                '}';
    }


}
