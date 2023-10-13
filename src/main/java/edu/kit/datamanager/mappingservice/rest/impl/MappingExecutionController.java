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
package edu.kit.datamanager.mappingservice.rest.impl;

import edu.kit.datamanager.mappingservice.dao.IMappingRecordDao;
import edu.kit.datamanager.mappingservice.domain.MappingRecord;
import edu.kit.datamanager.mappingservice.exception.MappingException;
import edu.kit.datamanager.mappingservice.exception.MappingExecutionException;
import edu.kit.datamanager.mappingservice.exception.MappingNotFoundException;
import edu.kit.datamanager.mappingservice.impl.MappingService;
import edu.kit.datamanager.mappingservice.plugins.MappingPluginException;
import edu.kit.datamanager.mappingservice.plugins.MappingPluginState;
import edu.kit.datamanager.mappingservice.rest.IMappingExecutionController;
import edu.kit.datamanager.mappingservice.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Controller for executing document mappings via REST API.
 *
 * @author maximilianiKIT
 */
@Controller
@RequestMapping(value = "/api/v1/mappingExecution")
public class MappingExecutionController implements IMappingExecutionController{

  private static final Logger LOG = LoggerFactory.getLogger(MappingExecutionController.class);

  private final MappingService mappingService;

  private final IMappingRecordDao mappingRecordDao;

  public MappingExecutionController(MappingService mappingService, IMappingRecordDao mappingRecordDao){
    this.mappingService = mappingService;
    this.mappingRecordDao = mappingRecordDao;
  }

  @Override
  public void mapDocument(MultipartFile document, String mappingID, HttpServletRequest request, HttpServletResponse response, UriComponentsBuilder uriBuilder){
    LOG.trace("Performing mapDocument(File#{}, {})", document.getOriginalFilename(), mappingID);

    Optional<Path> resultPath;
    if(!document.isEmpty() && !mappingID.isBlank()){
      LOG.trace("Obtaining mapping for id {}.", mappingID);
      Optional<MappingRecord> record = mappingRecordDao.findByMappingId(mappingID);
      if(record.isEmpty()){
        String message = String.format("No mapping found for mapping id %s.", mappingID);
        LOG.error(message + " Returning HTTP 404.");
        throw new MappingNotFoundException(message);
        //return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
      }

      LOG.trace("Receiving mapping input file.");
      String extension = "." + FilenameUtils.getExtension(document.getOriginalFilename());
      LOG.trace("Found file extension: {}", extension);
      Path inputPath = FileUtil.createTempFile("inputMultipart", extension);
      LOG.trace("Writing user upload to: {}", inputPath);
      File inputFile = inputPath.toFile();
      try{
        document.transferTo(inputFile);
        LOG.trace("Successfully received user upload.");
      } catch(IOException e){
        LOG.error("Failed to receive upload from user.", e);
        throw new MappingExecutionException("Unable to write user upload to disk.");
      }

      try{
        LOG.trace("Performing mapping process of file {} via mapping service", inputPath.toString());

        resultPath = mappingService.executeMapping(inputFile.toURI(), mappingID);
        if(resultPath.isPresent()){
          LOG.trace("Mapping process finished. Output written to {}.", resultPath.toString());
        } else{
          throw new MappingPluginException(MappingPluginState.UNKNOWN_ERROR, "Mapping process finished, but no result was returned.");
        }
      } catch(MappingPluginException e){
        LOG.error("Failed to execute mapping.", e);
        throw new MappingExecutionException("Failed to execute mapping with id " + mappingID + " on provided input document.");
      } finally{
        LOG.trace("Removing user upload at {}.", inputFile);
        FileUtil.removeFile(inputPath);
        LOG.trace("User upload successfully removed.");
      }
    } else{
      String message = "Either mapping id or input document are missing. Unable to perform mapping.";
      LOG.error(message);
      throw new MappingException(message);
    }

    Path result = resultPath.get();
    if(!Files.exists(result) || !Files.isRegularFile(result) || !Files.isReadable(result)){
      String message = "The mapping result expected at path " + result + " is not accessible. This indicates an error of the mapper implementation.";
      LOG.error(message);
      throw new MappingExecutionException(message);
    }

    LOG.trace("Determining mime type for mapping result.");
    result = FileUtil.fixFileExtension(result);

    String mimeType = FileUtil.getMimeType(result);
    LOG.trace("Determining file extension for mapping result.");
    String extension = FileUtil.getExtensionForMimeType(mimeType);

    LOG.trace("Using mime type {} and extension {}.", mimeType, extension);

    response.setStatus(HttpStatus.OK.value());
    response.setHeader("Content-Type", mimeType);
    response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.toFile().length()));
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + "filename=result" + extension);
    try{
      Files.copy(result, response.getOutputStream());

    } catch(IOException ex){
      String message = "Failed to write mapping result file to stream.";
      LOG.error(message, ex);
      throw new MappingExecutionException(message);
    } finally{
      LOG.trace("Result file successfully transferred to client. Removing file {} from disk.", result);
      try{
        Files.delete(result);
        LOG.trace("Result file successfully removed.");
      } catch(IOException ignored){
        LOG.warn("Failed to remove result file. Please remove manually.");
      }
    }
  }
}
