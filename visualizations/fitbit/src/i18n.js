angular.module('fitbiti18n', [])
.constant('i18nc', {
  en : {
  "title" : "Import Records from your Fitbit Account",
  "account" : "Fitbit Account:",
  "member_since" : "Member since:",
  "which_measurements" : "Please select which measurements you want to import:",
  "measurement" : "Measurement",
  "imported" : "Imported",
  "from_date" : "From Date",
  "to_date" : "To Date",
  "autoimport" : "Automatically import measurements once per day",
  "import_btn" : "Import data",
  "progress" : "Requested {{requested}} records from Fitbit. {{saved}} records saved to database.",
  "importing" : "Importing data from Fitbit...",
  "done" : "Finished import",
  "with_errors" : "Finished import with errors.",
  
  
  "food_calories_intake": "Food - Calories Intake",
  "food_water_consumption" : "Food - Water Consumption",
  "activities_calories_burned" : "Activities - Calories Burned",
  "activities_steps" : "Activities - Steps",
  "activities_distance" : "Activities - Distance",
  "activities_floors_climbed" : "Activities - Floors Climbed",
  "activities_elevation" : "Activities - Elevation",
  "activities_minutes_sedentary" : "Activities - Minutes Sedentary",
  "activities_minutes_lightly_active" : "Activities - Minutes Lightly Active",
  "activities_minutes_fairly_active" : "Activities - Minutes Fairly Active",
  "activities_minutes_very_active" : "Activities - Minutes Very Active",
  "activities_calories_burned_in_activities" : "Activities - Calories Burned in Activities",
  "sleep_time_in_bed" : "Sleep - Time in Bed",
  "sleep_minutes_asleep" : "Sleep - Minutes Asleep",
  "sleep_minutes_awake" : "Sleep - Minutes Awake",
  "sleep_minutes_to_fall_asleep" : "Sleep - Minutes to Fall Asleep",
  "sleep_efficiency" : "Sleep - Efficiency",
  "body_weight" : "Body - Weight",
  "body_bmi" : "Body - BMI",
  "body_fat" : "Body - Fat",
  
  "titles" : {
	 "food_calories_intake" : "Fitbit food (calories intake) {date}",
     "food_water_consumption" : "Fitbit food (water consumption) {date}",
     "activities_calories_burned" : "Fitbit activities (calories burned) {date}",
     "activities_steps" : "Fitbit activities (steps) {date}",
     "activities_distance" : "Fitbit activities (distance) {date}",
     "activities_floors_climbed" : "Fitbit activities (floors climbed) {date}",
     "activities_elevation" : "Fitbit activities (elevation) {date}",
     "activities_minutes_sedentary" : "Fitbit activities (minutes sedentary) {date}",
     "activities_minutes_lightly_active" : "Fitbit activities (minutes lightly active) {date}",
     "activities_minutes_fairly_active" : "Fitbit activities (minutes fairly active) {date}",
     "activities_minutes_very_active" : "Fitbit activities (minutes very active) {date}",
     "activities_calories_burned_in_activities" : "Fitbit activities (calories burned in activities) {date}",
     "sleep_time_in_bed" : "Fitbit sleep (time in bed) {date}",
     "sleep_minutes_asleep" : "Fitbit sleep (minutes asleep) {date}",
     "sleep_minutes_awake" : "Fitbit sleep (minutes awake) {date}",
     "sleep_minutes_to_fall_asleep" : "Fitbit sleep (minutes to fall asleep) {date}",
     "sleep_efficiency" : "Sleep - Efficiency",
     "body_weight" : "Fitbit body (weight) {date}",
     "body_bmi" : "Fitbit body (BMI) {date}",
     "body_fat" : "Fitbit body (fat) {date}"
  }			
	  
  
},
de : {
	  "title" : "Import aus ihrem Fitbit Account",
	  "account" : "Fitbit Account:",
	  "member_since" : "Mitglied seit:",
	  "which_measurements" : "Bitte wählen Sie aus, welche Informationen sie importieren wollen:",
	  "measurement" : "Messungen",
	  "imported" : "Importiert",
	  "from_date" : "Ab (Datum)",
	  "to_date" : "Bis (Datum)",
	  "autoimport" : "Messungen einmal täglich automatisch importieren",
	  "import_btn" : "Importieren",
	  "progress" : "{{requested}} Datensätze angefragt. {{saved}} Datensätze gespeichert.",
	  "importing" : "Importiere Daten von Fitbit...",
	  "done" : "Import fertig",
	  "with_errors" : "Import mit Fehlern abgeschlossen.",
	  
	  
	  "food_calories_intake": "Nahrung - Kalorien Einnahme",
	  "food_water_consumption" : "Nahrung - Wasser Einnahme",
	  "activities_calories_burned" : "Aktivität - Kalorien verbrannt",
	  "activities_steps" : "Aktivität - Schritte",
	  "activities_distance" : "Aktivität - Distanz",
	  "activities_floors_climbed" : "Aktivität - Stockwerke erklommen",
	  "activities_elevation" : "Aktivität - Höhenunterschied",
	  "activities_minutes_sedentary" : "Aktivität - Minuten in Ruhe",
	  "activities_minutes_lightly_active" : "Aktivität - Minuten leicht aktiv",
	  "activities_minutes_fairly_active" : "Aktivität - Minuten durchschnittlich aktiv",
	  "activities_minutes_very_active" : "Aktivität - Minuten sehr aktiv",
	  "activities_calories_burned_in_activities" : "Aktivität - während Aktivität verbrannte Kalorien",
	  "sleep_time_in_bed" : "Schlaf - Zeit im Bett",
	  "sleep_minutes_asleep" : "Schlaf - Minuten schlafend",
	  "sleep_minutes_awake" : "Schlaf - Minuten wach",
	  "sleep_minutes_to_fall_asleep" : "Schlaf - Minuten um einzuschlafen",
	  "sleep_efficiency" : "Schlaf - Effizienz",
	  "body_weight" : "Körper - Gewicht",
	  "body_bmi" : "Körper - BMI",
	  "body_fat" : "Körper - Fett",
	  
	  "titles" : {
		 "food_calories_intake" : "Fitbit Nahrung (Kalorien Einnahme) {date}",
	     "food_water_consumption" : "Fitbit Nahrung (Wasser Einnahme) {date}",
	     "activities_calories_burned" : "Fitbit Aktivität (Kalorien verbrannt) {date}",
	     "activities_steps" : "Fitbit Aktivität (Schritte) {date}",
	     "activities_distance" : "Fitbit Aktivität (Distanz) {date}",
	     "activities_floors_climbed" : "Fitbit Aktivität (Stockwerke erklommen) {date}",
	     "activities_elevation" : "Fitbit Aktivität (Höhenunterschied) {date}",
	     "activities_minutes_sedentary" : "Fitbit Aktivität (Minuten in Ruhe) {date}",
	     "activities_minutes_lightly_active" : "Fitbit Aktivität (Minuten leicht aktiv) {date}",
	     "activities_minutes_fairly_active" : "Fitbit Aktivität (Minuten durchschnittlich aktiv) {date}",
	     "activities_minutes_very_active" : "Fitbit Aktivität (Minuten sehr aktiv) {date}",
	     "activities_calories_burned_in_activities" : "Fitbit Aktivität (während Aktivität verbrannte Kalorien) {date}",
	     "sleep_time_in_bed" : "Fitbit Schlaf (Zeit im Bett) {date}",
	     "sleep_minutes_asleep" : "Fitbit Schlaf (Minuten schlafend) {date}",
	     "sleep_minutes_awake" : "Fitbit Schlaf (Minuten wach) {date}",
	     "sleep_minutes_to_fall_asleep" : "Fitbit Schlaf (Minuten um einzuschlafen) {date}",
	     "sleep_efficiency" : "Schlaf - Effizienz",
	     "body_weight" : "Fitbit Körper (Gewicht) {date}",
	     "body_bmi" : "Fitbit Körper (BMI) {date}",
	     "body_fat" : "Fitbit Körper (Fett) {date}"
	  }					
},
it : {
		
},
fr : {
		
}
});