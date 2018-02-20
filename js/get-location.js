var map, infoWindow;
var imageLocation, overlay;
 var historicalOverlay;
function initMap() {
    map = new google.maps.Map(document.getElementById('map'), {
        center: {lat: 22.5, lng: 74.2},
        zoom: 15,
        mapTypeControl: true,
        mapTypeControlOptions: {
            style: google.maps.MapTypeControlStyle.HORIZONTAL_BAR,
            position: google.maps.ControlPosition.RIGHT_CENTER
        },
        zoomControl: true,
        zoomControlOptions: {
            position: google.maps.ControlPosition.RIGHT_CENTER
        },
        scaleControl: true,
        streetViewControl: false,
        streetViewControlOptions: {
            position: google.maps.ControlPosition.RIGHT_BOTTOM
        },
        fullscreenControl: true,
        fullscreenControlOptions: {
            position: google.maps.ControlPosition.RIGHT_BOTTOM
        }
    });

    // Create the search box and link it to the UI element.
    var input = document.getElementById('search-input');
    var searchBox = new google.maps.places.SearchBox(input);
    //map.controls[google.maps.ControlPosition.TOP_LEFT].push(input);

    // Bias the SearchBox results towards current map's viewport.
    map.addListener('bounds_changed', function() {
        searchBox.setBounds(map.getBounds());
    });

    var markers = [];
    // Listen for the event fired when the user selects a prediction and retrieve
    // more details for that place.
    searchBox.addListener('places_changed', function() {
        var places = searchBox.getPlaces();

        if (places.length == 0) {
            return;
        }

        // Clear out the old markers.
        markers.forEach(function(marker) {
            marker.setMap(null);
        });
        markers = [];

        // For each place, get the icon, name and location.
        var bounds = new google.maps.LatLngBounds();
        places.forEach(function(place) {
            if (!place.geometry) {
                console.log("Returned place contains no geometry");
                return;
            }
            var icon = {
                url: place.icon,
                size: new google.maps.Size(71, 71),
                origin: new google.maps.Point(0, 0),
                anchor: new google.maps.Point(17, 34),
                scaledSize: new google.maps.Size(25, 25)
            };

            // Create a marker for each place.
            markers.push(new google.maps.Marker({
                map: map,
                icon: icon,
                title: place.name,
                position: place.geometry.location,
                draggable: true
            }));

            if (place.geometry.viewport) {
                // Only geocodes have viewport.
                bounds.union(place.geometry.viewport);
            } else {
                bounds.extend(place.geometry.location);
            }
        });
        map.fitBounds(bounds);
    });



    //This is a pop-up if location is found.
    infoWindow = new google.maps.InfoWindow;

    // Try HTML5 geolocation.
    if (navigator.geolocation) {
        console.log("Location services are supported!");
        navigator.geolocation.getCurrentPosition(function(position) {
            var pos = {
                lat: position.coords.latitude,
                lng: position.coords.longitude
            };

            document.getElementById("latitude").innerHTML = pos.lat;
            document.getElementById("longitude").innerHTML = pos.lng;

            //This shows a pop-up when the location is found. Enable if required.
            //infoWindow.setPosition(pos);
            //infoWindow.setContent('Location found.');
            //infoWindow.open(map);
            map.setCenter(pos);

            var marker = new google.maps.Marker({
                draggable: true,
                position: pos,
                map: map
            });

            // This code block is just for demo of 'click' event. Uncomment only if you are sure of what you are trying to do.
            // google.maps.event.addListener(marker, 'click', function(event) {
            //     map.setCenter(marker.getPosition());
            //     placeMarkerAndPanTo(event.latLng, map);
            //
            //     //Get new coordinates after dragging the marker on the map.
            // 	console.log(event.latLng.lat());
            // 	console.log(event.latLng.lng());
            //
            // 	//Change the values in the HTML.
            // 	document.getElementById("latitude").innerHTML = event.latLng.lat();
            // 	document.getElementById("longitude").innerHTML = event.latLng.lng();
            // });

            google.maps.event.addListener(marker, 'dragend', function(event) {
                map.setCenter(marker.getPosition());

                //Get new coordinates after dragging the marker on the map.
                console.log(event.latLng.lat());
                console.log(event.latLng.lng());

                //Change the values in the HTML.
                document.getElementById("latitude").innerHTML = event.latLng.lat();
                document.getElementById("longitude").innerHTML = event.latLng.lng();
                // infoWindow.open(map, marker);

            });
        }, function() {
            handleLocationError(true, infoWindow, map.getCenter());
        });
    } else {
        // Browser doesn't support Geolocation
        handleLocationError(false, infoWindow, map.getCenter());
        console.log("Browser does not support location services!");
    }
    // var imgSrc = document.getElementById('imageLocation').getAttribute("src");
    // console.log("image source: " + imgSrc);
    // overlay = new DraggableOverlay(map,
    //     map.getCenter(),
    //     '<img src="https://www.w3schools.com/css/img_fjords.jpg">'
    // );


  var imageBounds = {
    north: 22.552514096149313,
    south: 22.552271340508586,
    east: 72.92396115365909,
    west: 72.92364465299534
  };

  historicalOverlay = new google.maps.GroundOverlay(
      'https://iffkkq-sn3302.files.1drv.com/y4muT9tLY479qCbwCFb2kPTyoUDzLwMzAPkUu89ZV5F0XgdsljFRtWNyfTHCyF2i_W-xCnOIIvqTit5c4EL9cgdo1zIF_rxeROPtFIfidi5il6sNXlG5YtBwczpvm44OsrTpf-y4_lGM6fEk4JxFiycxXt7Znzl-uspLO7E7t1GDVX9CC5hqN7ubftaZYUJHuN1BgrxrVkU67-a81cFL5EYhQ?width=636&height=590&cropmode=none',
      imageBounds
  );
  historicalOverlay.setMap(map);
}

