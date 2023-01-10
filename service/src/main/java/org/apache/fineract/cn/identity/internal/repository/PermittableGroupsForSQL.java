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

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.Mapper;
import org.apache.fineract.cn.cassandra.core.CassandraSessionProvider;
import org.apache.fineract.cn.cassandra.core.TenantAwareCassandraMapperProvider;
//import org.apache.fineract.cn.cassandra.core.TenantAwarePostgresMapperProvider;
import org.apache.fineract.cn.cassandra.core.TenantAwareEntityTemplate;
import org.apache.fineract.cn.identity.internal.util.DataSourceUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Myrle Krantz
 */
@Component
@Profile("postgres")
public class PermittableGroupsForSQL {
  static final String TABLE_NAME = "isis_permittable_groups";
  static final String IDENTIFIER_COLUMN = "identifier";
  static final String PERMITTABLES_COLUMN = "permittables";

  static final String TYPE_NAME = "isis_permittable_group";
  static final String PATH_FIELD = "path";
  static final String METHOD_FIELD = "method";
  static final String SOURCE_GROUP_ID_FIELD = "source_group_id";

  private final CassandraSessionProvider cassandraSessionProvider;
  private final TenantAwareEntityTemplate tenantAwareEntityTemplate;
//  private final IsisPermittableDAO isisPermittableDAO;
  private final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider;
  @Autowired
  private Flyway flyway;

  @Autowired
  private final Environment environment;

//  private final DataSource dataSource;
//  private final FlywayFactoryBean flywayFactoryBean;

  @Autowired
  PermittableGroupsForSQL(
          final CassandraSessionProvider cassandraSessionProvider,
          final TenantAwareEntityTemplate tenantAwareEntityTemplate,
          final TenantAwareCassandraMapperProvider tenantAwareCassandraMapperProvider, Environment environment) {
    this.cassandraSessionProvider = cassandraSessionProvider;
    this.tenantAwareEntityTemplate = tenantAwareEntityTemplate;
    this.tenantAwareCassandraMapperProvider = tenantAwareCassandraMapperProvider;
    this.environment = environment;
  }

  public void buildTable() {
    flyway.setLocations("db/migrations/postgresql");
    flyway.setDataSource("jdbc:postgresql://localhost:5432/seshat","postgres","postgres");
    flyway.setBaselineOnMigrate(true);
//    flyway.setSchemas("seshat");
    flyway.migrate();
  }

  public void add(final PermittableGroupEntityForSQL instance) {
    tenantAwareEntityTemplate.save(instance);
  }

  public Optional<PermittableGroupEntityForSQL> get(final String identifier)
  {
    final PermittableGroupEntityForSQL instance =
            tenantAwareCassandraMapperProvider.getMapper(PermittableGroupEntityForSQL.class).get(identifier);

    if (instance != null) {
      Assert.notNull(instance.getIdentifier());
    }

    return Optional.ofNullable(instance);
  }

  public List<PermittableGroupEntityForSQL> getAll() {
    final Session tenantSession = cassandraSessionProvider.getTenantSession();
    final Mapper<PermittableGroupEntityForSQL> entityMapper = tenantAwareCassandraMapperProvider.getMapper(PermittableGroupEntityForSQL.class);

    final Statement statement = QueryBuilder.select().all().from(TABLE_NAME);

    return new ArrayList<>(entityMapper.map(tenantSession.execute(statement)).all());
  }
}
