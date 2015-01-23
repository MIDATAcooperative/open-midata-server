package models;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Test;

import utils.DateTimeUtils;

public class SortingTest {
	
	@Test
	public void sortApps() {
		App app1 = new App();
		app1._id = new ObjectId();
		App app2 = new App();
		app2._id = new ObjectId();
		List<App> list = new ArrayList<App>();
		list.add(app1);
		list.add(app2);
		Collections.sort(list);
		app1.name = "a";
		app2.name = "b";
		Collections.sort(list);
		assertEquals(0, list.indexOf(app1));
	}
	
	@Test
	public void sortCircles() {
		Circle circle1 = new Circle();
		circle1._id = new ObjectId();
		Circle circle2 = new Circle();
		circle2._id = new ObjectId();
		List<Circle> list = new ArrayList<Circle>();
		list.add(circle1);
		list.add(circle2);
		Collections.sort(list);
		circle1.order = 2;
		circle2.order = 1;
		Collections.sort(list);
		assertEquals(1, list.indexOf(circle1));
	}
	
	@Test
	public void sortMessages() {
		Message message1 = new Message();
		message1._id = new ObjectId();
		Message message2 = new Message();
		message2._id = new ObjectId();
		List<Message> list = new ArrayList<Message>();
		list.add(message1);
		list.add(message2);
		Collections.sort(list);
		message1.created = DateTimeUtils.now();
		message2.created = DateTimeUtils.now();
		Collections.sort(list);
		assertEquals(0, list.indexOf(message1));
	}
	
	@Test
	public void sortNewsItems() {
		NewsItem newsItem1 = new NewsItem();
		newsItem1._id = new ObjectId();
		NewsItem newsItem2 = new NewsItem();
		newsItem2._id = new ObjectId();
		List<NewsItem> list = new ArrayList<NewsItem>();
		list.add(newsItem1);
		list.add(newsItem2);
		Collections.sort(list);
		newsItem1.created = DateTimeUtils.now();
		newsItem2.created = DateTimeUtils.now();
		Collections.sort(list);
		assertEquals(0, list.indexOf(newsItem1));
	}
	
	@Test
	public void sortRecords() {
		Record record1 = new Record();
		record1._id = new ObjectId();
		Record record2 = new Record();
		record2._id = new ObjectId();
		List<Record> list = new ArrayList<Record>();
		list.add(record1);
		list.add(record2);
		Collections.sort(list);
		record1.created = DateTimeUtils.now();
		record2.created = DateTimeUtils.now();
		Collections.sort(list);
		assertEquals(0, list.indexOf(record1));
	}
	
	@Test
	public void sortSpaces() {
		Space space1 = new Space();
		space1._id = new ObjectId();
		Space space2 = new Space();
		space2._id = new ObjectId();
		List<Space> list = new ArrayList<Space>();
		list.add(space1);
		list.add(space2);
		Collections.sort(list);
		space1.order = 1;
		space2.order = 2;
		Collections.sort(list);
		assertEquals(0, list.indexOf(space1));
	}
	
	@Test
	public void sortUsers() {
		Member user1 = new Member();
		user1._id = new ObjectId();
		Member user2 = new Member();
		user2._id = new ObjectId();
		List<Member> list = new ArrayList<Member>();
		list.add(user1);
		list.add(user2);
		Collections.sort(list);
		user1.name = "a";
		user2.name = "b";
		Collections.sort(list);
		assertEquals(0, list.indexOf(user1));
	}
	
	@Test
	public void sortVisualizations() {
		Visualization visualization1 = new Visualization();
		visualization1._id = new ObjectId();
		Visualization visualization2 = new Visualization();
		visualization2._id = new ObjectId();
		List<Visualization> list = new ArrayList<Visualization>();
		list.add(visualization1);
		list.add(visualization2);
		Collections.sort(list);
		visualization1.name = "a";
		visualization2.name = "b";
		Collections.sort(list);
		assertEquals(0, list.indexOf(visualization1));
	}

}
