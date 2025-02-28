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

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.Frozen;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Objects;

/**
 * @author Myrle Krantz
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Profile("postgres")
@Table(name = PermittableGroups.TABLE_NAME)
public class PermittableGroupEntityForSQL {
  @Column(name = PermittableGroups.IDENTIFIER_COLUMN)
  private String identifier;

  @Frozen
  @Column(name = PermittableGroups.PERMITTABLES_COLUMN)
  private List<PermittableType> permittables;

  public PermittableGroupEntityForSQL() {
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public List<PermittableType> getPermittables() {
    return permittables;
  }

  public void setPermittables(List<PermittableType> permittables) {
    this.permittables = permittables;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PermittableGroupEntityForSQL that = (PermittableGroupEntityForSQL) o;
    return Objects.equals(identifier, that.identifier) &&
            Objects.equals(permittables, that.permittables);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, permittables);
  }

  @Override
  public String toString() {
    return "PermittableGroupEntity{" +
            "identifier='" + identifier + '\'' +
            ", permittables=" + permittables +
            '}';
  }
}
