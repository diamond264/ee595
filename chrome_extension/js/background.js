var db = "https://ee595-c30a7-default-rtdb.asia-southeast1.firebasedatabase.app/";
var sdata = "sdata";
var linkdata = "linkdata";
var J = ".json";

// test code
fetch('access.token')
			.then(response => response.text())
			.then(access_token => {
				var auth = "?access_token="+access_token;

				fetch(db+sdata+J+auth)
					.then(response => response.json())
					.then(data => {
						var min_key = "";
						var min_ts = 0;
						var keys = Object.keys(data);

						if (keys.length > 0) {
							min_key = keys[0];
							min_ts = data[min_key]['timestamp'];
						}

						for (var i=0; i<keys.length; i++) {
							var key = keys[i];
							if (min_ts > data[key]['timestamp']) {
								min_key = key;
								min_ts = data[key]['timestamp'];
							}
						}

						console.log(data[min_key]);
						// SendResponse(data[min_key]);
					});
				});

fetch('access.token')
	.then(response => response.text())
	.then(access_token => {
		var auth = "?access_token="+access_token;

		fetch(db+linkdata+J+auth, {
			method: "POST",
			headers: {"Content-Type": "application/json"},
			body: JSON.stringify({url: "youtube.com"})
		}).then(response => console.log(response));
	});

chrome.runtime.onMessage.addListener(
	(request, sender, sendResponse) => {
	console.log("[background] request:" + request.type);
	var timed_url = ""

	if (request.type === 'requestSdata') {
		fetch('access.token')
			.then(response => response.text())
			.then(access_token => {
				var auth = "?access_token="+access_token;
				console.log(auth);
				console.log(access_token);

				fetch(db+sdata+J+auth)
					.then(response => response.json())
					.then(data => {
						var min_key = "";
						var min_ts = 0;
						var keys = Object.keys(data);

						if (keys.length > 0) {
							min_key = keys[0];
							min_ts = data[min_key]['timestamp'];
						}

						for (var i=0; i<keys.length; i++) {
							var key = keys[i];
							if (min_ts > data[key]['timestamp']) {
								min_key = key;
								min_ts = data[key]['timestamp'];
							}
						}

						console.log(data[min_key]);
						SendResponse(data[min_key]);
					});
				});

		// test code
		// Random directions/distances
		var directions = ['up', 'right', 'left'];
		var distances = [1, 3];
		var dir = directions[Math.floor(Math.random() * directions.length)];
		var dis = distances[Math.floor(Math.random() * distances.length)]
		sendResponse({distance: dis, direction: dir, attention: 'monitor'});
	}

	if (request.type === 'sendUrl') {
		timed_url = request.url

		fetch('access.token')
			.then(response => response.text())
			.then(access_token => {
				var auth = "?access_token="+access_token;

				fetch(db+linkdata+J+auth, {
					method: "POST",
					headers: {"Content-Type": "application/json"},
					body: JSON.stringify({url: timed_url})
				}).then(response => console.log(response));
			});
	}
});