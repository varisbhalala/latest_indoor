$(document).ready(function() {
    $.ajax ({
        url: 'demo.php',
        type: 'GET',
        success: function(data) {
            var markers = JSON.stringify(data);
            document.write(markers);
        }
    });
    // .fail(function(request, status, error) {
	// 	console.log(request.responseText);
	// });
});

for(i = 0; i < markers.length; i++) {
    var position = new google.maps.LatLng(markers[i][1], markers[i][2]);
    bounds.extend(position);
    marker = new google.maps.Marker({
            position: position,
            map: map,
            title: markers[i][0]
        });
}
