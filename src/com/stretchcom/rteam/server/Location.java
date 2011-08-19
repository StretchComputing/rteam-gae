package com.stretchcom.rteam.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

@Entity
public class Location {
	//constants
	public static final String EVENT_TYPE = "event"; // game or a practice event
	public static final String TEAM_TYPE = "team";

	private Double latitude;
	private Double longitude;
	private String locationName;
	private List<String> geocells;
	private Boolean isConfirmed;
	private String type;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	public Location() {
    	
    }

    public Key getKey() {
        return key;
    }
	
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}


	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	public static Boolean isTypeValid(String theLocationType) {
		if(theLocationType.equalsIgnoreCase(EVENT_TYPE)  || 
				theLocationType.equalsIgnoreCase(TEAM_TYPE)     ) {
			return true;
		}
		return false;
	}

	public List<String> getGeocells() {
		return geocells;
	}

	public void setGeocells(List<String> geocells) {
		this.geocells = geocells;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getIsConfirmed() {
		return isConfirmed;
	}

	public void setIsConfirmed(Boolean isConfirmed) {
		this.isConfirmed = isConfirmed;
	}

}
