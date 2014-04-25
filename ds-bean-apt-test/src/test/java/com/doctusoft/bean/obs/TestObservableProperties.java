package com.doctusoft.bean.obs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.doctusoft.bean.ObservableProperty;
import com.doctusoft.bean.ValueChangeListener;

public class TestObservableProperties {
	
	String listenerValueString = null;
	int listenerValueInt = 0;
	boolean listenerValueBoolean = false;

	@Test
	public void testStringField() {
		Person person = new Person();
		ObservableProperty<Person, String> nameProperty = Person._name;
		assertEquals("name", nameProperty.getName());
		assertEquals(String.class, nameProperty.getType());
		assertEquals(Person.class, nameProperty.getParent());
		nameProperty.addChangeListener(person, new ValueChangeListener<String>() {
			@Override
			public void valueChanged(String newValue) {
				listenerValueString = newValue;
			}
		});
		nameProperty.setValue(person, "value");
		assertEquals("value", person.getName());
		assertEquals("value", nameProperty.getValue(person));
		assertEquals("value", listenerValueString);
		person.setName("other");
		assertEquals("other", listenerValueString);
	}
	

	@Test
	public void testIntField() {
		Person person = new Person();
		ObservableProperty<Person, Integer> yearProperty = Person._birthYear;
		assertEquals("birthYear", yearProperty.getName());
		assertEquals(Integer.class, yearProperty.getType());
		assertEquals(Person.class, yearProperty.getParent());
		yearProperty.addChangeListener(person, new ValueChangeListener<Integer>() {
			@Override
			public void valueChanged(Integer newValue) {
				listenerValueInt = newValue;
			}
		});
		yearProperty.setValue(person, 42);
		assertEquals(42, person.getBirthYear());
		assertEquals(new Integer(42), yearProperty.getValue(person));
		assertEquals(42, listenerValueInt);
		person.setBirthYear(54);
		assertEquals(54, listenerValueInt);
	}
	
	@Test
	public void testBooleanField() {
		Person person = new Person();
		ObservableProperty<Person, Boolean> residentProperty = Person._resident;
		assertEquals("resident", residentProperty.getName());
		assertEquals(Boolean.class, residentProperty.getType());
		assertEquals(Person.class, residentProperty.getParent());
		listenerValueBoolean = true;
		residentProperty.addChangeListener(person, new ValueChangeListener<Boolean>() {
			@Override
			public void valueChanged(Boolean newValue) {
				listenerValueBoolean = newValue;
			}
		});
		residentProperty.setValue(person, false);
		assertEquals(false, person.isResident());
		assertEquals(Boolean.FALSE, residentProperty.getValue(person));
		assertEquals(Boolean.FALSE, listenerValueBoolean);
		person.setResident(true);
		assertEquals(Boolean.TRUE, listenerValueBoolean);
	}
	
}
