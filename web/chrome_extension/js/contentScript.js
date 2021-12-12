console.log("content start");

var cnt = 1;
var cnt_limit = 10;

function myLoop() {
	setTimeout(function() {
		chrome.runtime.sendMessage({type: 'requestSdata'}, (response) => {
			console.log("[contentscript] chrome.runtime.sendMessage('requestSdata')");

			rotate = 0;
			scale = 1;

			var video = document.getElementsByClassName('video-stream html5-main-video')[0];
			if (response.attention === 'monitor') {
				if (video.paused) {
					if (response.last_link != '') {
						var video_link = response.last_link.split('?t=')[0];
						var sec_link = response.last_link.split('?t=')[1];
						if (video_link === document.URL)
							video.currentTime = parseFloat(sec_link);
					}
					video.play();
				}
				if (response.distance > 2) {
					scale = 2;
				}

				if (response.direction === 'right') {
					rotate = 90;
					if (scale > 1) scale = 1.5;
				} else if (response.direction === 'left') {
					rotate = 270;
					if (scale > 1) scale = 1.5;
				}

				document.getElementById("movie_player").style.webkitTransform = "rotate("+rotate+"deg) scale("+scale+")"
				document.getElementById("movie_player").style["z-index"] = 60;
			} else {
				if (!video.paused)
					video.pause();
			}

			var url = document.URL;
			var sec = String(parseInt(video.currentTime));
			var url_with_time = url+'?t='+sec
			console.log(url+'?t='+sec);

			chrome.runtime.sendMessage({type: 'sendUrl', url: url_with_time}, (response) => {
				console.log("[contentscript] chrome.runtime.sendMessage(sendUrl)");
			});
		});
		cnt++;
		if (cnt < cnt_limit) {
			myLoop();
		}
	}, 5000)
}

myLoop();