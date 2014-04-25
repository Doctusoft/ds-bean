package com.doctusoft.common.core.bean;

/*
 * #%L
 * ds-bean-apt
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


import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.doctusoft.Attribute;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@SupportedAnnotationTypes("com.doctusoft.Attribute")
public class AnnotationProcessor extends AbstractProcessor {
	
	/**
	 * Attribute descriptors by class typename
	 */
	Multimap<TypeElement, AttributeDescriptor> attributeDescriptors = ArrayListMultimap.create();
	private Filer filer; 
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		// collect data
		for (Element element : roundEnv.getElementsAnnotatedWith(com.doctusoft.Attribute.class)) {
			if (element.getKind() == ElementKind.CLASS) {
				// TODO handle all fields and / or getters of the class
			}
			if (element.getKind() == ElementKind.FIELD) {
				VariableElement variableElement = (VariableElement) element;
				AttributeDescriptor descriptor = new AttributeDescriptor();
				descriptor.setFieldName(element.getSimpleName().toString());
				descriptor.setFieldType(variableElement.asType());
				descriptor.setReadonly(element.getAnnotation(Attribute.class).readonly());
				descriptor.setElement(element);
				Element enclosingElement = variableElement.getEnclosingElement();
				attributeDescriptors.put((TypeElement) enclosingElement, descriptor);
			}
			if (element.getKind() == ElementKind.METHOD) {
				ExecutableElement methodElement = (ExecutableElement) element;
				// ensure that the method is on a getter
				String fieldName = getFieldNameFromGetter(methodElement);
				if (fieldName == null) {
					processingEnv.getMessager().printMessage(Kind.ERROR, "@Attribute must be on a getter method", methodElement);
				}
				fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
				AttributeDescriptor descriptor = new AttributeDescriptor();
				descriptor.setFieldName(fieldName);
				ExecutableType type = (ExecutableType) methodElement.asType();
				descriptor.setFieldType(type.getReturnType());
				descriptor.setReadonly(element.getAnnotation(Attribute.class).readonly());
				descriptor.setElement(element);
				Element enclosingElement = methodElement.getEnclosingElement();
				attributeDescriptors.put((TypeElement) enclosingElement, descriptor); 
			}
		}
		filer = processingEnv.getFiler();
		for (TypeElement typeElement : attributeDescriptors.keySet()) {
			emitClassSource(typeElement, attributeDescriptors.get(typeElement));
		}
		return true;
 	}
	
	public String getFieldNameFromGetter(ExecutableElement element) {
		String methodName = element.getSimpleName().toString();
		String returnType = ((ExecutableType) element.asType()).getReturnType().toString();
		if (methodName.startsWith("get") || (methodName.startsWith("is") && returnType.equals("boolean"))) {
			if (methodName.startsWith("get")) {
				String fieldName = methodName.substring(3);
				if (fieldName.length() == 0)
					return null;		// not a valid getter
				return fieldName;
			}
			if (methodName.startsWith("is")) {
				String fieldName = methodName.substring(2);
				if (fieldName.length() == 0)
					return null;		// not a valid getter
				return fieldName;
			}
			return null;
		}
		return null;	// this is not a getter
	}
	
	public void emitClassSource(TypeElement enclosingType, Iterable<AttributeDescriptor> descriptors) {
		try {
			JavaFileObject source = filer.createSourceFile(enclosingType.getQualifiedName() + "_");
			PackageElement pck = (PackageElement) enclosingType.getEnclosingElement();
			Writer writer = source.openWriter();
			writer.write("package " + pck.getQualifiedName() + ";\n\n");
			writer.write("import com.doctusoft.common.core.bean.Attribute;\n");
			DeclaredType holderType = (DeclaredType) enclosingType.asType();
			String holderTypeSimpleName = ((TypeElement) holderType.asElement()).getSimpleName().toString();
			String holderTypeName = holderTypeSimpleName;
			if (!holderType.getTypeArguments().isEmpty()) {
				int parametersCount = holderType.getTypeArguments().size();
				holderTypeName += "<" + Strings.repeat("?,", parametersCount - 1) + "?>";
			}
			writer.write("\npublic class " + holderTypeSimpleName + "_ {\n");
			for (AttributeDescriptor descriptor : descriptors) {
				TypeMirror fieldType = descriptor.getFieldType();
				String fieldTypeName = fieldType.toString();
				String mappedFieldTypeName = mapPrimitiveTypeNames(fieldTypeName);
				String fieldTypeLiteral = mappedFieldTypeName;
				if (fieldType.getKind() == TypeKind.DECLARED) {
					// the field type literal is the type qualified name without the type parameters
					DeclaredType declaredType = (DeclaredType) fieldType;
					TypeElement typeElement = (TypeElement) declaredType.asElement();
					fieldTypeLiteral = typeElement.getQualifiedName().toString();
					// the type is present with the type parameters, but if the parameter is a type parameter of the enclosing type, it's replaced with a ? wildcard
					mappedFieldTypeName = eraseTypeVariables(declaredType);
				}
				String fieldName = descriptor.getFieldName();
				String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
				String getterName = "get" + capitalizedFieldName;
				if (fieldTypeName.equals("boolean")) {
					getterName = "is" + capitalizedFieldName;
				}
				String setterName = "set" + capitalizedFieldName;
				writer.write("    public static final Attribute<"
						+ holderTypeName + "," + mappedFieldTypeName + "> " + fieldName + " = \n"
								+ "    new Attribute<" + holderTypeName + "," + mappedFieldTypeName + ">() {\n"
								+ "    @Override public " + mappedFieldTypeName + " getValue(" + holderTypeName + " instance) {\n"
								+ "        return (" + fieldTypeLiteral + ") instance." + getterName + "();\n"
								+ "    }\n");
				if (!descriptor.isReadonly()) {
					writer.write(
							"    @Override public void setValue(" + holderTypeName + " instance, " + mappedFieldTypeName + " value) {\n"
							+ "        instance." + setterName + "((" + fieldTypeLiteral + ")value);\n"
							+ "    }\n");
				} else {
					// readonly attribute
					writer.write(
							"    @Override public void setValue(" + holderTypeName + " instance, " + mappedFieldTypeName + " value) {\n"
							+ "        throw new UnsupportedOperationException(\"The field " + fieldName + " on type " + holderTypeName + " did not declare a setter.\");\n"
							+ "    }\n");
				}
				writer.write(
								 "    @Override public String getName() {\n"
								+ "        return \"" + fieldName + "\";\n"
								+ "    }\n"
								+ "    @Override public Class<" + mappedFieldTypeName + "> getType() {\n"
								+ "        return (Class)" + fieldTypeLiteral + ".class;\n"
								+ "    }\n"
								+ "    @Override public Class<" + holderTypeName + "> getParent() {\n"
								+ "        return (Class)" + holderTypeSimpleName + ".class;\n"
								+ "    }\n"
								+ "};\n\n");
			}
			writer.write("\n}");
			writer.close();
		} catch (FilerException e) {
			// the file probably already existed, nothing to do
			// TODO more concise exception handling
		} catch (Exception e) {
			throw new RuntimeException("error creating source file for type: " + enclosingType, e);
		}
	}
	
	/**
	 * Recursively cans for type arguments at full depth and replaces type variables with ? wildcards. Returns the resulting type reference string 
	 */
	public String eraseTypeVariables(DeclaredType declaredType) {
		String result = ((TypeElement) declaredType.asElement()).getQualifiedName().toString();
		if (!declaredType.getTypeArguments().isEmpty()) {
			List<String> parameterStrings = Lists.newArrayList();
			for (TypeMirror typeMirror : declaredType.getTypeArguments()) {
				if (typeMirror.getKind() == TypeKind.DECLARED) {
					// normal declared types parameters are kept
					parameterStrings.add(eraseTypeVariables((DeclaredType) typeMirror));
					
				}
				if (typeMirror.getKind() == TypeKind.TYPEVAR) { 
					// type parameters of the enclosign type are erased due to the static declaration
					parameterStrings.add("?");
				}
			}
			result += "<" + Joiner.on(",").join(parameterStrings) + ">";
		}
		return result;
	}
	
	
