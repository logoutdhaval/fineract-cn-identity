/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.identity.internal.repository;

import org.apache.fineract.cn.identity.api.v1.domain.DatabaseConnectionInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public class IsisPermittableDAO {

    private static final class Builder {

        private final ResultSet resultSet;

        private Builder(final ResultSet resultSet) {
            super();
            this.resultSet = resultSet;
        }

        Optional<IsisPermittableDAO> build() throws SQLException {
            if (this.resultSet.next()) {
                final IsisPermittableDAO isisPermittableDAO = new IsisPermittableDAO();
                isisPermittableDAO.setIdentifier(this.resultSet.getString("identifier"));
                isisPermittableDAO.setPermittables(this.resultSet.getString("permittables"));
//                isisPermittableDAO.setDatabaseName(this.resultSet.getString("database_name"));
//                isisPermittableDAO.setHost(this.resultSet.getString("host"));
//                isisPermittableDAO.setPort(this.resultSet.getString("port"));
//                isisPermittableDAO.setUser(this.resultSet.getString("a_user"));
//                isisPermittableDAO.setPassword(this.resultSet.getString("pwd"));
                return Optional.of(isisPermittableDAO);
            } else {
                return Optional.empty();
            }
        }

        List<IsisPermittableDAO> collect() throws SQLException {
            final ArrayList<IsisPermittableDAO> isisPermittableDAOS = new ArrayList<>();
            while (this.resultSet.next()) {
                final IsisPermittableDAO isisPermittableDAO = new IsisPermittableDAO();
                isisPermittableDAOS.add(isisPermittableDAO);
                isisPermittableDAO.setIdentifier(this.resultSet.getString("identifier"));
                isisPermittableDAO.setPermittables(this.resultSet.getString("permittables"));
            }
            return isisPermittableDAOS;
        }
    }

    private static final int INDEX_IDENTIFIER = 1;
    private static final int INDEX_PERMITTABLE = 2;

    private static final String TABLE_NAME = "isis_permittable_groups";
    private static final String FETCH_ALL_STMT = " SELECT * FROM " + IsisPermittableDAO.TABLE_NAME;
    private static final String FIND_ONE_STMT = " SELECT * FROM " + IsisPermittableDAO.TABLE_NAME + " WHERE identifier = ?";
    private static final String INSERT_STMT = " INSERT INTO " + IsisPermittableDAO.TABLE_NAME +
            " (identifier, permittables) " +
            " values " +
            " (?, ?) ";
    private static final String DELETE_STMT = " DELETE FROM " + IsisPermittableDAO.TABLE_NAME + " WHERE identifier = ? ";

    private String identifier;
    private String permittables;
//    private String driverClass;
//    private String databaseName;
//    private String host;
//    private String port;
//    private String user;
//    private String password;

    public IsisPermittableDAO() {
        super();
    }

    private static Builder create(final ResultSet resultSet) {
        return new Builder(resultSet);
    }

    public static Optional<IsisPermittableDAO> find(final Connection connection, final String identifier) throws SQLException {
        try (final PreparedStatement findOneTenantStatement = connection.prepareStatement(IsisPermittableDAO.FIND_ONE_STMT)) {
            findOneTenantStatement.setString(INDEX_IDENTIFIER, identifier);
            try (final ResultSet resultSet = findOneTenantStatement.executeQuery()) {
                return IsisPermittableDAO.create(resultSet).build();
            }
        }
    }

    public static List<IsisPermittableDAO> fetchAll(final Connection connection) throws SQLException {
        try (final Statement fetchAllTenantsStatement = connection.createStatement()) {
            try (final ResultSet resultSet = fetchAllTenantsStatement.executeQuery(IsisPermittableDAO.FETCH_ALL_STMT)) {
                return IsisPermittableDAO.create(resultSet).collect();
            }
        }
    }

    public static void delete(final Connection connection, final String identifier) throws SQLException {
        try (final PreparedStatement deleteTenantStatement = connection.prepareStatement(IsisPermittableDAO.DELETE_STMT)) {
            deleteTenantStatement.setString(INDEX_IDENTIFIER, identifier);
            deleteTenantStatement.execute();
        }
    }

    public void insert(final Connection connection) throws SQLException {
        try (final PreparedStatement insertTenantStatement = connection.prepareStatement(IsisPermittableDAO.INSERT_STMT)) {
            insertTenantStatement.setString(INDEX_IDENTIFIER, this.getIdentifier());
            insertTenantStatement.setString(INDEX_PERMITTABLE, this.getPermittables());
            insertTenantStatement.execute();
        }
    }



    public DatabaseConnectionInfo map() {
        final DatabaseConnectionInfo databaseConnectionInfo = new DatabaseConnectionInfo();
//        databaseConnectionInfo.setDriverClass(this.getDriverClass());
//        databaseConnectionInfo.setDatabaseName(this.getDatabaseName());
//        databaseConnectionInfo.setHost(this.getHost());
//        databaseConnectionInfo.setPort(this.getPort());
//        databaseConnectionInfo.setUser(this.getUser());
//        databaseConnectionInfo.setPassword(this.getPassword());
        return databaseConnectionInfo;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public void setPermittables(String permittables){
        this.permittables = permittables;
    }
    private String getPermittables() {
        return this.permittables;
    }

//    private String getDriverClass() {
//        return driverClass;
//    }
//
//    public void setDriverClass(String driverClass) {
//        this.driverClass = driverClass;
//    }
//
//    private String getDatabaseName() {
//        return databaseName;
//    }
//
//    public void setDatabaseName(String databaseName) {
//        this.databaseName = databaseName;
//    }
//
//    private String getHost() {
//        return host;
//    }
//
//    public void setHost(String host) {
//        this.host = host;
//    }
//
//    private String getPort() {
//        return port;
//    }
//
//    public void setPort(String port) {
//        this.port = port;
//    }
//
//    private String getUser() {
//        return user;
//    }
//
//    public void setUser(String user) {
//        this.user = user;
//    }
//
//    private String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
}
