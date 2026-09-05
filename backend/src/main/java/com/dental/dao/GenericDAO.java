package com.dental.dao;

import java.util.List;
import java.util.Optional;

/**
 * <b>DAO pattern.</b> Contract every data-access object honours, so the business
 * tier can talk to storage without knowing that MySQL/JDBC is behind it.
 *
 * @param <T> the DTO this DAO reads and writes
 */
public interface GenericDAO<T> {

    List<T> findAll();

    Optional<T> findById(int id);

    /** Inserts and returns the generated primary key. */
    int insert(T entity);

    boolean update(T entity);

    boolean delete(int id);
}
