angular.module('withingsi18n', [])
  .constant('i18nc', {
    en: {
      "hello_world": "Hello World",
      "start_import": "Import Now",
      "test_import": "Test Import with Withings",
      title: "Import Records from your Withings Account",
      account: "Withings Account:",
      "member_since": "Member since:",
      "which_measurements": "Please select which measurements you want to import:",

      activity_measures_steps: "EN - Activity Measures - Steps",

      titles: {
        activity_measures_steps: "EN - Withings - Activity Measures - Steps"
      }
    },
    de: {
      "hello_world": "Hallo Welt",
      "start_import": "Import starten",
      "test_import": "Test Importierung mit Withings",
      title: "Import aus ihrem Withings Account",
      account: "Withings Account:",
      "member_since": "Mitglied seit:",
      "which_measurements": "Bitte wählen Sie aus, welche Informationen sie importieren wollen:",

      activity_measures_steps: "DE - Activity Measures - Steps",

      titles: {
        activity_measures_steps: "DE - Withings - Activity Measures - Steps"
      }
    },
    it: {
      "hello_world": "Ciao mondo",
      "start_import": "??",
      "test_import": "IT - Test Import with Withings"
    },
    fr: {
      "hello_world": "Bonjour tout le monde",
      "start_import": "??",
      "test_import": "Importation d'essai avec Withings"
    }
  });