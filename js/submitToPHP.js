function submitData() {

	var json_data = {
		"latitude": document.getElementById("latitude").innerHTML,
		"longitude": document.getElementById("longitude").innerHTML
	}


	// console.log("Lat: " + latitude);
	// console.log("Lng: " + longitude);
	$('#response').html("<h5>Finishing up...</h5>");

	$.ajax ({
		type: 'POST',
		url: 'addToPHP.php',
		data: json_data,
		dataType: 'text',
	})
	.done(function(data){
		// show the response
        $('#response').html(data);
		dataType: 'text'
    })
	.fail(function(request, status, error) {
		$('#response').html(request.responseText);
	});
	return false;
}
