module.exports = {
	status: "wip",

	context: {
		fieldsets: [
			{
				type: "text",
				id: "first-name",
				label: "First Name",
			},
			{
				type: "text",
				id: "last-name",
				label: "Last Name",
			},
			{
				type: "date",
				id: "birthdate",
				label: "Date of Birth",
			},
			{
				type: "text",
				id: "gender",
				label: "gender",
			},
			{
				type: "email",
				label: "Email",
			},
			{
				type: "password",
				label: "Password",
			},
			{
				type: "text",
				id: "country",
				label: "Country"
			}
		]
	}
}
