function fileSelected() {
    var file = document.getElementById('fileToUpload').files[0];
    if (file) {
        var fileSize = 0;
        if (file.size > 1024 * 1024)
            fileSize = (Math.round(file.size * 100 / (1024 * 1024)) / 100).toString() + 'MB';
        else
            fileSize = (Math.round(file.size * 100 / 1024) / 100).toString() + 'KB';

        document.getElementById('fileName').innerHTML = 'Name: ' + file.name;
        document.getElementById('fileSize').innerHTML = 'Size: ' + fileSize;
    }
}

function uploadProgress(evt) {
    if (evt.lengthComputable) {
        var percentComplete = Math.round(evt.loaded * 100 / evt.total);
        document.getElementById('progressNumber').innerHTML = percentComplete.toString() + '%';
    }
    else {
        document.getElementById('progressNumber').innerHTML = 'unable to compute';
    }
}

function uploadComplete(evt) {
}

function uploadFailed(evt) {
    document.getElementById('fileName').innerHTML = 'Upload failed =(';
}

function uploadCanceled(evt) {
    alert("The upload has been canceled by the user or the browser dropped the connection.");
}

function uploadFile() {
    var xhr = new XMLHttpRequest();
    var fd = new FormData(document.getElementById('uploadForm'));
    var complete = false;

    xhr.upload.addEventListener("progress", uploadProgress, false);
    xhr.addEventListener("load", uploadComplete, false);
    xhr.addEventListener("error", uploadFailed, false);
    xhr.addEventListener("abort", uploadCanceled, false);
    xhr.open("POST", "/browser-upload");
    xhr.send(fd);

    var statusDiv = document.getElementById('uploadStatus');
    xhr.onprogress = function () {
        if(complete){
            return;
        }
        var stateInProgress = 3;
        var stateDone = 4;
        if(xhr.readyState >= stateInProgress && xhr.responseText !== ""){
            console.log(xhr.responseText);
            statusDiv.innerHTML = xhr.responseText;
        }
        if(xhr.readyState === stateDone){
            complete = true;
        }
    };
}
document.getElementById('fileToUpload').addEventListener('change', function(){
    fileSelected();
});
document.getElementById('uploadButton').addEventListener('click', function(e){
    uploadFile();
});