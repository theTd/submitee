package org.starrel.submitee.attribute;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.starrel.submitee.model.NotExistsSignal;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class JdbcAttributeSource implements AttributeSource {
    private final DataSource dataSource;
    private final String table;
    private final String whereClause;

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    public JdbcAttributeSource(DataSource dataSource, String table, String whereClause) {
        this.dataSource = dataSource;
        this.table = table;
        this.whereClause = whereClause.toLowerCase(Locale.ROOT).startsWith("where") ? whereClause : "WHERE " + whereClause;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TValue> TValue getAttribute(String path, Class<TValue> type) {
        try {
            return (TValue) cache.get(path, () -> {
                try (Connection conn = dataSource.getConnection()) {
                    ResultSet r = conn.createStatement().executeQuery(String.format("SELECT %s FROM %s %s", getColumnName(path), table, whereClause));
                    if (!r.next()) throw NotExistsSignal.INSTANCE;
                    Object obj = r.getObject(1);
                    if (obj == null) throw NotExistsSignal.INSTANCE;
                    return obj;
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof NotExistsSignal) return null;
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void setAttribute(String path, Object value) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(String.format("UPDATE %s SET %s=? %s", table, getColumnName(path), whereClause));
            stmt.setObject(1, value);
            stmt.executeUpdate();

            cache.put(path, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> listKeys(String path) {
        return null;
    }

    @Override
    public void delete(String path) {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().executeUpdate(String.format("UPDATE %s SET %s=NULL %s", table, getColumnName(path), whereClause));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <TValue> TValue getListAttribute(String path, int index, Class<TValue> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setListAttribute(String path, int index, Object tValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListAttribute(String path, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <TValue> List<TValue> getListAttributes(String path, Class<TValue> type) {
        throw new UnsupportedOperationException();
    }

    public String getColumnName(String path) {
        int idx = path.lastIndexOf(".");
        if (idx == -1) return path;
        return path.substring(idx + 1);
    }
}
