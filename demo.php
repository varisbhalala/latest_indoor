<?php
$server = "localhost";
$user = "root";
$password = "";
$DB_name = "indoor";

// Connect to the server.
$connection = mysqli_connect($server, $user, $password, $DB_name) or die(mysql_error());

$query = "SELECT * FROM data";
$result = mysqli_query($connection, $query) or die("Error hai bhai!");
$storage = array();

while($data = mysqli_fetch_assoc($result)) {
    $temp_array = array($data['id'], $data['latitude'], $data['longitude']);
    array_push($storage, $temp_array);
}

header("Content-type:application/json");
print_r(json_encode($storage));
?>
