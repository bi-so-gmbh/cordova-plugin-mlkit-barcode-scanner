/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);

const options = {
  beepOnSuccess: false,
  vibrateOnSuccess: false,
  detectorSize: 0.9,
  detectorAspectRatio: "5:1",
  drawFocusRect: true,
  focusRectColor: "#FFFFFF",
  focusRectBorderRadius: 10,
  focusRectBorderThickness: 5,
  drawFocusLine: false,
  focusLineColor: "#ff2d37",
  focusLineThickness: 2,
  drawFocusBackground: false,
  focusBackgroundColor: "#66FFFFFF",
  stableThreshold: 5,
  debugOverlay: false,
  ignoreRotatedBarcodes: false
};

function onSuccess(result) {
  const scan = document.createElement('div');
    for (const barcode of result) {
        const node = document.createElement('div');
        node.className = 'log_item'
        node.textContent = `${barcode.value} (${barcode.format}/${barcode.type} - ${barcode.distanceToCenter})`;
        scan.appendChild(node)
    }
  document.getElementById('output').prepend(scan);
}

function onFail(result) {
  if(result.cancelled) {
    const node = document.createElement('div');
    node.className = 'log_item'
    node.textContent = `${result.message}`;
    document.getElementById('output').prepend(node);
  }
}

function scan() {
  for (const key in options) {
    const element =  document.getElementById(key);
    if (element) {
      if (element.tagName === "INPUT" && element.type === "checkbox") {
        options[key] = element.checked
      }
      else {
        options[key] = element.value
      }
    }
  }

  cordova.plugins.mlkit.barcodeScanner.scan(options, onSuccess, onFail);
}

function clearLog() {
  let logItems = document.getElementsByClassName('log_item')
  logItems = [...logItems]
  for (const item of logItems) {
    item.parentNode.removeChild(item)
  }
}

function onDeviceReady() {
  console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
  document.getElementById('scan').onclick = scan;
  document.getElementById('clearLog').onclick = clearLog;

  for (const key in options) {
    const element =  document.getElementById(key);
    if (element) {
      if (element.tagName === "INPUT" && element.type === "range") {
        element.addEventListener("input", updateTextInput)
        element.nextElementSibling.value=options[key]
        element.value = options[key]
      }
      else if (element.tagName === "INPUT" && element.type === "checkbox") {
        element.checked = options[key]
      }
      else {
        element.value = options[key]
      }
    }
  }
}

function updateTextInput() {
  document.getElementById(this.id).nextElementSibling.value=this.value;
}
