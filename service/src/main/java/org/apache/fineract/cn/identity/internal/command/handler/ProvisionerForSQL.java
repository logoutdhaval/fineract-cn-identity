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
package org.apache.fineract.cn.identity.internal.command.handler;

import com.datastax.driver.core.exceptions.InvalidQueryException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.fineract.cn.anubis.api.v1.domain.ApplicationSignatureSet;
import org.apache.fineract.cn.crypto.SaltGenerator;
import org.apache.fineract.cn.identity.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.identity.internal.mapper.SignatureMapper;
import org.apache.fineract.cn.identity.internal.repository.*;
import org.apache.fineract.cn.identity.internal.util.DataSourceUtils;
import org.apache.fineract.cn.identity.internal.util.IdentityConstants;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.lang.TenantContextHolder;
import org.apache.fineract.cn.lang.security.RsaKeyPairFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author Myrle Krantz
 */
@Component
@Profile("postgres")
public class ProvisionerForSQL {
    private final Signatures signature;
    private final Tenants tenant;
    private final Users users;
    private final PermittableGroupsForSQL permittableGroupsForSQL;
    private final Permissions permissions;
    private final Roles roles;
    @Autowired
    private final Environment environment;

    private final ApplicationSignatures applicationSignatures;
    private final ApplicationPermissions applicationPermissions;
    private final ApplicationPermissionUsers applicationPermissionUsers;
    private final ApplicationCallEndpointSets applicationCallEndpointSets;
    private final UserEntityCreator userEntityCreator;
    private final Logger logger;
    private final SaltGenerator saltGenerator;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${identity.passwordExpiresInDays:93}")
    private int passwordExpiresInDays;

    @Value("${identity.timeToChangePasswordAfterExpirationInDays:4}")
    private int timeToChangePasswordAfterExpirationInDays;

    @Autowired
    ProvisionerForSQL(
            final Signatures signature,
            final Tenants tenant,
            final Users users,
            final PermittableGroupsForSQL permittableGroupsForSQL,
            final Permissions permissions,
            final Roles roles,
            Environment environment, final ApplicationSignatures applicationSignatures,
            final ApplicationPermissions applicationPermissions,
            final ApplicationPermissionUsers applicationPermissionUsers,
            final ApplicationCallEndpointSets applicationCallEndpointSets,
            final UserEntityCreator userEntityCreator,
            @Qualifier(IdentityConstants.LOGGER_NAME) final Logger logger,
            final SaltGenerator saltGenerator)
    {
        this.signature = signature;
        this.tenant = tenant;
        this.users = users;
        this.permittableGroupsForSQL = permittableGroupsForSQL;
        this.permissions = permissions;
        this.roles = roles;
        this.environment = environment;
        this.applicationSignatures = applicationSignatures;
        this.applicationPermissions = applicationPermissions;
        this.applicationPermissionUsers = applicationPermissionUsers;
        this.applicationCallEndpointSets = applicationCallEndpointSets;
        this.userEntityCreator = userEntityCreator;
        this.logger = logger;
        this.saltGenerator = saltGenerator;
    }