DraggableOverlay.prototype = new google.maps.OverlayView();

DraggableOverlay.prototype.onAdd = function() {
    var container=document.createElement('div'),
    that=this;

    if(typeof this.get('content').nodeName!=='undefined'){
        container.appendChild(this.get('content'));
    }
    else{
        if(typeof this.get('content')==='string'){
            container.innerHTML=this.get('content');
        }
        else{
            return;
        }
    }
    container.style.position='absolute';
    container.draggable=true;
    google.maps.event.addDomListener(this.get('map').getDiv(),
    'mouseleave',
    function(){
        google.maps.event.trigger(container,'mouseup');
    }
);


google.maps.event.addDomListener(container,
    'mousedown',
    function(e){
        this.style.cursor='move';
        that.map.set('draggable',false);
        that.set('origin',e);

        that.moveHandler  = google.maps.event.addDomListener(that.get('map').getDiv(),
        'mousemove',
        function(e){
            var origin = that.get('origin'),
            left   = origin.clientX-e.clientX,
            top    = origin.clientY-e.clientY,
            pos    = that.getProjection()
            .fromLatLngToDivPixel(that.get('position')),
            latLng = that.getProjection()
            .fromDivPixelToLatLng(new google.maps.Point(pos.x-left,
                pos.y-top));
                that.set('origin',e);
                that.set('position',latLng);
                that.draw();
            });


        }
    );

    google.maps.event.addDomListener(container,'mouseup',function(){
        that.map.set('draggable',true);
        this.style.cursor='default';
        google.maps.event.removeListener(that.moveHandler);
    });


    this.set('container',container)
    this.getPanes().floatPane.appendChild(container);
};

function DraggableOverlay(map,position,content){
    if(typeof draw==='function'){
        this.draw=draw;
    }
    this.setValues({
        position:position,
        container:null,
        content:content,
        map:map
    });
}



DraggableOverlay.prototype.draw = function() {
    var pos = this.getProjection().fromLatLngToDivPixel(this.get('position'));
    this.get('container').style.left = pos.x + 'px';
    this.get('container').style.top = pos.y + 'px';
};

DraggableOverlay.prototype.onRemove = function() {
    this.get('container').parentNode.removeChild(this.get('container'));
    this.set('container',null)
};

//new google.maps.event.addDomListener(window, "load", initMap());


function handleLocationError(browserHasGeolocation, infoWindow, pos) {
    infoWindow.setPosition(pos);
    infoWindow.setContent(browserHasGeolocation ?
        'Error: The Geolocation service failed.' :
        'Error: Your browser doesn\'t support geolocation.');
        infoWindow.open(map);
}
