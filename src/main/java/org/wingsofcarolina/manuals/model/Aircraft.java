package org.wingsofcarolina.manuals.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Aircraft {

  private String registration;
  private AircraftType type;
  private String label;
  private Boolean hasDocument = false;
  private List<String> equipment;
  private String uuid = UUID.randomUUID().toString();

  public Aircraft() {}

  public Aircraft(String registration, AircraftType atype) {
    this.registration = registration;
    this.type = atype;
  }

  public Boolean getHasDocument() {
    return hasDocument;
  }

  public void setHasDocument(Boolean hasDocument) {
    this.hasDocument = hasDocument;
  }

  public String getRegistration() {
    return registration;
  }

  public void setRegistration(String registration) {
    this.registration = registration;
  }

  public AircraftType getType() {
    return type;
  }

  public void setType(AircraftType atype) {
    this.type = atype;
  }

  public String getLabel() {
    return type.getLabel();
  }

  public List<String> getEquipment() {
    return equipment;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String equipmentId) {
    this.uuid = equipmentId;
  }

  public void addEquipment(Equipment equipment) {
    if (this.equipment == null) {
      this.equipment = new ArrayList<String>();
    }
    this.equipment.add(equipment.getUuid());
  }

  public void removeEquipment(Equipment target) {
    String id = target.getUuid();
    if (this.equipment != null) {
      Iterator<String> it = this.equipment.iterator();
      while (it.hasNext()) {
        String equipmentId = it.next();
        if (equipmentId.equals(id)) {
          it.remove();
        }
      }
    }
  }

  @Override
  public String toString() {
    return (
      "Aircraft [registration=" +
      registration +
      ", atype=" +
      type +
      ", getLabel()=" +
      getLabel() +
      "]"
    );
  }
}
