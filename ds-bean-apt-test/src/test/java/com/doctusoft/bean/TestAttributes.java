package com.doctusoft.bean;

import static org.junit.Assert.assertEquals;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.junit.Test;

import com.doctusoft.Property;
import com.google.common.collect.ImmutableList;

/*
 * #%L
 * ds-bean-apt-test
 * %%
 * Copyright (C) 2014 Doctusoft Ltd.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


/**
 * This tests the generated attribute implementations. It uses attributes generated on fields.
 */
@Getter @Setter
public class TestAttributes {
	
	@Property
	private String stringField;
	
	@Property
	private int intField;
	
	@Property
	private List<String> stringListField;
	
	@Test
	public void testStringField() {
		com.doctusoft.bean.Property<TestAttributes, String> field = TestAttributes_._stringField;
		assertEquals("stringField", field.getName());
		assertEquals(String.class, field.getType());
		assertEquals(TestAttributes.class, field.getParent());
		field.setValue(this, "value");
		assertEquals("value", stringField);
		assertEquals("value", field.getValue(this));
	}

	@Test
	public void testIntField() {
		com.doctusoft.bean.Property<TestAttributes, Integer> field = TestAttributes_._intField;
		assertEquals("intField", field.getName());
		assertEquals(Integer.class, field.getType());
		assertEquals(TestAttributes.class, field.getParent());
		field.setValue(this, 42);
		assertEquals(42, intField);
		assertEquals(new Integer(42), field.getValue(this));
	}
	
	@Test
	public void testStringListField() {
		com.doctusoft.bean.Property<TestAttributes, List<String>> field = TestAttributes_._stringListField;
		assertEquals("stringListField", field.getName());
		assertEquals(List.class, field.getType());
		assertEquals(TestAttributes.class, field.getParent());
		ImmutableList<String> list = ImmutableList.of("a");
		field.setValue(this, list);
		assertEquals(list, stringListField);
		assertEquals(list, field.getValue(this));
	}
}
