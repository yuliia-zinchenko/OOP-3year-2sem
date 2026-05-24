package com.library.dao;

import com.library.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * GoF: Template Method / DAO pattern.
 * Subclasses provide SQL + ResultSet -> entity mapping.
 */
public abstract class BaseDao<T, ID> {

    protected Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public abstract Optional<T> findById(ID id);
    public abstract List<T> findAll();
    public abstract T save(T entity);
    public abstract boolean deleteById(ID id);
}