//	public boolean isSetterPresenet(TypeElement type, String setterName, String typeName) {
//		System.out.println("looking for setter: " + setterName + ", "+ typeName);
//		// look for declared methods
//		// TODO do we have to look in supertypes? Is there a utility for this maybe?
//		for (Element element: type.getEnclosedElements()) {
//			if (element.getKind() == ElementKind.METHOD) {
//				System.out.println("checking method: " + element);
//				ExecutableElement methodElement = (ExecutableElement) element;
//				// check method name
//				if (!methodElement.getSimpleName().equals(setterName))
//					continue;
//				// return type void
//				if (!"void".equals(methodElement.getReturnType().toString()))
//					continue;
//				// parameter count
//				List<? extends VariableElement> parameters = methodElement.getParameters();
//				if (parameters.size() != 1)
//					continue;
//				// parameter types
//				VariableElement variableElement = parameters.get(0);
//				if (typeName.equals(variableElement.asType().toString()))
//					return true;
//			}
//		}
//		return false;
//	}
	
	public static final Map<String, String> primitiveTypesMap = ImmutableMap.<String, String>builder()
			.put("boolean", "Boolean")
				.put("byte", "Byte")
				.put("short", "Short")
				.put("char", "Character")
				.put("int", "Integer")
				.put("float", "Float")
				.put("long", "Long")
				.put("double", "Double")
				.build();
	
	public static String mapPrimitiveTypeNames(String typeName) {
		return Objects.firstNonNull(primitiveTypesMap.get(typeName), typeName);
	}
}