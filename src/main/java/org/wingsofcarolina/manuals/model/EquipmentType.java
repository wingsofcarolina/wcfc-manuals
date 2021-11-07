package org.wingsofcarolina.manuals.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum EquipmentType {
    INTERCOM("Intercoms"),
    AUDIOPNL("Audio Panels"),
    NAVCOM("Nav/Coms"),
    TRANSPONDER("Transponders"),
    GPS("GPSs"),
    AUTOPILOT("AutoPilots"),
    TOTALIZER("Fuel Totalizers"),
    ENGMONITOR("Engine Monitors"),
    DME("DMEs"),
    OTHER("Other");
	
	private String label;
    
    private static final Map<String,EquipmentType> lookup = new HashMap<String,EquipmentType>();

	static {
	    for(EquipmentType t : EnumSet.allOf(EquipmentType.class)) {
	         lookup.put(t.getLabel(), t);
	    }
	}
	
	private EquipmentType(String label) {
	    this.label = label;
	}
	
	public String getLabel() { return label; }
	
	public static EquipmentType get(String label) { 
	    return lookup.get(label); 
	}

	public static List<Map> getTypes() {
		List<Map> response = new ArrayList<Map>();
		 for(EquipmentType t : EnumSet.allOf(EquipmentType.class)) {
			 Map<String, Object> entry = new HashMap<String, Object>();
			 entry.put("mtype", t);
			 entry.put("label", t.getLabel());
			 response.add(entry);
	    }
		return response;
	}
}