package org.wingsofcarolina.manuals.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum AircraftType {
  C152("Cessna 152"),
  PA28("Warrior"),
  C172("Cessna 172"),
  M20J("Mooney M20J");

  private String label;

  private static final Map<String, AircraftType> lookup = new HashMap<String, AircraftType>();

  static {
    for (AircraftType t : EnumSet.allOf(AircraftType.class)) {
      lookup.put(t.getLabel(), t);
    }
  }

  private AircraftType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static AircraftType get(String label) {
    return lookup.get(label);
  }

  public static List<Map> getTypes() {
    List<Map> response = new ArrayList<Map>();
    for (AircraftType t : EnumSet.allOf(AircraftType.class)) {
      Map<String, Object> entry = new HashMap<String, Object>();
      entry.put("atype", t);
      entry.put("label", t.getLabel());
      response.add(entry);
    }
    return response;
  }
}