    public synchronized ApplicationSignatureSet provisionTenant(final String initialPasswordHash) {
        {
            final Optional<ApplicationSignatureSet> latestSignature = signature.getAllKeyTimestamps().stream()
                    .max(String::compareTo)
                    .flatMap(signature::getSignature)
                    .map(SignatureMapper::mapToApplicationSignatureSet);

            if (latestSignature.isPresent()) {
                final Optional<ByteBuffer> fixedSalt = tenant.getPrivateTenantInfo().map(PrivateTenantInfoEntity::getFixedSalt);
                if (fixedSalt.isPresent()) {
                    logger.info("Changing password for tenant '{}' instead of provisioning...", TenantContextHolder
                            .checkedGetIdentifier());
                    final UserEntity suUser = userEntityCreator
                            .build(IdentityConstants.SU_NAME, IdentityConstants.SU_ROLE, initialPasswordHash, true,
                                    fixedSalt.get().array(), timeToChangePasswordAfterExpirationInDays);
                    users.add(suUser);
                    logger.info("Successfully changed admin password '{}'...", TenantContextHolder.checkedGetIdentifier());

                    return latestSignature.get();
                }
            }
        }

        logger.info("Provisioning cassandra tables for tenant '{}'...", TenantContextHolder.checkedGetIdentifier());
        final RsaKeyPairFactory.KeyPairHolder keys = RsaKeyPairFactory.createKeyPair();

        byte[] fixedSalt = this.saltGenerator.createRandomSalt();

        try {
            signature.buildTable();
            final SignatureEntity signatureEntity = signature.add(keys);

            tenant.buildTable();
            tenant.add(fixedSalt, passwordExpiresInDays, timeToChangePasswordAfterExpirationInDays);

            users.buildTable();
            permittableGroupsForSQL.buildTable();
            permissions.buildType();
            roles.buildTable();
            applicationSignatures.buildTable();
            applicationPermissions.buildTable();
            applicationPermissionUsers.buildTable();
            applicationCallEndpointSets.buildTable();


            createPermittablesGroup(PermittableGroupIds.ROLE_MANAGEMENT, "/roles/*", "/permittablegroups/*");
            createPermittablesGroup(PermittableGroupIds.IDENTITY_MANAGEMENT, "/users/*");
            createPermittablesGroup(PermittableGroupIds.SELF_MANAGEMENT, "/users/{useridentifier}/password", "/applications/*/permissions/*/users/{useridentifier}/enabled");
            createPermittablesGroup(PermittableGroupIds.APPLICATION_SELF_MANAGEMENT, "/applications/{applicationidentifier}/permissions");

            final List<PermissionType> permissions = new ArrayList<>();
            permissions.add(fullAccess(PermittableGroupIds.ROLE_MANAGEMENT));
            permissions.add(fullAccess(PermittableGroupIds.IDENTITY_MANAGEMENT));
            permissions.add(fullAccess(PermittableGroupIds.SELF_MANAGEMENT));
            permissions.add(fullAccess(PermittableGroupIds.APPLICATION_SELF_MANAGEMENT));

            final RoleEntity suRole = new RoleEntity();
            suRole.setIdentifier(IdentityConstants.SU_ROLE);
            suRole.setPermissions(permissions);

            roles.add(suRole);

            final UserEntity suUser = userEntityCreator
                    .build(IdentityConstants.SU_NAME, IdentityConstants.SU_ROLE, initialPasswordHash, true,
                            fixedSalt, timeToChangePasswordAfterExpirationInDays);
            users.add(suUser);

            final ApplicationSignatureSet ret = SignatureMapper.mapToApplicationSignatureSet(signatureEntity);

            logger.info("Successfully provisioned cassandra tables for tenant '{}'...", TenantContextHolder.checkedGetIdentifier());

            return ret;
        }
        catch (final InvalidQueryException e)
        {
            logger.error("Failed to provision cassandra tables for tenant.", e);
            throw ServiceException.internalError("Failed to provision tenant.");
        }
    }

    private PermissionType fullAccess(final String permittableGroupIdentifier) {
        final PermissionType ret = new PermissionType();
        ret.setPermittableGroupIdentifier(permittableGroupIdentifier);
        ret.setAllowedOperations(AllowedOperationType.ALL);
        return ret;
    }

    private void createPermittablesGroup(final String identifier, final String... paths) {
        final PermittableGroupEntityForSQL permittableGroupEntityForSQL = new PermittableGroupEntityForSQL();
        permittableGroupEntityForSQL.setIdentifier(identifier);
        permittableGroupEntityForSQL.setPermittables(Arrays.stream(paths).flatMap(this::permittables).collect(Collectors.toList()));
        permittableGroupsForSQL.add(permittableGroupEntityForSQL);
        try (
                final Connection connection = DataSourceUtils.createConnection(this.environment,"seshat");
        ) {

                final IsisPermittableDAO isisPermittableDAO = new IsisPermittableDAO();
                isisPermittableDAO.setIdentifier(identifier);
                isisPermittableDAO.setPermittables(permittableGroupEntityForSQL.getPermittables().toString());
                isisPermittableDAO.insert(connection);

        }
        catch (SQLException sqlex) {
    //      this.logger.error(sqlex.getMessage(), sqlex);
          throw new IllegalStateException("Could not provision database for tenant {}" + sqlex);
        }
    }

    private Stream<PermittableType> permittables(final String path)
    {
        final PermittableType getret = new PermittableType();
        getret.setPath(applicationName + path);
        getret.setMethod("GET");

        final PermittableType postret = new PermittableType();
        postret.setPath(applicationName + path);
        postret.setMethod("POST");

        final PermittableType putret = new PermittableType();
        putret.setPath(applicationName + path);
        putret.setMethod("PUT");

        final PermittableType delret = new PermittableType();
        delret.setPath(applicationName + path);
        delret.setMethod("DELETE");

        final List<PermittableType> ret = new ArrayList<>();
        ret.add(getret);
        ret.add(postret);
        ret.add(putret);
        ret.add(delret);

        return ret.stream();
    }
}
