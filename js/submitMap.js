function submitMap() {

	var json_data = {
        "floor_map_name": document.getElementById("imgInp").value,
        "floor_no": document.getElementById("floor-number").value,
        "floor_name": document.getElementById("floor-name").value
	}


	// console.log("Lat: " + latitude);
	// console.log("Lng: " + longitude);
	$('#response').html("<h5>Finishing up...</h5>");

	$.ajax ({
		type: 'POST',
		url: 'addToPHPmap.php',
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
