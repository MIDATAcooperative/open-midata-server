// The frontend controller
MiSens.controller('ViewController', ['$scope', '$document', '$translate', '$location', 'midataServer', 'midataPortal', 'information',
	function($scope, $document, $translate, $location, midataServer, midataPortal, information) {

	    // Make layout fit into MIDATA page
	    midataPortal.autoresize();

	    // Use language from MIDATA portal
		$translate.use(midataPortal.language);
		
		midataServer.authToken = $location.search().authToken;

		information.getInformation(midataServer.authToken).then(function (result) {
			
			// draw image
			var canvas = $document[0].getElementById("myCanvas");
			var ctx = canvas.getContext("2d");
			var imageObj = new Image();
			var arrayWithValues = [null,null,null,null,null,null,null,null];
			var _value;

			if (result['isobuteryl-aldehyde']) {
				_value = Math.round(result['isobuteryl-aldehyde']);
				switch (_value) {
					case Math.round(0.0): arrayWithValues[0] = 0; break;
					case Math.round(100000): arrayWithValues[0] = 1; break;
					case Math.round(27825.62239): arrayWithValues[0] = 2; break;
					case Math.round(7742.652614): arrayWithValues[0] = 3; break;
					case Math.round(2154.441279): arrayWithValues[0] = 4; break;
					case Math.round(599.486695): arrayWithValues[0] = 5; break;
					case Math.round(166.810904): arrayWithValues[0] = 6; break;
					case Math.round(46.41617226): arrayWithValues[0] = 7; break;
					case Math.round(12.91558882): arrayWithValues[0] = 8; break;
					case Math.round(3.593842974): arrayWithValues[0] = 9; break;
					case Math.round(1.000009175): arrayWithValues[0] = 10; break;
					default:
						break;
				}
			}
			if (result['isovaleci-acid']) {
				_value = Math.round(result['isovaleci-acid']);
				switch (_value) {
					case Math.round(0.0): arrayWithValues[1] = 0; break;
					case Math.round(100000): arrayWithValues[1] = 1; break;
					case Math.round(27825.62239): arrayWithValues[1] = 2; break;
					case Math.round(7742.652614): arrayWithValues[1] = 3; break;
					case Math.round(2154.441279): arrayWithValues[1] = 4; break;
					case Math.round(599.486695): arrayWithValues[1] = 5; break;
					case Math.round(166.810904): arrayWithValues[1] = 6; break;
					case Math.round(46.41617226): arrayWithValues[1] = 7; break;
					case Math.round(12.91558882): arrayWithValues[1] = 8; break;
					case Math.round(3.593842974): arrayWithValues[1] = 9; break;
					case Math.round(1.000009175): arrayWithValues[1] = 10; break;
					default:
						break;
				}
			}
			if (result.rotundone) {
				_value = Math.round(result.rotundone);
				switch (_value) {
					case Math.round(0.0): arrayWithValues[2] = 0; break;
					case Math.round(100): arrayWithValues[2] = 1; break;
					case Math.round(27.82562239): arrayWithValues[2] = 2; break;
					case Math.round(7.742652614): arrayWithValues[2] = 3; break;
					case Math.round(2.154441279): arrayWithValues[2] = 4; break;
					case Math.round(0.599486695): arrayWithValues[2] = 5; break;
					case Math.round(0.166810904): arrayWithValues[2] = 6; break;
					case Math.round(0.046416172): arrayWithValues[2] = 7; break;
					case Math.round(0.012915589): arrayWithValues[2] = 8; break;
					case Math.round(0.003593843): arrayWithValues[2] = 9; break;
					case Math.round(0.001): arrayWithValues[2] = 10; break;
					default:
						break;
				}
			}
			if (result.sucrose) {
				_value = result.sucrose;	
				if (_value == 0) {
					arrayWithValues[3] = 0;
				} else {
					arrayWithValues[3] = (60 - (_value % 60))/6;
				}
			}
			if (result.nacl) {
				_value = result.nacl;	
				if (_value == 0) {
					arrayWithValues[4] = 0;
				} else {
					arrayWithValues[4] = (60 - (_value % 60))/6;
				}
			}
			if (result.prop) {
				_value = result.prop;	
				if (_value == 0) {
					arrayWithValues[5] = 0;
				} else {
					arrayWithValues[5] = (60 - (_value % 60))/6;
				}
			}
			if (result['beta-ionone']) {
				_value = Math.round(result['beta-ionone']);
				switch (_value) {
					case Math.round(0.0): arrayWithValues[6] = 0; break;
					case Math.round(100000): arrayWithValues[6] = 1; break;
					case Math.round(21547.08037): arrayWithValues[6] = 2; break;
					case Math.round(4642.766725): arrayWithValues[6] = 3; break;
					case Math.round(1000.380678): arrayWithValues[6] = 4; break;
					case Math.round(215.5528286): arrayWithValues[6] = 5; break;
					case Math.round(46.44534123): arrayWithValues[6] = 6; break;
					case Math.round(10.007615): arrayWithValues[6] = 7; break;
					case Math.round(2.156348848): arrayWithValues[6] = 8; break;
					case Math.round(0.464630219): arrayWithValues[6] = 9; break;
					case Math.round(0.100114247): arrayWithValues[6] = 10; break;
					default:
						break;
				}
			}
			if (result.heptanone) {
				_value = Math.round(result.heptanone);
				switch (_value) {
					case Math.round(0.0): arrayWithValues[7] = 0; break;
					case Math.round(100000): arrayWithValues[7] = 1; break;
					case Math.round(27825.62239): arrayWithValues[7] = 2; break;
					case Math.round(7742.652614): arrayWithValues[7] = 3; break;
					case Math.round(2154.441279): arrayWithValues[7] = 4; break;
					case Math.round(599.486695): arrayWithValues[7] = 5; break;
					case Math.round(166.810904): arrayWithValues[7] = 6; break;
					case Math.round(46.41617226): arrayWithValues[7] = 7; break;
					case Math.round(12.91558882): arrayWithValues[7] = 8; break;
					case Math.round(3.593842974): arrayWithValues[7] = 9; break;
					case Math.round(1.000009175): arrayWithValues[7] = 10; break;
					default:
						break;
				}
			}

			imageObj.onload = function() {
				ctx.drawImage(imageObj, 0, 0);
				
				// variables
				var x = 453;
				var y = 462;
				var r = 21.7;
				var numberOfElements = 8;
				var angleInRadian = Math.PI * 2 / numberOfElements;
				
				// draw point in the center to calibrate!
				ctx.fillStyle = "#872233";
				ctx.fillRect(x, y, 1, 1);

				// center of the diagram is -1. The next point is 0, then 1, etc.
				// the lines are going to be drawed in clockwise and start with angle 0°

				ctx.lineWidth = 4;
				ctx.beginPath();

				// first point is a position, not a line
				var _firstValueInPosition = arrayWithValues[0];
				ctx.moveTo(x + (_firstValueInPosition + 1) * r, y);

				for (var i = 1; i < numberOfElements; i++) {
					var _valueInPosition = arrayWithValues[i];

					if (_valueInPosition == null) {
						continue;
					}

					var _distance = (_valueInPosition + 1) * r;
					ctx.lineTo(x + _distance * Math.cos(angleInRadian * i), y + _distance * Math.sin(angleInRadian * i));
				}

				// close diagram
				ctx.lineTo(x + (_firstValueInPosition + 1) * r, y);

				ctx.strokeStyle = "green";//"#00ff00";
				ctx.stroke();
			};
			
			imageObj.src = "spider_plot.png";
		});
	}
]);
MiSens.controller('PreviewCtrl', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'information',
	function($scope, $translate, $location, midataServer, midataPortal, information) {
		$translate.use(midataPortal.language);
	}
]);
