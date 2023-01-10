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
package org.apache.fineract.cn.identity.internal.service;

import org.apache.fineract.cn.anubis.api.v1.domain.PermittableEndpoint;
import org.apache.fineract.cn.identity.api.v1.domain.PermittableGroup;
import org.apache.fineract.cn.identity.internal.repository.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Myrle Krantz
 */
@Service
@Profile("postgres")
public class PermittableGroupServiceForSQL {
  private final PermittableGroupsForSQL repository;

  public PermittableGroupServiceForSQL(final PermittableGroupsForSQL repository) {
    this.repository = repository;
  }

  public Optional<PermittableGroup> findByIdentifier(final String identifier) {
    final Optional<PermittableGroupEntityForSQL> ret = repository.get(identifier);

    return ret.map(this::mapPermittableGroup);
  }

  public List<PermittableGroup> findAll() {
    return repository.getAll().stream()
            .map(this::mapPermittableGroup)
            .collect(Collectors.toList());
  }

  private PermittableGroup mapPermittableGroup(final PermittableGroupEntityForSQL permittableGroupEntityForSQL) {
    final PermittableGroup ret = new PermittableGroup();
    ret.setIdentifier(permittableGroupEntityForSQL.getIdentifier());
    ret.setPermittables(permittableGroupEntityForSQL.getPermittables().stream()
            .map(this::mapPermittable)
            .collect(Collectors.toList()));
    return ret;
  }

  private PermittableEndpoint mapPermittable(final PermittableType entity) {
    final PermittableEndpoint ret = new PermittableEndpoint();
    ret.setMethod(entity.getMethod());
    ret.setGroupId(entity.getSourceGroupId());
    ret.setPath(entity.getPath());
    return ret;
  }
}
