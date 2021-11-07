package org.wingsofcarolina.manuals.resources;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Index  implements Comparable<Index> {
	private Integer lesson = 0;
	private String path;
	private String label;
	private boolean directory = false;
	private List<Index> children = null;
	
	public Index() {}
	
	public Index(String path, String label, Integer lesson) {
		this.path = path;
		this.label = label;
		this.lesson = lesson;
	}

	public Index(String path, String label) {
		this(path, label, 0);
	}
	
	public Integer getLesson() {
		return lesson;
	}

	public void setLesson(Integer lesson) {
		this.lesson = lesson;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<Index> getChildren() {
		return children;
	}

	public void setDirectory() {
		this.directory = true;
	}

	public boolean isDirectory() {
		return this.directory == true;
	}
	public void setDocument() {
		this.directory = false;
	}

	public void addChild(Index index) {
		if (children == null) {
			this.children = new ArrayList<Index>();
		}
		this.children.add(index);
	}

	public int compareTo(Index index) {
		if (getLesson() == null || index.getLesson() == null) {
			return 0;
		}
		return getLesson().compareTo(index.getLesson());
	}

	@Override
	public String toString() {
		return "Index [path=" + path + ", label=" + label + ", lesson=" + lesson + "]";
	}
}
