module.exports = {
	context: {
		text: "sebastian.weber@gmail.com",
	},

	variants: [
		{
			name: "highlighted",
			context: {
				modifier_classes: " mi-at-text--highlighted",
			}
		},
		{
			name: "title",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--title",
			}
		},
		{
			name: "title-white",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--title mi-at-text--white",
			}
		},
		{
			name: "title-highlighted",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--highlighted mi-at-text--title",
			}
		},
		{
			name: "title-highlighted-white",
			label: "Title Highlighted White",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--title mi-at-text--highlighted mi-at-text--white",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
				"padding": "15px"
			},
			notes: "The gradient background and text paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
		{
			name: "big-title",
			label: "Big Title",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--big-title",
			}
		},
		{
			name: "big-title-white",
			label: "Big Title White",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--big-title mi-at-text--white",
			}
		},
		{
			name: "smaller",
			context: {
				modifier_classes: " mi-at-text--smaller",
			}
		},
		{
			name: "smaller-white",
			context: {
				modifier_classes: " mi-at-text--smaller mi-at-text--white",
			}
		},
		{
			name: "smallest",
			context: {
				text: "connected apps",
				modifier_classes: " mi-at-text--smallest",
			}
		},
		{
			name: "smallest-white",
			context: {
				text: "connected apps",
				modifier_classes: " mi-at-text--smallest mi-at-text--white",
			}
		},
		{
			name: "smallest-highlighted-white",
			context: {
				text: "connected apps",
				modifier_classes: " mi-at-text--highlighted mi-at-text--smallest mi-at-text--white",
			}
		},
		{
			name: "white",
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
				"max-height": "50px",
				"max-width": "320px",
				"padding": "15px"
			},
			notes: "The gradient background and text paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
			context: {
				modifier_classes: " mi-at-text--white",
			}
		},
		{
			name: "highlighted-white",
			context: {
				text: "Timeline",
				modifier_classes: " mi-at-text--highlighted mi-at-text--white",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd);",
				"padding": "15px"
			},
			notes: "The gradient background and text paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
	],
}
