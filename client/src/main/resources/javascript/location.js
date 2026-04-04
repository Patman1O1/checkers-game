navigator.geolocation.getCurrentPosition(
    function(position) {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;

        document.getElementById("status").innerText = "Lat: " + lat + ", Lon: " + lon;
        fetch('/location', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ lat: lat, lon: lon })
        });
    },

    function(error) { document.getElementById("status").innerText = "Error: " + error.message; }
);