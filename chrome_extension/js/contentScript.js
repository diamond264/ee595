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
				document.getElementById("movie_player").style["z-index"] = 40;
			} else {
				if (!video.paused) {
					video.pause();
					var url = document.URL;
					var cur_time = document.getElementsByClassName('ytp-time-current')[0].textContent;
					var sec = (+cur_time.split(':')[0])*60+(+cur_time.split(':')[1]);
					var url_with_time = url+'?t='+sec
					console.log(url+'?t='+sec);

					chrome.runtime.sendMessage({type: 'sendUrl', url: url_with_time}, (response) => {
						console.log("[contentscript] chrome.runtime.sendMessage(sendUrl)");
					});
				}
			}
		});
		cnt++;
		if (cnt < cnt_limit) {
			myLoop();
		}
	}, 3000)
}

myLoop();