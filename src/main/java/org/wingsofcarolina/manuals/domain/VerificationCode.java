package org.wingsofcarolina.manuals.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.dao.VerificationCodeDAO;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity("Verification")
public class VerificationCode {
	private static final Logger LOG = LoggerFactory.getLogger(VerificationCode.class);

	private static VerificationCodeDAO dao = new VerificationCodeDAO();
	
	@Id
	@JsonIgnore
	private ObjectId id;
	private String uuid;
	private Integer code;
	private Date expire;

	public static String ID_KEY = "verification";

	static Random random = new Random();
	
	public VerificationCode() {}
	
	public static VerificationCode makeEntry(String uuid) {
		VerificationCode entry = VerificationCode.getByPersonUUID(uuid);
		if (entry == null) {
			entry = new VerificationCode(uuid);
		} else {
			entry.setExpire();
		}
		entry.save();
		return entry;
	}

	public VerificationCode(String uuid) {
		this.uuid = uuid;
		code = random.nextInt(999999 - 100000) + 100000;
		setExpire();
	}
	
	private void setExpire() {
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(new Date());
	    calendar.add(Calendar.HOUR_OF_DAY, 2);
	    this.expire = calendar.getTime();
	}

	public String getUUID() {
		return uuid;
	}
	
	public Integer getCode() {
		return code;
	}

	public Date getExpire() {
		return expire;
	}
	
	public static void cleanCache() {
		Date now = new Date();
		
		// Get the iterator over the HashMap
        Iterator<VerificationCode>iterator = VerificationCode.getAll().iterator();
  
        // Iterate over the HashMap
        while (iterator.hasNext()) {
            // Get the entry at this iteration
        	VerificationCode entry = iterator.next();
  
            // Check if this entry has expired
            Date expire = entry.getExpire();
            if (now.compareTo(expire) > 0) {
            	LOG.info("Removed unused {} / {} from verification cache.", entry.getUUID(), entry.getCode());
                entry.delete();
            }
        }
	}
	
	/*
	 * Database Management Functionality
	 */
	public static long count() {
		return dao.count();
	}
	
	public static void drop() {
		dao.drop();
	}
	
	@SuppressWarnings("unchecked")
	public static List<VerificationCode> getAll() {
		return (List<VerificationCode>) dao.getAll();
	}

	public static VerificationCode getByID(long id) {
		return (VerificationCode) dao.getByID(id);
	}
	
	public static VerificationCode getByPersonUUID(String uuid) {
		return dao.getByPersonUUID(uuid);
	}

	public static VerificationCode getByCode(Integer code) {
		return dao.getByCode(code);
	}

	public void save() {
		dao.save(this);
	}
	
	public void delete() {
		dao.delete(this);
	}
}
