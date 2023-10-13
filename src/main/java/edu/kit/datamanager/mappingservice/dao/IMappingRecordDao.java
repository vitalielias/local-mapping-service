/*
 * Copyright 2022 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kit.datamanager.mappingservice.dao;

import edu.kit.datamanager.mappingservice.domain.MappingRecord;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * This interface defines the methods for accessing the database for the MappingRecords.
 *
 * @author maximilianiKIT
 */
public interface IMappingRecordDao extends JpaRepository<MappingRecord, String>, JpaSpecificationExecutor<MappingRecord> {

    /**
     * Find a MappingRecords by the given ID.
     *
     * @param mappingId The id to search for.
     * @return A optional of the matching MappingRecord.
     */
    Optional<MappingRecord> findByMappingId(String mappingId);

    Iterable<MappingRecord> findByMappingIdIn(List<String> mappingId);

    Page<MappingRecord> findByMappingIdIn(List<String> mappingId, Pageable pgbl);
}
