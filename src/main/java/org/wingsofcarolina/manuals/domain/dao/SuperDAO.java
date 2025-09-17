package org.wingsofcarolina.manuals.domain.dao;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.persistence.Persistence;

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
    List<?> result = query.iterator().toList();
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
    } catch (
      IllegalAccessException | IllegalArgumentException | InvocationTargetException e
    ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void save(Object user) {
    try {
      ds.save(user);
    } catch (Exception e) {
      // Log the error and try to ensure collection exists, then retry
      LOG.warn(
        "Error saving object, attempting to ensure collection exists: {}",
        e.getMessage()
      );
      try {
        // Try to ensure the collection exists and retry the save
        ensureCollectionExistsForClass(user.getClass());
        ds.save(user);
      } catch (Exception retryException) {
        LOG.error(
          "Failed to save object even after ensuring collection exists",
          retryException
        );
        throw retryException;
      }
    }
  }

  public void delete(Object user) {
    ds.delete(user);
  }

  private String collectionName(Class<?> clazz)
    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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

  /**
   * Ensure the collection exists for the given class, creating it if it doesn't
   */
  private void ensureCollectionExistsForClass(Class<?> clazz) {
    try {
      String collectionName = collectionName(clazz);
      if (collectionName != null) {
        ensureCollectionExists(collectionName);
      }
    } catch (Exception e) {
      LOG.debug(
        "Could not determine collection name for class {}: {}",
        clazz.getSimpleName(),
        e.getMessage()
      );
    }
  }

  /**
   * Ensure the collection exists, creating it if it doesn't
   */
  private void ensureCollectionExists(String collectionName) {
    try {
      // Check if collection exists by listing collections
      boolean collectionExists = false;
      for (String name : ds.getDatabase().listCollectionNames()) {
        if (name.equals(collectionName)) {
          collectionExists = true;
          break;
        }
      }

      // Create collection if it doesn't exist
      if (!collectionExists) {
        LOG.info("Creating MongoDB collection: {}", collectionName);
        ds.getDatabase().createCollection(collectionName);
      }
    } catch (Exception e) {
      // Log but don't fail - the collection might exist or be created automatically
      LOG.debug(
        "Could not ensure collection exists (this is usually fine): {}",
        e.getMessage()
      );
    }
  }
}
