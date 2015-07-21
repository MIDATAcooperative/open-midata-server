package controllers;

import models.ModelException;
import models.User;

public class AccountPatches {

	public static void check(User user) throws ModelException {
		if (user.accountVersion < 1) { formatPatch150717(user); }					
	}
	
	public static void makeCurrent(User user, int currentAccountVersion) throws ModelException {
		if (user.accountVersion < currentAccountVersion) {
			user.accountVersion = currentAccountVersion;
			User.set(user._id, "accountVersion", user.accountVersion);
		}
	}
	
	public static void formatPatch150717(User user) throws ModelException {
		RecordSharing.instance.changeFormatName(user._id, "Genome Data/23andMe", "genome-data","23-and-me" );
		RecordSharing.instance.changeFormatName(user._id, "CDA", "medical","cda");
		RecordSharing.instance.changeFormatName(user._id, "File", "other","application/octet-stream");
		RecordSharing.instance.changeFormatName(user._id, "Json", "other","application/json");
		RecordSharing.instance.changeFormatName(user._id, "Credentials/Midata", "accounts/credentials", "credentials-manager");
		RecordSharing.instance.changeFormatName(user._id, "cardio2/demo-card", "observation/cardio", "demo-card/part2");
		RecordSharing.instance.changeFormatName(user._id, "cardio1/demo-card", "observation/cardio", "demo-card/part1");
		RecordSharing.instance.changeFormatName(user._id, "Water Consumption/Fitbit", "food/water-consumption", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Body Weight/Fitbit", "body/weight", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Calories Intake/Fitbit", "food/calories-intake", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Calories Burned/Fitbit", "activities/calories-burned", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Steps/Fitbit", "activities/steps", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "height", "body/height", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "heartrate", "activities/heartrate", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Distance/Fitbit", "activities/distance", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Floors Climbed/Fitbit", "activities/floors-climbed", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Elevation/Fitbit", "activities/elevation", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit/Sedentary", "activities/sedentary", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit", "activities/minutes-active", "measurements");  
		RecordSharing.instance.changeFormatName(user._id, "activities-minutesFairlyActive", "activities/minutes-active", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "sleep-timeInBed", "sleep/time-in-bed", "measurements"); 
		RecordSharing.instance.changeFormatName(user._id, "body-weight", "body/weight", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit/Time In Bed", "sleep/time-in-bed", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit/Asleep", "sleep/minutes-asleep", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit/Awake", "sleep/minutes-awake", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Minutes/Fitbit/Fall Asleep", "sleep/minutes-to-fall-asleep", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Sleep Efficiency/Fitbit", "sleep/sleep-efficiency", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Body Weight/Fitbit", "body/weight", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "BMI/Fitbit", "body/bmi", "measurements"); 
		RecordSharing.instance.changeFormatName(user._id, "Fat/Fitbit", "body/fat", "measurements");
		RecordSharing.instance.changeFormatName(user._id, "Meals/Jawbone", "food/meals", "jawbone");
		RecordSharing.instance.changeFormatName(user._id, "Moves/Jawbone", "activities/moves", "jawbone/summary");
		RecordSharing.instance.changeFormatName(user._id, "Move Ticks/Jawbone", "activities/moves", "jawbone/ticks");
		RecordSharing.instance.changeFormatName(user._id, "Sleep/Jawbone", "sleep", "jawbone/summary");
		RecordSharing.instance.changeFormatName(user._id, "Sleep Ticks/Jawbone", "sleep", "jawbone/ticks");
		RecordSharing.instance.changeFormatName(user._id, "Text", "diary", "text-app");
		RecordSharing.instance.changeFormatName(user._id, "text", "diary", "text-app");
		RecordSharing.instance.changeFormatName(user._id, "trainingplan", "calendar/trainingplan", "training-app");
	}
}
