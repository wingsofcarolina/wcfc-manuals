package org.wingsofcarolina.manuals.model;

import java.util.HashMap;
import java.util.UUID;

public class Equipment {
	private String name;
	private EquipmentType type;
	private Boolean hasDocument = false;
	private String uuid = UUID.randomUUID().toString();
	
	public Equipment() {}
	
	public Equipment(String name, EquipmentType type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}

	public Boolean getHasDocument() {
		return hasDocument;
	}

	public void setHasDocument(Boolean hasDocument) {
		this.hasDocument = hasDocument;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EquipmentType getType() {
		return type;
	}

	public void setType(EquipmentType type) {
		this.type = type;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String equipmentId) {
		this.uuid = equipmentId;
	}
}
