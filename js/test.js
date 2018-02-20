var canvasOverlay = null;
isCanvasOverlayActive = false;

$('#floor_ok').click(function () {


    if (!canvasOverlay) {
        return;
    }

    if ($('#building_number_select').val() == "-1") {
        console.log('building is undefined');
        alert("Something went wrong. It seems like there is no building selected");
        return;
    }

    var newFl = {
        is_published: 'true',
        buid: $('#building_number_select').val(),
        floor_name: $('#floor_number').val(),
        description: $('#floor_number').val(),
        floor_number: $('#floor_number').val(),
        owner_id: "1234"

    };

    var floor = {
        floor_plan_coords: {},
        floor_plan_base64_data: {},
        floor_plan_groundOverlay: null
    };


    // create the proper image inside the canvas
    canvasOverlay.drawBoundingCanvas();

    // create the ground overlay and destroy the canvasOverlay object
    // and also set the floor_plan_coords in $scope.data
    var bl = canvasOverlay.bottom_left_coords;
    var tr = canvasOverlay.top_right_coords;
    newFl.bottom_left_lat = bl.lat()+"";
    newFl.bottom_left_lng = bl.lng()+"";
    newFl.top_right_lat = tr.lat()+"";
    newFl.top_right_lng = tr.lng()+"";
    var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
    floor.floor_plan_base64_data = data;
    var imageBounds = new google.maps.LatLngBounds(
        new google.maps.LatLng(bl.lat(), bl.lng()),
        new google.maps.LatLng(tr.lat(), tr.lng()));
    floor.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, map);

    canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
    $('#floor_file').prop('disabled', false);
    isCanvasOverlayActive = false;

    var fl_data = floor.floor_plan_base64_data.replace('data:image/png;base64,', '');
    var uarray = LPUtils.Base64Binary.decode(fl_data);
    var blob = new Blob([uarray]);
    fl_data = "";
    for (var i = 0; i < uarray.length; i++) {
        fl_data += uarray[i];
    }
    var json_req = JSON.stringify(newFl);

    var formData = new FormData();
    formData.append("json", json_req);
    formData.append("floorplan", blob);
    $.ajax({
        url: FLOOR_PLAN_UPLOAD,
        data: formData,
        processData: false,
        contentType: false,
        type: 'POST',
        success: function (data) {
            alert(data);
        }
    });

});
$('#floor_cancel').click(function () {


    if (canvasOverlay) {
        canvasOverlay.setMap(null);
    }


    var x = $('#floor_file');
    x.val('')
    x.prop('disabled', false);

    isCanvasOverlayActive = false;
})

$('#floor_file').change(function handleImage(e) {
    var reader = new FileReader();
    reader.onload = function (event) {
        var imgObj = new Image();
        imgObj.src = event.target.result;
        imgObj.onload = function () {
            canvasOverlay = new CanvasOverlay(imgObj, map);
            isCanvasOverlayActive = true;

            $('#floor_control_bar').css("visibility", "visible");

            // hide previous floorplan

        }

    };
    reader.readAsDataURL(e.target.files[0]);

    this.disabled = true;
});