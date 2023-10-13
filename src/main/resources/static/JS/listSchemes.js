const apiUrl = "./api/v1/mappingAdministration/";

let records = new Map();

function getRecords() {
    const http = new XMLHttpRequest();
    http.open("GET", apiUrl);
    http.send();
    http.onprogress = () => {
        document.getElementById("progress").hidden = false;
    };
    http.onload = (e) => {
        const results = JSON.parse(http.responseText);
        if (results.length > 0) {
            document.getElementById("nothingHere").hidden = true;
            document.getElementById("progress").hidden = true;
        }
        for (let i = 0; i < results.length; i++) {
            document.getElementById("progress").hidden = false;
            console.log(results[i].mappingId);
            const schemaHttp = new XMLHttpRequest();
            //var schema
            var ETAG

            schemaHttp.open("GET", apiUrl + results[i].mappingId);
            schemaHttp.setRequestHeader("Accept", "application/vnd.datamanager.mapping-record+json");
            schemaHttp.send();
            schemaHttp.onload = (e) => {
              //  schema = JSON.parse(schemaHttp.responseText);
                ETAG = schemaHttp.getResponseHeader("ETag");
                console.log("Received Data:");
                console.log({
                    "record": results[i],
                    //"schema": schema,
                    "ETAG": ETAG
                });
                records.set(results[i].mappingId, {
                    "record": results[i],
                    //"schema": schema,
                    "ETAG": ETAG
                })
                console.log(records);
                addListElement(results[i].mappingId, results[i].mappingType, results[i].title, results[i].description);
            };
        }
        document.getElementById("progress").hidden = true;
    };
    document.getElementById("progress").hidden = true;
}

function mapWithMapping(id) {
    const data = {
        id: id,
        type: records.get(id).record.mappingType
    };
    window.sessionStorage.setItem("data", JSON.stringify(data));
    window.location = "mapDocument.html";
}

function editMapping(id) {
    let sessionData = JSON.stringify(records.get(id))
    window.sessionStorage.setItem("data", sessionData)
    window.location = "./addScheme.html";
}

function downloadMapping(id) {
    const http = new XMLHttpRequest();
    http.open("GET", apiUrl + id);
    http.send();
    http.onload = (e) => {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(http.responseText));
        element.setAttribute('download', id + "_schema.json");
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    };
}

function deleteMapping(id) {
    let mapEntry = records.get(id);
    console.log("Deleting mapping with id " + id);
    if (mapEntry !== null && mapEntry.record.mappingId === id) {
        const http = new XMLHttpRequest();
        http.open("DELETE", apiUrl + id);
        http.setRequestHeader("If-Match", mapEntry.ETAG);
        http.send();
        http.onload = (e) => {
            records.delete(id);
            document.getElementById(id).remove();
            if (records.size < 1) document.getElementById("nothingHere").hidden = false;
            console.log("Successfully deleted mapping with id " + id);
        };
    }
}

function addListElement(id, type, title, description) {
    let element =
        `<li class="list-group-item" id="${id}">
            <div class="row align-items-center clearfix">
                <div class="me-auto float-start col-auto row align-items-center">
                    <div class="me-auto col-auto row align-items-center">
                        <h4 class="p-1 col-auto">${type}</h4>
                        <span class="col-auto p-1 text-muted">${id}</span>
                    </div>
                    <div class="me-auto col-auto row align-items-center">
                        <h4 class="p-1 col-auto">${title}</h4>
                        <span class="col-auto p-1 text-muted">${description}</span>
                    </div>
                </div>
    
                <div class="float-end col-auto ms-auto">
                    <button class="btn btn-primary col-auto m-1" onclick="editMapping('${id}')">
                        <svg class="bi me-1" fill="currentColor" width="16" height="16">
                            <use xlink:href="#editButton"/>
                        </svg>
                        Edit
                    </button>
                    <button class="btn btn-primary col-auto m-1" onclick="mapWithMapping('${id}')">
                        <svg class="bi me-1" fill="currentColor" width="16" height="16">
                            <use xlink:href="#mapButton"/>
                        </svg>
                        Map document
                    </button>
                    <button class="btn btn-primary col-auto m-1" onclick="downloadMapping('${id}')">
                        <svg class="bi me-1" fill="currentColor" width="16" height="16">
                            <use xlink:href="#downloadButton"/>
                        </svg>
                        Download
                    </button>
                    <button class="btn btn-danger col-auto m-1" onclick="deleteMapping('${id}')">
                        <svg class="bi me-1" fill="currentColor" width="16" height="16">
                            <use xlink:href="#deleteButton"/>
                        </svg>
                        Delete
                    </button>
                </div>
            </div>
        </li>`;

    let html = document.getElementById('list');
    html.innerHTML += element;
}