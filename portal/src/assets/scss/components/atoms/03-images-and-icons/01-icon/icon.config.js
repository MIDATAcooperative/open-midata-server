module.exports = {
	default: "default",
	notes: "You can have a look at the full list of available icons <a href='/docs/icons'>in the docs</a>.",
	variants: [
		{
			name: "default",
			label: "Default",
			context: {
				icon: "timeline",
			}
		},
		{
			name: "close",
			label: "White Close Icon",
			context: {
				icon: "hamburger-close",
				modifier_classes: " mi-at-icon--white",
				alt: "close icon",
			},
			display: {
				"background": "linear-gradient(90deg, #32c6b6,  #0879dd)",
				"padding": "15px"
			},
			notes: "The gradient background and paddings are **not** part of the style of the component.<br />They are used here for rendering purposes only.",
		},
		{
			name: "with-badge",
			label: "Icon with badge",
			context: {
				icon: "notifications",
				alt: "icon with badge",
				badge: 9,
			}
		},
	]
};
