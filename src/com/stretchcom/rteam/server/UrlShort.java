package com.stretchcom.rteam.server;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.google.appengine.api.datastore.Key;

@Entity
@NamedQueries({
    @NamedQuery(
    		name="UrlShort.getAll",
    		query="SELECT us FROM UrlShort us"
    ),
})
public class UrlShort {
	//private static final Logger log = Logger.getLogger(Activity.class.getName());
	private static RskyboxClient log = new RskyboxClient();
	
	private String uniqueId;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Key key;

	private Key getKey() {
        return key;
    }

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	// Reserves and returns a unique ID.
	// Transaction used so getting and saving incremented value in data store is an atomic action.
	public static String reserveUniqueId() {
    	EntityManager em = EMF.get().createEntityManager();
    	em.getTransaction().begin();
		String reservedId = null;
    	try {
    		UrlShort urlShort = null;
			List<UrlShort> urlShorts = (List<UrlShort>)em.createNamedQuery("UrlShort.getAll").getResultList();
			if(urlShorts.size() == 0) {
				// handle startup of this feature. We log an error because the error will only happen once
				urlShort = new UrlShort();
				urlShort.setUniqueId("0");
				log.error("UrlShort:reserveUniqueId:Exception", "no URL short entities -- if this is startup of feature just ignore");
			} else  {
				urlShort = urlShorts.get(0);
				if(urlShorts.size() > 1) {
					log.error("UrlShort:reserveUniqueId:Exception", "muliple URL short entities detected, but processing proceeded ...");
				}
			}
			reservedId = urlShort.incrementUniqueId();
			em.persist(urlShort);
			em.getTransaction().commit();
    	} catch (Exception e) {
			log.exception("UrlShort:reserveUniqueId:Exception", "", e);
		} finally {
		    if (em.getTransaction().isActive()) {
		        em.getTransaction().rollback();
		    }
		    em.close();
		}
    	//log.debug("new UrlShort unique ID = " + reservedId);
    	return reservedId;
	}
	
	// increments the uniqueId and updates this urlShort object with the new value
	private String incrementUniqueId() {
		int idAsDecimal = UrlShort.fromBase62ToDecimal(this.getUniqueId());
		idAsDecimal++;
		String idAsString = UrlShort.fromDecimalToBase62(idAsDecimal);
		this.setUniqueId(idAsString);
		return idAsString;
	}
	
    private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";  
  
    private static String fromDecimalToBase62(int decimalNumber) {
    	int base = 62;
        String tempVal = decimalNumber == 0 ? "0" : "";  
        int mod = 0;  
        while( decimalNumber != 0 ) {  
            mod = decimalNumber % base;  
            tempVal = baseDigits.substring( mod, mod + 1 ) + tempVal;  
            decimalNumber = decimalNumber / base;  
        }  
        return tempVal;  
    }  
  
    private static int fromBase62ToDecimal(String number) {
    	int base = 62;
        int iterator = number.length();  
        int returnValue = 0;  
        int multiplier = 1;  
        while( iterator > 0 ) {  
            returnValue = returnValue + ( baseDigits.indexOf( number.substring( iterator - 1, iterator ) ) * multiplier );  
            multiplier = multiplier * base;  
            --iterator;  
        }  
        return returnValue;  
    }  	
}
