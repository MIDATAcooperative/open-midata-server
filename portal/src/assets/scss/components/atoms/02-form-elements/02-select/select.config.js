module.exports = {
	context: {
		name: "account",
		required: true,
		options: [
			{
				option: "MIDATA Member",
			},
			{
				option: "Healthcare Professional",
			},
			{
				option: "Research Scientist",
			}
		],
		text_modifier_classes: " mi-at-text--smaller"
	}
}
