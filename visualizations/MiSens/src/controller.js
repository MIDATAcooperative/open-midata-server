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

            // resize canvas
            var newSize = 0.7;
            canvas.width = canvas.width * newSize;
            canvas.height = canvas.height * newSize;
            ctx.scale(newSize, newSize);

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
					if (_value > 60) {
						_value = 60;
					}
					arrayWithValues[3] = _value/6;
				}
			}
			if (result.nacl) {
				_value = result.nacl;	
				if (_value == 0) {
					arrayWithValues[4] = 0;
				} else {
					if (_value > 60) {
						_value = 60;
					}
					arrayWithValues[4] = _value/6;
				}
			}
			if (result.prop) {
				_value = result.prop;	
				if (_value == 0) {
					arrayWithValues[5] = 0;
				} else {
					if (_value > 60) {
						_value = 60;
					}
					arrayWithValues[5] = _value/6;
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
				var pointWidth = 8;
				
				// draw point in the center to calibrate!
				ctx.fillStyle = "#872233";
				ctx.fillRect(x, y, 1, 1);
				ctx.fillStyle = "green";

				// center of the diagram is -1. The next point is 0, then 1, etc.
				// the lines are going to be drawed in clockwise and start with angle 0°

				ctx.lineWidth = 2;
				ctx.beginPath();

				// first point is a position, not a line
				var _firstValueInPosition;

				var _positionOfFirstValue = -1;

				do {
					_positionOfFirstValue++;

					if (arrayWithValues[_positionOfFirstValue] != null) {
						_firstValueInPosition = arrayWithValues[_positionOfFirstValue];
						//ctx.moveTo(x + (_firstValueInPosition + 1) * r, y);
						ctx.moveTo(x + ((_firstValueInPosition + 1) * r) * Math.cos(angleInRadian * _positionOfFirstValue), y + ((_firstValueInPosition + 1) * r) * Math.sin(angleInRadian * _positionOfFirstValue));
						ctx.fillRect(x + ((_firstValueInPosition + 1) * r) * Math.cos(angleInRadian * _positionOfFirstValue) - (pointWidth/2), y + ((_firstValueInPosition + 1) * r) * Math.sin(angleInRadian * _positionOfFirstValue) - (pointWidth/2), pointWidth, pointWidth);
					}

				} while (arrayWithValues[_positionOfFirstValue] == null && _positionOfFirstValue < numberOfElements);

				for (var i = _positionOfFirstValue + 1; i < numberOfElements; i++) {
					var _valueInPosition = arrayWithValues[i];

					if (_valueInPosition == null) {
						continue;
					}

					var _distance = (_valueInPosition + 1) * r;
					ctx.lineTo(x + _distance * Math.cos(angleInRadian * i), y + _distance * Math.sin(angleInRadian * i));
					ctx.fillRect(x + _distance * Math.cos(angleInRadian * i) - (pointWidth/2), y + _distance * Math.sin(angleInRadian * i) - (pointWidth/2), pointWidth, pointWidth);
				}

				// close diagram
				ctx.lineTo(x + ((_firstValueInPosition + 1) * r) * Math.cos(angleInRadian * _positionOfFirstValue), y + ((_firstValueInPosition + 1) * r) * Math.sin(angleInRadian * _positionOfFirstValue));
				//ctx.lineTo(x + (_firstValueInPosition + 1) * r, y);

				ctx.strokeStyle = "green";//"#00ff00";
				ctx.stroke();
			};
			
			imageObj.src = "spider_plot.png";

			/**
			 * Draw bar diagrams
			 * 
			 */
			
            function generateBar(canvasId, chartLabels, chartData, chartLabel, chartLabelStringY, chartLabelStringX, selectedIndex) {
				var defaultBackgroundColor = 'rgba(0, 0, 0, 0.2)';
				var defaultBoderColor = 'rgba(0, 0, 0, 0.4)';

				var selectedBackgroundColor = 'rgba(72, 136, 30, 0.2)';
				var selectedBorderColor = 'rgba(72, 136, 30, 1)';

				var _backgroundColor = [];
				var _borderColor = [];
				for (var iColor = 0; iColor < chartLabels.length; iColor++) {
					_backgroundColor.push(defaultBackgroundColor);
					_borderColor.push(defaultBoderColor);
				}
				if (selectedIndex != null && selectedIndex >= 0 && selectedIndex <= chartLabels.length) {
					selectedIndex = Math.round(selectedIndex);
					
					if (selectedIndex == chartLabels.length) {
						selectedIndex--;
					}

					_backgroundColor[selectedIndex] = selectedBackgroundColor;
					_borderColor[selectedIndex] = selectedBorderColor;
				}

                var ctx = document.getElementById(canvasId).getContext('2d');

                var chart = new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: chartLabels,
                        datasets: [{
                            label: chartLabel,
                            data: chartData,
							backgroundColor: _backgroundColor,
							/*[
                                'rgba(255, 99, 132, 0.2)',
                                'rgba(54, 162, 235, 0.2)',
                                'rgba(255, 206, 86, 0.2)',
                                'rgba(75, 192, 192, 0.2)',
                                'rgba(153, 102, 255, 0.2)',
								'rgba(255, 159, 64, 0.2)',
								'rgba(0, 0, 0, 0.2)'
							],*/
							borderColor: _borderColor,
							/*[
                                'rgba(255,99,132,1)',
                                'rgba(54, 162, 235, 1)',
                                'rgba(255, 206, 86, 1)',
                                'rgba(75, 192, 192, 1)',
                                'rgba(153, 102, 255, 1)',
								'rgba(255, 159, 64, 1)',
								'rgba(0, 0, 0, 0.4)'
                            ],*/
                            borderWidth: 1
                        }]
                    },
                    options: {
                        scales: {
                            yAxes: [{
                                ticks: {
                                    beginAtZero:true
                                },
                                scaleLabel:{
                                    display: true,
                                    labelString: chartLabelStringY//
                                }
                            }],
                            xAxes: [{
                                ticks: {
                                    beginAtZero:true
                                },
                                scaleLabel:{
                                    display: true,
                                    labelString: chartLabelStringX//
                                }
                            }]
						},
						legend: {
							display: false
						}
						//,
						//title: {
						//	display: true,
						//	text: chartTitle
						//}
                    }
                });
            }

            var chartLabel = '# Teilnehmer';
            var chartLabelY = chartLabel;
			var chartLabelX = 'Wahrnehmungsschwelle  (ppb)';
			
			$translate("chartLabelY").then(function (translation) { 
				chartLabel = translation; 
				chartLabelY = translation;
			}).then(function () {
				
				generateBar('chart-beta-ionon',
					["0.1", "0.46", "2.15", "10", "46", "215", "1'000", "4'600", "21'500", "100'000", ">100'0000"],
					[6, 0, 5, 8, 2, 13, 10, 13, 8, 22, 21],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[6]);

				generateBar("chart-heptanone",
					["1", "3.6", "13", "46", "167", "600", "2'154", "7'742", "27'825", "100'000", ">100'000"],
					[1, 8, 2, 3, 3, 12, 9, 15, 12, 20, 21],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[7]);

				generateBar("chart-isobuteraldehyde",
					["1", "3.6", "13", "46", "167", "600", "2'154", "7'742", "27'825", "100'000", ">100'000"],
					[12, 4, 3, 4, 4, 8, 13, 14, 11, 17, 17],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[0]);
				
				generateBar("chart-iso-valeric-acid",
					["1", "3.6", "13", "46", "167", "600", "2'154", "7'742", "27'825", "100'000", ">100'000"],
					[16, 3, 10, 6, 10, 13, 13, 5, 10, 16, 7],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[1]);
				
				generateBar("chart-rotundone",
					["0.001", "0.0036", "0.013", "0.046", "0.167", "0.6", "2.15", "7.7", "27.8", "100", ">100"],
					[7, 3, 3, 5, 5, 13, 13, 18, 14, 19, 8],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[2]);
				
				chartLabelX = "Intensitätsbewertung (gLMS Skala)";
				generateBar("chart-prop",
					["0-10", "11-20", "21-30", "31-40", "41-50", "51-60", "61-70", "71-80", "81-90", "91-100"],
					[32, 13, 7, 19, 18, 18, 5, 2, 2, 3],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[5]);
				
				generateBar("chart-sugar",
					["0-10", "11-20", "21-30", "31-40", "41-50", "51-60", "61-70", "71-80", "81-90", "91-100"],
					[1, 25, 25, 40, 10, 13, 3, 1, 0, 2],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[3]);
				
				generateBar("chart-salt",
					["0-10", "11-20", "21-30", "31-40", "41-50", "51-60", "61-70", "71-80", "81-90", "91-100"],
					[3, 22, 14, 34, 20, 19, 0, 0, 0, 0],
					chartLabel, chartLabelY, chartLabelX, arrayWithValues[4]);
			});
		});
	}
]);
MiSens.controller('PreviewCtrl', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'information',
	function($scope, $translate, $location, midataServer, midataPortal, information) {
		$translate.use(midataPortal.language);
	}
]);
