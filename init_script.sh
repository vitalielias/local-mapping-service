#!/bin/sh

# Wait for the Mapping Service to start
sleep 30

# POST the mapping schemas
curl -X POST http://mapping-service:8095/api/v1/mappingAdministration \
     -H "Content-Type: multipart/form-data" \
     -F "record=@/initializer_files/semAcqMapping.txt" \
     -F "document=@/initializer_files/test2_SEMTomoAcqToJSON_1.0.0.mapping"
