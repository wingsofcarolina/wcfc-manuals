package org.wingsofcarolina.manuals.domain.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.persistence.Persistence;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;

public class SuperDAO {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(SuperDAO.class);

	private Class<?> clazz;
	private Datastore ds;
	
	public SuperDAO(Class<?> clazz) {
		this.clazz = clazz;
		ds = Persistence.instance().datastore();
	}
	
	public long count() {
		return ds.find(clazz).count();
	}
	
	public List<?> getAll() {
		Query<?> query = ds.find(clazz);
		List<?> result =  query.iterator().toList();
		return result;
	}
	

	public Object getByID(long id) {
		Datastore ds = Persistence.instance().datastore();
		Query<?> query = ds.find(clazz);
		List<?> users = query.filter(Filters.eq("userId", id)).iterator().toList();
		if (users.size() > 0) {
			return users.get(0);
		} else {
			return null;
		}
	}
	
	public void drop() {
		try {
			String name = collectionName(clazz);
			ds.getDatabase().getCollection(name).drop();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	      
	public void save(Object user) {
		ds.save(user);
	}
	
	public void delete(Object user) {
		ds.delete(user);
	}
	
	private String collectionName(Class<?> clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object value = null;
		for (Annotation annotation : clazz.getAnnotations()) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (type.getName().contains("dev.morphia.annotations.Entity")) {
				for (Method method : type.getDeclaredMethods()) {
					if (method.getName().equals("value")) {
						value = method.invoke(annotation, (Object[]) null);
					}
				}
			}
		}
		return (String) value;
	}
}
